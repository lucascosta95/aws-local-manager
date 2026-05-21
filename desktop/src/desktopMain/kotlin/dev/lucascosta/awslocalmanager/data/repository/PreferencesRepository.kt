package dev.lucascosta.awslocalmanager.data.repository

import dev.lucascosta.awslocalmanager.constants.AppConstants.APP_DATA_DIR_NAME
import dev.lucascosta.awslocalmanager.constants.AppConstants.USER_HOME
import dev.lucascosta.awslocalmanager.theme.AppTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class PreferencesRepository {
    private val json =
        Json {
            prettyPrint = true
            ignoreUnknownKeys = true
        }
    private val mutex = Mutex()
    private val prefsFile = File(System.getProperty(USER_HOME), "$APP_DATA_DIR_NAME/preferences.json")

    private val _preferences = MutableStateFlow(loadFromDisk())
    val preferences: Flow<AppPreferences> = _preferences.asStateFlow()

    private fun loadFromDisk(): AppPreferences =
        runCatching {
            if (prefsFile.exists()) {
                json.decodeFromString<AppPreferences>(prefsFile.readText())
            } else {
                AppPreferences()
            }
        }.getOrElse { AppPreferences() }

    private suspend fun save(prefs: AppPreferences) =
        mutex.withLock {
            runCatching {
                prefsFile.parentFile?.mkdirs()
                prefsFile.writeText(json.encodeToString(prefs))
                _preferences.value = prefs
            }
        }

    suspend fun updateEndpoint(endpoint: String) = save(_preferences.value.copy(endpoint = endpoint))

    suspend fun updateTheme(theme: AppTheme) = save(_preferences.value.copy(theme = theme))

    suspend fun updateLanguage(language: String) = save(_preferences.value.copy(language = language))

    suspend fun updatePollingInterval(seconds: Int) = save(_preferences.value.copy(pollingIntervalSeconds = seconds))

    suspend fun updateMaxHistory(max: Int) = save(_preferences.value.copy(maxHistory = max))

    suspend fun updateProjectsDir(dir: String) = save(_preferences.value.copy(projectsDir = dir))

    suspend fun updateAutoCheckEnv(enabled: Boolean) = save(_preferences.value.copy(autoCheckEnv = enabled))

    suspend fun updateSkippedVersion(version: String) = save(_preferences.value.copy(skippedVersion = version))

    suspend fun resetToDefaults() = save(AppPreferences())
}
