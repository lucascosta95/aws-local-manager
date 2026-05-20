package dev.lucascosta.awslocalmanager.domain

import dev.lucascosta.awslocalmanager.data.model.health.AppServiceStatus
import dev.lucascosta.awslocalmanager.data.model.resources.SnsResource
import dev.lucascosta.awslocalmanager.data.remote.AwsSnsClient
import java.util.concurrent.ConcurrentHashMap

class SnsHealthProbe(
    private val clientFactory: (String) -> AwsSnsClient = ::AwsSnsClient,
) : ServiceHealthProbe {
    override val resourceType = SnsResource

    private val clientCache = ConcurrentHashMap<String, AwsSnsClient>()

    private fun clientFor(endpoint: String): AwsSnsClient =
        clientCache.computeIfAbsent(endpoint, clientFactory)

    override suspend fun check(endpoint: String): AppServiceStatus = probeResources { clientFor(endpoint).listTopics().getOrThrow() }
}
