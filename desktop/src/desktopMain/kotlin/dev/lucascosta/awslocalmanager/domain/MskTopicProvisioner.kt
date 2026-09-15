package dev.lucascosta.awslocalmanager.domain

import dev.lucascosta.awslocalmanager.data.remote.AwsMskClient
import dev.lucascosta.awslocalmanager.data.remote.KafkaBrokerClient
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

class MskTopicProvisioner(
    private val brokerClient: KafkaBrokerClient,
    private val mskClientFactory: (String) -> AwsMskClient = ::AwsMskClient,
    private val activationTimeout: Duration = 10.minutes,
    private val pollInterval: Duration = 2.seconds,
) {
    suspend fun createTopic(
        endpoint: String,
        cluster: String,
        topic: String,
        partitions: Int,
    ): Result<Unit> =
        runCatching {
            awaitClusterActive(mskClientFactory(endpoint), cluster)
            val container = brokerClient.findBrokerContainer(cluster) ?: error("No broker container found for cluster $cluster")
            brokerClient.createTopic(container, topic, partitions).getOrThrow()
        }

    private suspend fun awaitClusterActive(
        client: AwsMskClient,
        cluster: String,
    ) {
        val deadline = TimeSource.Monotonic.markNow() + activationTimeout
        while (true) {
            val info = client.findCluster(cluster).getOrThrow() ?: error("Cluster $cluster does not exist")
            if (info.isActive) return
            check(deadline.hasNotPassedNow()) { "Cluster $cluster is still ${info.state} after $activationTimeout" }
            delay(pollInterval)
        }
    }
}
