package dev.lucascosta.awslocalmanager.data.model.health

import kotlinx.serialization.Serializable

@Serializable
data class HealthResponse(
    val services: Map<String, String> = emptyMap(),
    val edition: String? = null,
    val version: String? = null,
)
