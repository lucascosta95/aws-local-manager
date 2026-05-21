package dev.lucascosta.awslocalmanager.domain

import dev.lucascosta.awslocalmanager.data.model.health.AppServiceStatus
import dev.lucascosta.awslocalmanager.data.model.resources.StepFunctionsResource
import dev.lucascosta.awslocalmanager.data.remote.AwsStepFunctionsClient
import java.util.concurrent.ConcurrentHashMap

class StepFunctionsHealthProbe(
    private val clientFactory: (String) -> AwsStepFunctionsClient = ::AwsStepFunctionsClient,
) : ServiceHealthProbe {
    override val resourceType = StepFunctionsResource

    private val clientCache = ConcurrentHashMap<String, AwsStepFunctionsClient>()

    private fun clientFor(endpoint: String): AwsStepFunctionsClient = clientCache.computeIfAbsent(endpoint, clientFactory)

    override suspend fun check(endpoint: String): AppServiceStatus = probeResources { clientFor(endpoint).listStateMachines().getOrThrow() }
}
