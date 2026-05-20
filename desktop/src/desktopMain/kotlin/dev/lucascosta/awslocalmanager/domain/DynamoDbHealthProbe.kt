package dev.lucascosta.awslocalmanager.domain

import dev.lucascosta.awslocalmanager.data.model.health.AppServiceStatus
import dev.lucascosta.awslocalmanager.data.model.resources.DynamoDbResource
import dev.lucascosta.awslocalmanager.data.remote.AwsDynamoDbClient
import java.util.concurrent.ConcurrentHashMap

class DynamoDbHealthProbe(
    private val clientFactory: (String) -> AwsDynamoDbClient = ::AwsDynamoDbClient,
) : ServiceHealthProbe {
    override val resourceType = DynamoDbResource

    private val clientCache = ConcurrentHashMap<String, AwsDynamoDbClient>()

    private fun clientFor(endpoint: String): AwsDynamoDbClient =
        clientCache.computeIfAbsent(endpoint, clientFactory)

    override suspend fun check(endpoint: String): AppServiceStatus = probeResources { clientFor(endpoint).listTables().getOrThrow() }
}
