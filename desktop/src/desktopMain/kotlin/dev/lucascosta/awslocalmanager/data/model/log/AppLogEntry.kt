package dev.lucascosta.awslocalmanager.data.model.log

enum class LogLevel { DEBUG, INFO, WARN, ERROR }

data class AppLogEntry(
    val id: Long,
    val timestamp: Long,
    val level: LogLevel,
    val source: String,
    val message: String,
    val stackTrace: String? = null,
    val repeatCount: Int = 1,
)
