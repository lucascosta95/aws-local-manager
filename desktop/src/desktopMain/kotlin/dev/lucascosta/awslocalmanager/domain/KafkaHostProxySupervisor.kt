package dev.lucascosta.awslocalmanager.domain

import dev.lucascosta.awslocalmanager.constants.AppConstants.DEFAULT_POLLING_INTERVAL_SECONDS
import dev.lucascosta.awslocalmanager.constants.AppConstants.KAFKA_PROXY_FIRST_HOST_PORT
import dev.lucascosta.awslocalmanager.constants.AppConstants.KAFKA_PROXY_LAST_HOST_PORT
import dev.lucascosta.awslocalmanager.data.remote.AwsMskClient
import dev.lucascosta.awslocalmanager.data.remote.KafkaBrokerClient
import dev.lucascosta.awslocalmanager.data.remote.KafkaHostProxy
import dev.lucascosta.awslocalmanager.data.remote.KafkaHostProxyClient
import dev.lucascosta.awslocalmanager.data.repository.PreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds

// The MSK broker advertises a name that only resolves inside the Docker network, so a client on the host, such as a
// service started from an IDE, cannot follow it. Each active cluster gets a proxy that rewrites it to a localhost port.
class KafkaHostProxySupervisor(
    private val preferencesRepository: PreferencesRepository,
    private val brokerClient: KafkaBrokerClient,
    private val proxyClient: KafkaHostProxyClient = KafkaHostProxyClient(),
    private val mskClientFactory: (String) -> AwsMskClient = ::AwsMskClient,
) {
    private companion object {
        const val LOG_SOURCE = "KafkaHostProxy"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private val wakeUps = Channel<Unit>(Channel.CONFLATED)
    private var loop: Job? = null

    fun start() {
        if (loop?.isActive == true) return
        loop =
            scope.launch {
                while (isActive) {
                    val prefs = preferencesRepository.preferences.first()
                    runCatching { reconcile(prefs.endpoint) }
                        .onFailure { AppLogger.warn(LOG_SOURCE, "Could not reconcile Kafka host proxies", it) }
                    val interval = prefs.pollingIntervalSeconds.takeIf { it > 0 } ?: DEFAULT_POLLING_INTERVAL_SECONDS
                    withTimeoutOrNull(interval.seconds) { wakeUps.receive() }
                }
            }
    }

    fun requestReconcile() {
        wakeUps.trySend(Unit)
    }

    fun stop() {
        scope.cancel()
    }

    suspend fun reconcile(endpoint: String) =
        mutex.withLock {
            val proxies = proxyClient.list()
            val brokers = brokerClient.listBrokerContainers()
            val clusters = mskClientFactory(endpoint).listClusters().getOrNull()
            if (clusters == null) {
                proxies.filter { it.broker !in brokers.values }.forEach { removeProxy(it) }
                return@withLock
            }

            val activeClusters = clusters.filter { it.isActive }.map { it.name }.toSet()
            val desiredBrokers = brokers.filterKeys { it in activeClusters }
            val (healthy, stale) = proxies.partition { it.isRunning && desiredBrokers[it.cluster] == it.broker }
            stale.forEach { removeProxy(it) }

            val usedPorts = healthy.map { it.hostPort }.toMutableSet()
            desiredBrokers
                .filterKeys { cluster -> healthy.none { it.cluster == cluster } }
                .toSortedMap()
                .forEach { (cluster, broker) ->
                    val previousPort = stale.firstOrNull { it.cluster == cluster }?.hostPort
                    val port = choosePort(previousPort, usedPorts)
                    if (port == null) {
                        AppLogger.warn(
                            LOG_SOURCE,
                            "No free port between $KAFKA_PROXY_FIRST_HOST_PORT and $KAFKA_PROXY_LAST_HOST_PORT for $cluster",
                        )
                    } else {
                        startProxy(cluster, broker, port)
                        usedPorts += port
                    }
                }
        }

    private suspend fun startProxy(
        cluster: String,
        broker: String,
        port: Int,
    ) {
        proxyClient.start(cluster, broker, port)
            .onSuccess { AppLogger.info(LOG_SOURCE, "Kafka cluster $cluster is reachable from the host at localhost:$port") }
            .onFailure { AppLogger.error(LOG_SOURCE, "Could not start the Kafka proxy for $cluster", it) }
    }

    private suspend fun removeProxy(proxy: KafkaHostProxy) {
        AppLogger.info(LOG_SOURCE, "Removing Kafka proxy ${proxy.container} for ${proxy.cluster}")
        proxyClient.remove(proxy.container)
    }

    private fun choosePort(
        previousPort: Int?,
        usedPorts: Set<Int>,
    ): Int? {
        val candidates = listOfNotNull(previousPort) + (KAFKA_PROXY_FIRST_HOST_PORT..KAFKA_PROXY_LAST_HOST_PORT)
        return candidates.firstOrNull { it !in usedPorts && proxyClient.isHostPortFree(it) }
    }
}
