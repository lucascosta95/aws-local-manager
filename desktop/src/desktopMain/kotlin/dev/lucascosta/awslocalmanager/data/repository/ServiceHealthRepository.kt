package dev.lucascosta.awslocalmanager.data.repository

import dev.lucascosta.awslocalmanager.data.model.aws.AwsService
import dev.lucascosta.awslocalmanager.data.model.health.ServiceStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ServiceHealthRepository(
    private val serviceRepository: ServiceRepository,
) {
    private val _servicesHealth = MutableStateFlow<Map<String, ServiceStatus>>(emptyMap())
    val servicesHealth: StateFlow<Map<String, ServiceStatus>> = _servicesHealth.asStateFlow()

    suspend fun refresh(endpoint: String): Result<List<AwsService>> =
        serviceRepository.getServices(endpoint).onSuccess { services ->
            _servicesHealth.value = services.associate { it.name.lowercase() to it.status }
        }

    fun runningServices(): Set<String> = _servicesHealth.value.filter { it.value == ServiceStatus.RUNNING }.keys
}
