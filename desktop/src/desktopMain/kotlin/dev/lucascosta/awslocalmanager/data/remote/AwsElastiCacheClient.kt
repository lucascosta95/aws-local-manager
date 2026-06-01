package dev.lucascosta.awslocalmanager.data.remote

import dev.lucascosta.awslocalmanager.data.model.process.ProcessConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

data class ElastiCacheClusterInfo(
    val clusterId: String,
    val engine: String,
    val status: String,
    val numNodes: Int,
    val nodeType: String,
    val engineVersion: String,
    val endpointAddress: String?,
    val endpointPort: Int?,
)

class AwsElastiCacheClient(private val endpointUrl: String) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun listAllClusters(): Result<List<ElastiCacheClusterInfo>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val env = ProcessConfig(envVars = ProcessRunner.awsEnvVars(endpointUrl))
                val memcached = fetchMemcachedClusters(env)
                val redis = fetchRedisClusters(env)
                memcached + redis
            }
        }

    private suspend fun fetchMemcachedClusters(config: ProcessConfig): List<ElastiCacheClusterInfo> {
        val result =
            ProcessRunner.run(
                command = listOf("aws", "elasticache", "describe-cache-clusters", "--output", "json"),
                config = config,
            ).getOrElse { return emptyList() }

        if (result.exitCode != 0 || result.stdout.isBlank()) {
            return emptyList()
        }

        return runCatching {
            json.decodeFromString<ElastiCacheClustersResponse>(result.stdout).cacheClusters
                .filter { it.engine == "memcached" }
                .map { dto ->
                    ElastiCacheClusterInfo(
                        clusterId = dto.cacheClusterId,
                        engine = dto.engine,
                        status = dto.cacheClusterStatus,
                        numNodes = dto.numCacheNodes,
                        nodeType = dto.cacheNodeType,
                        engineVersion = dto.engineVersion,
                        endpointAddress = dto.configurationEndpoint?.address,
                        endpointPort = dto.configurationEndpoint?.port,
                    )
                }
        }.getOrElse { emptyList() }
    }

    private suspend fun fetchRedisClusters(config: ProcessConfig): List<ElastiCacheClusterInfo> {
        val result =
            ProcessRunner.run(
                command = listOf("aws", "elasticache", "describe-replication-groups", "--output", "json"),
                config = config,
            ).getOrElse { return emptyList() }

        if (result.exitCode != 0 || result.stdout.isBlank()) {
            return emptyList()
        }

        return runCatching {
            json.decodeFromString<ElastiCacheReplicationGroupsResponse>(result.stdout).replicationGroups
                .map { dto ->
                    val nodeGroup = dto.nodeGroups.firstOrNull()
                    val endpoint = nodeGroup?.primaryEndpoint
                    ElastiCacheClusterInfo(
                        clusterId = dto.replicationGroupId,
                        engine = "redis",
                        status = dto.status,
                        numNodes = dto.nodeGroups.sumOf { it.nodeGroupMembers.size }.coerceAtLeast(1),
                        nodeType = dto.cacheNodeType,
                        engineVersion = dto.engineVersion,
                        endpointAddress = endpoint?.address,
                        endpointPort = endpoint?.port,
                    )
                }
        }.getOrElse { emptyList() }
    }

    @Serializable
    private data class ElastiCacheClustersResponse(
        @SerialName("CacheClusters") val cacheClusters: List<CacheClusterDto> = emptyList(),
    )

    @Serializable
    private data class CacheClusterDto(
        @SerialName("CacheClusterId") val cacheClusterId: String,
        @SerialName("Engine") val engine: String = "",
        @SerialName("CacheClusterStatus") val cacheClusterStatus: String = "",
        @SerialName("NumCacheNodes") val numCacheNodes: Int = 0,
        @SerialName("CacheNodeType") val cacheNodeType: String = "",
        @SerialName("EngineVersion") val engineVersion: String = "",
        @SerialName("ConfigurationEndpoint") val configurationEndpoint: EndpointDto? = null,
    )

    @Serializable
    private data class ElastiCacheReplicationGroupsResponse(
        @SerialName("ReplicationGroups") val replicationGroups: List<ReplicationGroupDto> = emptyList(),
    )

    @Serializable
    private data class ReplicationGroupDto(
        @SerialName("ReplicationGroupId") val replicationGroupId: String,
        @SerialName("Status") val status: String = "",
        @SerialName("CacheNodeType") val cacheNodeType: String = "",
        @SerialName("EngineVersion") val engineVersion: String = "",
        @SerialName("NodeGroups") val nodeGroups: List<NodeGroupDto> = emptyList(),
    )

    @Serializable
    private data class NodeGroupDto(
        @SerialName("PrimaryEndpoint") val primaryEndpoint: EndpointDto? = null,
        @SerialName("NodeGroupMembers") val nodeGroupMembers: List<NodeGroupMemberDto> = emptyList(),
    )

    @Serializable
    private data class NodeGroupMemberDto(
        @SerialName("CacheClusterId") val cacheClusterId: String = "",
    )

    @Serializable
    private data class EndpointDto(
        @SerialName("Address") val address: String = "",
        @SerialName("Port") val port: Int = 0,
    )
}
