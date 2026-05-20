package dev.lucascosta.awslocalmanager.domain

import dev.lucascosta.awslocalmanager.data.model.aws.AwsResourceDefinition
import dev.lucascosta.awslocalmanager.data.model.health.AppServiceStatus
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ServiceStatusChecker(private val probes: List<ServiceHealthProbe>) {
    private val _lastStatuses = MutableStateFlow<Map<AwsResourceDefinition, AppServiceStatus>>(emptyMap())
    val lastStatuses: StateFlow<Map<AwsResourceDefinition, AppServiceStatus>> = _lastStatuses.asStateFlow()

    suspend fun checkAll(endpoint: String): Map<AwsResourceDefinition, AppServiceStatus> {
        val result =
            coroutineScope {
                probes
                    .map { probe -> async { probe.resourceType to probe.check(endpoint) } }
                    .awaitAll()
                    .toMap()
            }

        _lastStatuses.value = result
        return result
    }
}
