package dev.lucascosta.awslocalmanager.data.model.resources

import dev.lucascosta.awslocalmanager.data.model.aws.AwsResourceDefinition
import dev.lucascosta.awslocalmanager.data.model.aws.RunningResource
import dev.lucascosta.awslocalmanager.data.model.aws.SuccessSnackbarKey
import dev.lucascosta.awslocalmanager.data.remote.AwsCommands
import dev.lucascosta.awslocalmanager.data.remote.EmulatorDefaults

object ElastiCacheResource : AwsResourceDefinition {
    override val id = "ELASTICACHE"
    override val displayName = "ElastiCache"
    override val terraformPrefix = "aws_elasticache_cluster"
    override val healthKey = "elasticache"
    override val creationPriority = 2
    override val isQuickCreatable = true
    override val hasFilePublish = false
    override val publishableViaJson = false
    override val supportsPayloads = false
    override val successSnackbarKey = SuccessSnackbarKey.GENERIC

    override fun createCommand(
        name: String,
        extraProperties: Map<String, String>,
    ): List<String> {
        val engine = extraProperties["engine"] ?: "redis"
        val nodeType = extraProperties["node_type"] ?: "cache.t3.micro"
        val numNodes = extraProperties["num_cache_nodes"] ?: "1"
        return if (engine == "redis") {
            AwsCommands.createElastiCacheReplicationGroup(name, nodeType)
        } else {
            AwsCommands.createElastiCacheCluster(name, nodeType, numNodes)
        }
    }

    override fun deleteCommand(resource: RunningResource): List<String> {
        val engine = resource.url ?: "redis"
        return if (engine == "redis") {
            AwsCommands.deleteElastiCacheReplicationGroup(resource.name)
        } else {
            AwsCommands.deleteElastiCacheCluster(resource.name)
        }
    }

    override fun buildArn(name: String) =
        "arn:aws:elasticache:${EmulatorDefaults.AWS_REGION}:${EmulatorDefaults.AWS_ACCOUNT_ID}:cluster:$name"

    override fun terraformTemplate(label: String) =
        """
        resource "aws_elasticache_cluster" "$label" {
          cluster_id      = "$label"
          engine          = "redis"
          # engine          = "memcached"
          node_type       = "cache.t3.micro"
          num_cache_nodes = 1
          port            = 6379
          # port            = 11211
        }
        """.trimIndent()

    override fun publishIdentifier(resource: RunningResource) = resource.name
}
