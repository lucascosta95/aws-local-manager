package dev.lucascosta.awslocalmanager.features.inspector.handler

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.ui.graphics.vector.ImageVector
import dev.lucascosta.awslocalmanager.data.model.inspector.CacheEntry
import dev.lucascosta.awslocalmanager.data.model.inspector.InspectorDetail
import dev.lucascosta.awslocalmanager.data.model.inspector.InspectorResource
import dev.lucascosta.awslocalmanager.data.model.process.ProcessConfig
import dev.lucascosta.awslocalmanager.data.remote.AwsElastiCacheClient
import dev.lucascosta.awslocalmanager.data.remote.MemcachedCacheClient
import dev.lucascosta.awslocalmanager.data.remote.ProcessRunner
import dev.lucascosta.awslocalmanager.data.remote.RedisCacheClient

private const val REDIS_PAGE_SIZE = 100L

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

        val rawHost = info.endpointAddress
        val port = info.endpointPort
        val resolvedHost =
            if (rawHost != null && port != null && port > 0) {
                resolveHost(info.engine, info.clusterId, rawHost)
            } else {
                rawHost
            }
        val (entries, cursor, hasMore) =
            if (resolvedHost != null && port != null && port > 0) {
                loadCacheContent(info.engine, resolvedHost, port, cursor = "0", prefix = "")
            } else {
                Triple(emptyList<CacheEntry>(), "0", false)
            }

        return InspectorDetail.ElastiCacheDetail(
            clusterId = info.clusterId,
            engine = info.engine,
            status = info.status,
            numNodes = info.numNodes,
            nodeType = info.nodeType,
            engineVersion = info.engineVersion,
            endpoint = resolvedHost,
            port = port,
            cacheEntries = entries,
            hasMore = hasMore,
            cursor = cursor,
        )
    }

    override suspend fun loadMore(
        endpoint: String,
        resource: InspectorResource,
        currentDetail: InspectorDetail,
    ): InspectorDetail? {
        val detail = currentDetail as? InspectorDetail.ElastiCacheDetail ?: return null
        if (!detail.hasMore) return null
        val host = detail.endpoint ?: return null
        val port = detail.port ?: return null

        val (newEntries, newCursor, newHasMore) =
            loadCacheContent(detail.engine, host, port, cursor = detail.cursor, prefix = "")

        return detail.copy(
            cacheEntries = detail.cacheEntries + newEntries,
            cursor = newCursor,
            hasMore = newHasMore,
        )
    }

    private suspend fun resolveHost(
        engine: String,
        clusterId: String,
        host: String,
    ): String {
        if (host != "localhost" && host != "127.0.0.1") return host
        val containerName = "floci-$engine-$clusterId"
        val result =
            ProcessRunner.run(
                listOf(
                    "docker",
                    "inspect",
                    "--format",
                    "{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}",
                    containerName,
                ),
                ProcessConfig(),
            ).getOrElse { return host }
        return result.stdout.trim().takeIf { it.isNotBlank() } ?: host
    }

    private suspend fun loadCacheContent(
        engine: String,
        host: String,
        port: Int,
        cursor: String,
        prefix: String,
    ): Triple<List<CacheEntry>, String, Boolean> =
        when (engine) {
            "redis" -> {
                val page =
                    RedisCacheClient(host, port)
                        .scanPage(cursor = cursor, pattern = prefix, pageSize = REDIS_PAGE_SIZE)
                        .getOrElse { return Triple(emptyList(), "0", false) }
                Triple(page.entries, page.nextCursor, page.hasMore)
            }
            else -> {
                val entries =
                    MemcachedCacheClient(host, port)
                        .listKeys()
                        .getOrElse { return Triple(emptyList(), "0", false) }
                Triple(entries, "0", false)
            }
        }
}
