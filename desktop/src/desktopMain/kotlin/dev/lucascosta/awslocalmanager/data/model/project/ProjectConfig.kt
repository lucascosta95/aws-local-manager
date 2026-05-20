package dev.lucascosta.awslocalmanager.data.model.project

import kotlinx.serialization.Serializable

@Serializable
data class ProjectConfig(
    val name: String,
)
