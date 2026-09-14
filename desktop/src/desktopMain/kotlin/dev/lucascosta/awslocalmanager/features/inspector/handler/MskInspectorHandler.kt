package dev.lucascosta.awslocalmanager.features.inspector.handler

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Stream
import androidx.compose.ui.graphics.vector.ImageVector
import dev.lucascosta.awslocalmanager.constants.AppConstants.EMULATOR_DOCKER_NETWORK
import dev.lucascosta.awslocalmanager.data.model.inspector.InspectorDetail
import dev.lucascosta.awslocalmanager.data.model.inspector.InspectorResource
import dev.lucascosta.awslocalmanager.data.model.inspector.MskInspectorGroup
import dev.lucascosta.awslocalmanager.data.model.inspector.MskInspectorRecord
import dev.lucascosta.awslocalmanager.data.model.inspector.MskInspectorSchema
import dev.lucascosta.awslocalmanager.data.model.inspector.MskInspectorTopic
import dev.lucascosta.awslocalmanager.data.remote.AwsMskClient
import dev.lucascosta.awslocalmanager.data.remote.HostProxy
import dev.lucascosta.awslocalmanager.data.remote.HostProxyClient
import dev.lucascosta.awslocalmanager.data.remote.HostProxyKind
import dev.lucascosta.awslocalmanager.data.remote.KafkaBrokerClient
import dev.lucascosta.awslocalmanager.data.remote.MskClusterInfo

class MskInspectorHandler(
    private val brokerClient: KafkaBrokerClient = KafkaBrokerClient(),
    private val proxyClient: HostProxyClient = HostProxyClient(),
) : InspectorServiceHandler {
    companion object {
        const val SUMMARY_TYPE = "msk"
        private const val RECORDS_LIMIT = 100
    }

    override val serviceKey: String = "kafka"
    override val displayName: String = "MSK"
    override val icon: ImageVector = Icons.Outlined.Stream

    private var lastKnownClusters: List<MskClusterInfo> = emptyList()

    override suspend fun loadResources(endpoint: String): List<InspectorResource> {
        val clusters = AwsMskClient(endpoint).listClusters().getOrElse { return emptyList() }
        lastKnownClusters = clusters
        return clusters
            .map { info -> InspectorResource(id = info.name, name = info.name, summaryType = SUMMARY_TYPE) }
            .sortedBy { it.name }
    }

    override suspend fun loadDetail(
        endpoint: String,
        resource: InspectorResource,
    ): InspectorDetail {
        val info =
            lastKnownClusters.firstOrNull { it.name == resource.id }
                ?: AwsMskClient(endpoint).findCluster(resource.id).getOrThrow()
                ?: error("Cluster ${resource.id} no longer exists")
        val container = if (info.isActive) brokerClient.findBrokerContainer(info.name) else null
        val proxies = if (container == null) emptyList() else proxyClient.list()
        val detail =
            InspectorDetail.MskDetail(
                clusterName = info.name,
                clusterType = info.type,
                state = info.state,
                kafkaVersion = info.kafkaVersion,
                brokerNodes = info.brokerNodes,
                brokerContainer = container,
                hostAddress = container?.let { proxies.hostAddress(HostProxyKind.KAFKA, info.name, it) },
                schemaRegistryHostAddress = container?.let { proxies.hostAddress(HostProxyKind.SCHEMA_REGISTRY, info.name, it) },
                dockerNetwork = EMULATOR_DOCKER_NETWORK,
            )
        if (container == null) return detail

        return detail.copy(
            topics = brokerClient.listTopics(container).getOrThrow().map { MskInspectorTopic(it.name, it.partitions, it.replicas) },
            consumerGroups =
                brokerClient.listConsumerGroups(container).getOrThrow().map {
                    MskInspectorGroup(it.name, it.state, it.members, it.totalLag)
                },
            schemas =
                brokerClient.listSchemaSubjects(container).getOrThrow().map {
                    MskInspectorSchema(it.subject, it.version, it.id, it.type, it.schema)
                },
        )
    }

    private fun List<HostProxy>.hostAddress(
        kind: HostProxyKind,
        cluster: String,
        broker: String,
    ): String? = firstOrNull { it.kind == kind && it.cluster == cluster && it.broker == broker && it.isRunning }?.hostAddress

    override suspend fun loadSubDetail(
        endpoint: String,
        resource: InspectorResource,
        currentDetail: InspectorDetail,
        subItemId: String,
    ): InspectorDetail? {
        val detail = currentDetail as? InspectorDetail.MskDetail ?: return null
        val container = detail.brokerContainer ?: return null
        val records =
            brokerClient.consumeLatest(container, subItemId, RECORDS_LIMIT).getOrThrow().map { record ->
                MskInspectorRecord(record.partition, record.offset, record.timestamp, record.key, record.value, record.headers)
            }
        return detail.copy(selectedTopic = subItemId, records = records)
    }
}
