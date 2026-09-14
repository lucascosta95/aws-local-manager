package dev.lucascosta.awslocalmanager.domain

import dev.lucascosta.awslocalmanager.data.model.health.AppServiceStatus
import dev.lucascosta.awslocalmanager.data.model.resources.MskClusterResource
import dev.lucascosta.awslocalmanager.data.remote.AwsMskClient
import java.util.concurrent.ConcurrentHashMap

class MskHealthProbe(
    private val clientFactory: (String) -> AwsMskClient = ::AwsMskClient,
) : ServiceHealthProbe {
    override val resourceType = MskClusterResource

    private val clientCache = ConcurrentHashMap<String, AwsMskClient>()

    private fun clientFor(endpoint: String): AwsMskClient = clientCache.computeIfAbsent(endpoint, clientFactory)

    override suspend fun check(endpoint: String): AppServiceStatus = probeResources { clientFor(endpoint).listClusters().getOrThrow() }
}
