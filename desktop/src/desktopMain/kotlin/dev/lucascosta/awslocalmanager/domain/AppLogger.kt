package dev.lucascosta.awslocalmanager.domain

import dev.lucascosta.awslocalmanager.constants.AppConstants.LOG_MAX_ENTRIES
import dev.lucascosta.awslocalmanager.data.model.log.AppLogEntry
import dev.lucascosta.awslocalmanager.data.model.log.LogLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.atomic.AtomicLong

object AppLogger {
    private val nextId = AtomicLong()
    private val _entries = MutableStateFlow<List<AppLogEntry>>(emptyList())
    val entries: StateFlow<List<AppLogEntry>> = _entries.asStateFlow()

    fun debug(
        source: String,
        message: String,
    ) = log(LogLevel.DEBUG, source, message, null)

    fun info(
        source: String,
        message: String,
    ) = log(LogLevel.INFO, source, message, null)

    fun warn(
        source: String,
        message: String,
        throwable: Throwable? = null,
    ) = log(LogLevel.WARN, source, message, throwable)

    fun error(
        source: String,
        message: String,
        throwable: Throwable? = null,
    ) = log(LogLevel.ERROR, source, message, throwable)

    fun clear() {
        _entries.value = emptyList()
    }

    private fun log(
        level: LogLevel,
        source: String,
        message: String,
        throwable: Throwable?,
    ) {
        val entry =
            AppLogEntry(
                id = nextId.incrementAndGet(),
                timestamp = System.currentTimeMillis(),
                level = level,
                source = source,
                message = message,
                stackTrace = throwable?.stackTraceToString(),
            )
        _entries.update { current ->
            val last = current.lastOrNull()
            val appended =
                if (last != null && last.isSameAs(entry)) {
                    current.dropLast(1) + last.copy(timestamp = entry.timestamp, repeatCount = last.repeatCount + 1)
                } else {
                    current + entry
                }
            if (appended.size > LOG_MAX_ENTRIES) appended.takeLast(LOG_MAX_ENTRIES) else appended
        }
    }

    private fun AppLogEntry.isSameAs(other: AppLogEntry): Boolean =
        level == other.level && source == other.source && message == other.message && stackTrace == other.stackTrace
}
