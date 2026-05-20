package dev.lucascosta.awslocalmanager.features.settings

import dev.lucascosta.awslocalmanager.constants.AppConstants.EMPTY_STRING
import dev.lucascosta.awslocalmanager.data.remote.EmulatorConfig
import dev.lucascosta.awslocalmanager.data.repository.AppPreferences

data class SettingsUiState(
    val preferences: AppPreferences = AppPreferences(),
    val isSaved: Boolean = false,
    val endpointDraft: String = EmulatorConfig.DEFAULT_ENDPOINT,
    val pollingIntervalDraft: Int = 10,
    val maxHistoryDraft: Int = 50,
    val projectsDirDraft: String = EMPTY_STRING,
    val autoCheckEnvDraft: Boolean = true,
)
