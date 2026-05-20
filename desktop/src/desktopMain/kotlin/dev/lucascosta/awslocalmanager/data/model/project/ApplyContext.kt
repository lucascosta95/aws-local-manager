package dev.lucascosta.awslocalmanager.data.model.project

internal data class ApplyContext(
    val env: Map<String, String>,
    val logStrings: InfraLogStrings,
)
