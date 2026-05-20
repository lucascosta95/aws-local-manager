package dev.lucascosta.awslocalmanager.data.model.health

data class PrerequisiteCheck(
    val id: String,
    val name: String,
    val status: CheckStatus,
    val detail: String?,
    val canAutoFix: Boolean,
    val isFixing: Boolean = false,
)
