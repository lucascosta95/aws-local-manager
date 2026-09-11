package dev.lucascosta.awslocalmanager.domain

import dev.lucascosta.awslocalmanager.data.model.health.AppServiceStatus
import dev.lucascosta.awslocalmanager.data.model.resources.SsmParameterResource
import dev.lucascosta.awslocalmanager.data.remote.AwsSsmClient
import java.util.concurrent.ConcurrentHashMap

class SsmHealthProbe(
    private val clientFactory: (String) -> AwsSsmClient = ::AwsSsmClient,
) : ServiceHealthProbe {
    override val resourceType = SsmParameterResource

    private val clientCache = ConcurrentHashMap<String, AwsSsmClient>()

    private fun clientFor(endpoint: String): AwsSsmClient = clientCache.computeIfAbsent(endpoint, clientFactory)

    override suspend fun check(endpoint: String): AppServiceStatus = probeResources { clientFor(endpoint).listParameters().getOrThrow() }
}
