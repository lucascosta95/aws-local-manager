package dev.lucascosta.awslocalmanager.features.logs

import dev.lucascosta.awslocalmanager.constants.AppConstants.LOG_TIME_PATTERN
import dev.lucascosta.awslocalmanager.data.model.log.AppLogEntry
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val TIME_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern(LOG_TIME_PATTERN).withZone(ZoneId.systemDefault())

internal fun AppLogEntry.formattedTime(): String = TIME_FORMATTER.format(Instant.ofEpochMilli(timestamp))

internal fun AppLogEntry.asPlainText(): String =
    buildString {
        append("${formattedTime()} ${level.name.padEnd(5)} [$source] $message")
        if (repeatCount > 1) {
            append(" (x$repeatCount)")
        }
        stackTrace?.let { append("\n$it") }
    }
