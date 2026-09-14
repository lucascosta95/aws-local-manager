package dev.lucascosta.awslocalmanager.data.repository

import dev.lucascosta.awslocalmanager.data.model.aws.RunningResource
import dev.lucascosta.awslocalmanager.data.model.resources.MskClusterResource
import dev.lucascosta.awslocalmanager.data.model.resources.MskTopicResource
import dev.lucascosta.awslocalmanager.data.remote.AwsMskClient
import dev.lucascosta.awslocalmanager.data.remote.KafkaBrokerClient
import dev.lucascosta.awslocalmanager.data.remote.MskClusterInfo

class MskRunningResources(
    private val mskClientFactory: (String) -> AwsMskClient = ::AwsMskClient,
    private val kafkaBrokerClient: KafkaBrokerClient = KafkaBrokerClient(),
) {
    suspend fun fetch(
        endpoint: String,
        activeServices: Set<String>,
    ): List<RunningResource> {
        if (MskClusterResource.healthKey !in activeServices) return emptyList()
        val clusters = mskClientFactory(endpoint).listClusters().getOrElse { return emptyList() }
        val clusterResources =
            clusters.map { info ->
                RunningResource(name = info.name, type = MskClusterResource, arn = info.arn, url = null, projectName = null)
            }
        return clusterResources + clusters.filter { it.isActive }.flatMap { fetchTopics(it) }
    }

    private suspend fun fetchTopics(cluster: MskClusterInfo): List<RunningResource> {
        val container = kafkaBrokerClient.findBrokerContainer(cluster.name) ?: return emptyList()
        return kafkaBrokerClient.listTopics(container).getOrElse { emptyList() }.map { topic ->
            RunningResource(
                name = MskTopicResource.qualifiedName(cluster.name, topic.name),
                type = MskTopicResource,
                arn = null,
                url = container,
                projectName = null,
            )
        }
    }
}
