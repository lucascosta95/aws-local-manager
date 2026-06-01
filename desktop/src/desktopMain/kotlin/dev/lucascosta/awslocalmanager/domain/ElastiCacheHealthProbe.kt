package dev.lucascosta.awslocalmanager.domain

import dev.lucascosta.awslocalmanager.data.model.health.AppServiceStatus
import dev.lucascosta.awslocalmanager.data.model.resources.ElastiCacheResource
import dev.lucascosta.awslocalmanager.data.remote.AwsElastiCacheClient
import java.util.concurrent.ConcurrentHashMap

class ElastiCacheHealthProbe(
    private val clientFactory: (String) -> AwsElastiCacheClient = ::AwsElastiCacheClient,
) : ServiceHealthProbe {
    override val resourceType = ElastiCacheResource

    private val clientCache = ConcurrentHashMap<String, AwsElastiCacheClient>()

    private fun clientFor(endpoint: String): AwsElastiCacheClient = clientCache.computeIfAbsent(endpoint, clientFactory)

    override suspend fun check(endpoint: String): AppServiceStatus =
        probeResources { clientFor(endpoint).listAllClusters().getOrThrow().map { it.clusterId } }
}
