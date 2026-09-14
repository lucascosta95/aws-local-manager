package dev.lucascosta.awslocalmanager.data.model.project

internal data class ApplyContext(
    val endpoint: String,
    val env: Map<String, String>,
    val logStrings: InfraLogStrings,
)
