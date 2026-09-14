package dev.lucascosta.awslocalmanager.domain

import dev.lucascosta.awslocalmanager.constants.AppConstants.DEFAULT_POLLING_INTERVAL_SECONDS
import dev.lucascosta.awslocalmanager.data.remote.AwsMskClient
import dev.lucascosta.awslocalmanager.data.remote.HostProxy
import dev.lucascosta.awslocalmanager.data.remote.HostProxyClient
import dev.lucascosta.awslocalmanager.data.remote.HostProxyKind
import dev.lucascosta.awslocalmanager.data.remote.KafkaBrokerClient
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

// The MSK broker and its schema registry only resolve inside the Docker network, so a client on the host, such as a
// service started from an IDE, cannot reach them. Each active cluster gets one proxy per kind on a localhost port.
class HostProxySupervisor(
    private val preferencesRepository: PreferencesRepository,
    private val brokerClient: KafkaBrokerClient,
    private val proxyClient: HostProxyClient = HostProxyClient(),
    private val mskClientFactory: (String) -> AwsMskClient = ::AwsMskClient,
) {
    private companion object {
        const val LOG_SOURCE = "HostProxy"
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
                        .onFailure { AppLogger.warn(LOG_SOURCE, "Could not reconcile host proxies", it) }
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
            HostProxyKind.entries.forEach { kind -> reconcileKind(kind, proxies.filter { it.kind == kind }, desiredBrokers) }
        }

    private suspend fun reconcileKind(
        kind: HostProxyKind,
        proxies: List<HostProxy>,
        desiredBrokers: Map<String, String>,
    ) {
        val (healthy, stale) = proxies.partition { it.isRunning && desiredBrokers[it.cluster] == it.broker }
        stale.forEach { removeProxy(it) }

        val usedPorts = healthy.map { it.hostPort }.toMutableSet()
        desiredBrokers
            .filterKeys { cluster -> healthy.none { it.cluster == cluster } }
            .toSortedMap()
            .forEach { (cluster, broker) ->
                val previousPort = stale.firstOrNull { it.cluster == cluster }?.hostPort
                val port = choosePort(kind, previousPort, usedPorts)
                if (port == null) {
                    AppLogger.warn(LOG_SOURCE, "No free port in ${kind.hostPorts} for the ${kind.role} of $cluster")
                } else {
                    startProxy(kind, cluster, broker, port)
                    usedPorts += port
                }
            }
    }

    private suspend fun startProxy(
        kind: HostProxyKind,
        cluster: String,
        broker: String,
        port: Int,
    ) {
        proxyClient.start(kind, cluster, broker, port)
            .onSuccess { AppLogger.info(LOG_SOURCE, "The ${kind.role} of $cluster is reachable at ${kind.addressFor(port)}") }
            .onFailure { AppLogger.error(LOG_SOURCE, "Could not start the ${kind.role} of $cluster", it) }
    }

    private suspend fun removeProxy(proxy: HostProxy) {
        AppLogger.info(LOG_SOURCE, "Removing ${proxy.container} for ${proxy.cluster}")
        proxyClient.remove(proxy.container)
    }

    private fun choosePort(
        kind: HostProxyKind,
        previousPort: Int?,
        usedPorts: Set<Int>,
    ): Int? {
        val candidates = listOfNotNull(previousPort) + kind.hostPorts
        return candidates.firstOrNull { it !in usedPorts && proxyClient.isHostPortFree(it) }
    }
}
