package dev.lucascosta.awslocalmanager.domain

import dev.lucascosta.awslocalmanager.data.model.health.AppServiceStatus
import dev.lucascosta.awslocalmanager.data.model.resources.GlueRegistryResource
import dev.lucascosta.awslocalmanager.data.remote.AwsGlueSchemaRegistryClient
import java.util.concurrent.ConcurrentHashMap

class GlueSchemaRegistryHealthProbe(
    private val clientFactory: (String) -> AwsGlueSchemaRegistryClient = ::AwsGlueSchemaRegistryClient,
) : ServiceHealthProbe {
    override val resourceType = GlueRegistryResource

    private val clientCache = ConcurrentHashMap<String, AwsGlueSchemaRegistryClient>()

    private fun clientFor(endpoint: String): AwsGlueSchemaRegistryClient = clientCache.computeIfAbsent(endpoint, clientFactory)

    override suspend fun check(endpoint: String): AppServiceStatus = probeResources { clientFor(endpoint).listRegistries().getOrThrow() }
}
