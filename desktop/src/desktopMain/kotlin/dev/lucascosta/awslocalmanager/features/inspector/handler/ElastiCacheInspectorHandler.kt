package dev.lucascosta.awslocalmanager.features.inspector.handler

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.ui.graphics.vector.ImageVector
import dev.lucascosta.awslocalmanager.data.model.inspector.InspectorDetail
import dev.lucascosta.awslocalmanager.data.model.inspector.InspectorResource
import dev.lucascosta.awslocalmanager.data.remote.AwsElastiCacheClient

class ElastiCacheInspectorHandler : InspectorServiceHandler {
    override val serviceKey: String = "elasticache"
    override val displayName: String = "ElastiCache"
    override val icon: ImageVector = Icons.Outlined.Storage

    override suspend fun loadResources(endpoint: String): List<InspectorResource> =
        AwsElastiCacheClient(endpoint).listAllClusters().getOrElse { return emptyList() }
            .map { info ->
                InspectorResource(
                    id = info.clusterId,
                    name = info.clusterId,
                    summaryType = info.engine,
                )
            }

    override suspend fun loadDetail(
        endpoint: String,
        resource: InspectorResource,
    ): InspectorDetail {
        val clusters = AwsElastiCacheClient(endpoint).listAllClusters().getOrElse { emptyList() }
        val info =
            clusters.firstOrNull { it.clusterId == resource.id }
                ?: return InspectorDetail.ElastiCacheDetail(
                    clusterId = resource.id,
                    engine = "",
                    status = "unknown",
                    numNodes = 0,
                    nodeType = "",
                    engineVersion = "",
                    endpoint = null,
                    port = null,
                )

        return InspectorDetail.ElastiCacheDetail(
            clusterId = info.clusterId,
            engine = info.engine,
            status = info.status,
            numNodes = info.numNodes,
            nodeType = info.nodeType,
            engineVersion = info.engineVersion,
            endpoint = info.endpointAddress,
            port = info.endpointPort,
        )
    }
}
