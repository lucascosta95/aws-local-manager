package dev.lucascosta.awslocalmanager.features.update

import dev.lucascosta.awslocalmanager.BaseViewModel
import dev.lucascosta.awslocalmanager.BuildConfig
import dev.lucascosta.awslocalmanager.constants.AppConstants.UPDATE_CHECK_DELAY_MS
import dev.lucascosta.awslocalmanager.data.model.update.ReleaseInfo
import dev.lucascosta.awslocalmanager.data.repository.PreferencesRepository
import dev.lucascosta.awslocalmanager.data.repository.UpdateRepository
import dev.lucascosta.awslocalmanager.util.isNewerVersion
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class UpdateViewModel(
    private val updateRepository: UpdateRepository,
    private val preferencesRepository: PreferencesRepository,
) : BaseViewModel() {
    private val _updateAvailable = MutableStateFlow<ReleaseInfo?>(null)
    val updateAvailable: StateFlow<ReleaseInfo?> = _updateAvailable.asStateFlow()

    init {
        scope.launch {
            delay(UPDATE_CHECK_DELAY_MS.milliseconds)
            val release = updateRepository.checkLatestRelease() ?: return@launch
            if (!isNewerVersion(BuildConfig.APP_VERSION, release.version)) {
                return@launch
            }

            val skipped = preferencesRepository.preferences.firstOrNull()?.skippedVersion.orEmpty()
            if (release.version == skipped) {
                return@launch
            }

            _updateAvailable.value = release
        }
    }

    fun dismissUpdate() {
        _updateAvailable.value = null
    }

    fun skipVersion(version: String) {
        scope.launch {
            preferencesRepository.updateSkippedVersion(version)
            _updateAvailable.value = null
        }
    }
}
