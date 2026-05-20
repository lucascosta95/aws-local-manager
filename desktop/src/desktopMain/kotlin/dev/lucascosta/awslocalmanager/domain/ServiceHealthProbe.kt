package dev.lucascosta.awslocalmanager.domain

import dev.lucascosta.awslocalmanager.constants.AppConstants.HEALTH_PROBE_TIMEOUT_SECONDS
import dev.lucascosta.awslocalmanager.data.model.aws.AwsResourceDefinition
import dev.lucascosta.awslocalmanager.data.model.health.AppServiceStatus
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.seconds

interface ServiceHealthProbe {
    val resourceType: AwsResourceDefinition

    suspend fun check(endpoint: String): AppServiceStatus
}

internal suspend fun probeResources(list: suspend () -> List<*>): AppServiceStatus =
    try {
        withTimeout(HEALTH_PROBE_TIMEOUT_SECONDS.seconds) {
            if (list().isNotEmpty()) AppServiceStatus.ACTIVE else AppServiceStatus.AVAILABLE
        }
    } catch (e: Exception) {
        System.err.println("[ServiceHealthProbe] Health check failed: ${e.message}")
        AppServiceStatus.ERROR
    }
