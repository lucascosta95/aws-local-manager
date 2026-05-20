package dev.lucascosta.awslocalmanager.domain

import dev.lucascosta.awslocalmanager.data.model.health.AppServiceStatus
import dev.lucascosta.awslocalmanager.data.model.resources.SqsResource
import dev.lucascosta.awslocalmanager.data.remote.AwsSqsClient
import java.util.concurrent.ConcurrentHashMap

class SqsHealthProbe(
    private val clientFactory: (String) -> AwsSqsClient = ::AwsSqsClient,
) : ServiceHealthProbe {
    override val resourceType = SqsResource

    private val clientCache = ConcurrentHashMap<String, AwsSqsClient>()

    private fun clientFor(endpoint: String): AwsSqsClient =
        clientCache.computeIfAbsent(endpoint, clientFactory)

    override suspend fun check(endpoint: String): AppServiceStatus = probeResources { clientFor(endpoint).listQueues().getOrThrow() }
}
