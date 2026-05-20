package dev.lucascosta.awslocalmanager.data.model.process

import dev.lucascosta.awslocalmanager.constants.AppConstants.PROCESS_DEFAULT_TIMEOUT_SECONDS
import java.io.File

data class ProcessConfig(
    val workingDir: File? = null,
    val envVars: Map<String, String> = emptyMap(),
    val timeoutSeconds: Long = PROCESS_DEFAULT_TIMEOUT_SECONDS,
)
