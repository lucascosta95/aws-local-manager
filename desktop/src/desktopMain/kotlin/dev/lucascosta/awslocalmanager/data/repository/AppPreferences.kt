package dev.lucascosta.awslocalmanager.data.repository

import dev.lucascosta.awslocalmanager.constants.AppConstants.DEFAULT_MAX_HISTORY
import dev.lucascosta.awslocalmanager.constants.AppConstants.DEFAULT_POLLING_INTERVAL_SECONDS
import dev.lucascosta.awslocalmanager.constants.AppConstants.EMPTY_STRING
import dev.lucascosta.awslocalmanager.constants.AppConstants.PORTUGUESE
import dev.lucascosta.awslocalmanager.constants.AppConstants.USER_HOME
import dev.lucascosta.awslocalmanager.data.remote.EmulatorConfig
import dev.lucascosta.awslocalmanager.theme.AppTheme
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

private object AppThemeSerializer : KSerializer<AppTheme> {
    override val descriptor = PrimitiveSerialDescriptor("AppTheme", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: AppTheme) = encoder.encodeString(value.name)
    override fun deserialize(decoder: Decoder): AppTheme =
        runCatching { AppTheme.valueOf(decoder.decodeString()) }.getOrElse { AppTheme.DARK }
}

@Serializable
data class AppPreferences(
    val endpoint: String = EmulatorConfig.DEFAULT_ENDPOINT,

    @Serializable(with = AppThemeSerializer::class)
    val theme: AppTheme = AppTheme.DARK,
    val language: String = PORTUGUESE,
    val pollingIntervalSeconds: Int = DEFAULT_POLLING_INTERVAL_SECONDS,
    val maxHistory: Int = DEFAULT_MAX_HISTORY,
    val projectsDir: String = System.getProperty(USER_HOME) ?: EMPTY_STRING,
    val autoCheckEnv: Boolean = true,
    val skippedVersion: String = EMPTY_STRING,
)
