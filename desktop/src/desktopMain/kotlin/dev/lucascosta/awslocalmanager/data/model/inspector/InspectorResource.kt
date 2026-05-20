package dev.lucascosta.awslocalmanager.data.model.inspector

data class InspectorResource(
    val id: String,
    val name: String,
    val summaryType: String = "",
    val summaryCount: Long? = null,
    val currentPath: String = "",
)
