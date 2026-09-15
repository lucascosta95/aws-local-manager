package dev.lucascosta.awslocalmanager.data.repository

import dev.lucascosta.awslocalmanager.data.model.aws.AwsResourceDefinition
import dev.lucascosta.awslocalmanager.data.model.aws.RunningResource
import dev.lucascosta.awslocalmanager.data.model.resources.GlueRegistryResource
import dev.lucascosta.awslocalmanager.data.model.resources.GlueSchemaResource
import dev.lucascosta.awslocalmanager.data.remote.AwsGlueSchemaRegistryClient

class GlueRunningResources(
    private val clientFactory: (String) -> AwsGlueSchemaRegistryClient = ::AwsGlueSchemaRegistryClient,
) {
    suspend fun fetch(
        endpoint: String,
        activeServices: Set<String>,
    ): List<RunningResource> {
        if (GlueRegistryResource.healthKey !in activeServices) return emptyList()
        val client = clientFactory(endpoint)
        val registries = client.listRegistries().getOrElse { return emptyList() }
        return registries.flatMap { registry ->
            val schemas =
                client.listSchemaNames(registry).getOrElse { emptyList() }.map { schema ->
                    running(GlueSchemaResource.qualifiedName(registry, schema), GlueSchemaResource, arn = null)
                }
            listOf(running(registry, GlueRegistryResource, GlueRegistryResource.buildArn(registry))) + schemas
        }
    }

    private fun running(
        name: String,
        type: AwsResourceDefinition,
        arn: String?,
    ) = RunningResource(name = name, type = type, arn = arn, url = null, projectName = null)
}
