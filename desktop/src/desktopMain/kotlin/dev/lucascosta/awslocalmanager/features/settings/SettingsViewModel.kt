package dev.lucascosta.awslocalmanager.features.settings

import dev.lucascosta.awslocalmanager.BaseViewModel
import dev.lucascosta.awslocalmanager.constants.AppConstants.SAVED_FEEDBACK_DELAY_MS
import dev.lucascosta.awslocalmanager.data.repository.PreferencesRepository
import dev.lucascosta.awslocalmanager.theme.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

class SettingsViewModel(
    private val preferencesRepository: PreferencesRepository,
) : BaseViewModel() {
    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        scope.launch {
            preferencesRepository.preferences.collect { prefs ->
                _state.update {
                    it.copy(
                        preferences = prefs,
                        endpointDraft = prefs.endpoint,
                        pollingIntervalDraft = prefs.pollingIntervalSeconds,
                        maxHistoryDraft = prefs.maxHistory,
                        projectsDirDraft = prefs.projectsDir,
                        autoCheckEnvDraft = prefs.autoCheckEnv,
                    )
                }
            }
        }
    }

    fun updateEndpointDraft(value: String) = _state.update { it.copy(endpointDraft = value) }

    fun updatePollingDraft(value: Int) = _state.update { it.copy(pollingIntervalDraft = value) }

    fun updateMaxHistoryDraft(value: Int) = _state.update { it.copy(maxHistoryDraft = value) }

    fun updateProjectsDirDraft(value: String) = _state.update { it.copy(projectsDirDraft = value) }

    fun selectProjectsDir(chooser: () -> String?) {
        scope.launch(Dispatchers.IO) {
            val path = withContext(Dispatchers.IO) { chooser() }
            if (path != null) updateProjectsDirDraft(path)
        }
    }

    fun updateAutoCheckEnvDraft(value: Boolean) = _state.update { it.copy(autoCheckEnvDraft = value) }

    fun setTheme(theme: AppTheme) {
        scope.launch { preferencesRepository.updateTheme(theme) }
    }

    fun setLanguage(language: String) {
        scope.launch { preferencesRepository.updateLanguage(language) }
    }

    fun save() {
        scope.launch {
            val currentState = _state.value
            preferencesRepository.updateEndpoint(currentState.endpointDraft)
            preferencesRepository.updatePollingInterval(currentState.pollingIntervalDraft)
            preferencesRepository.updateMaxHistory(currentState.maxHistoryDraft)
            preferencesRepository.updateProjectsDir(currentState.projectsDirDraft)
            preferencesRepository.updateAutoCheckEnv(currentState.autoCheckEnvDraft)

            _state.update { it.copy(isSaved = true) }
            delay(SAVED_FEEDBACK_DELAY_MS.milliseconds)
            _state.update { it.copy(isSaved = false) }
        }
    }

    fun resetToDefaults() {
        scope.launch { preferencesRepository.resetToDefaults() }
    }
}
