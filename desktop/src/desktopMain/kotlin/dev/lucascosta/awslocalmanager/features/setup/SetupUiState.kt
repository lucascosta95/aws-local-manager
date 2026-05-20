package dev.lucascosta.awslocalmanager.features.setup

import dev.lucascosta.awslocalmanager.data.model.health.PrerequisiteCheck

data class SetupUiState(
    val checks: List<PrerequisiteCheck> = emptyList(),
    val isChecking: Boolean = false,
    val allOk: Boolean = false,
    val fixLogLines: List<String> = emptyList(),
)
