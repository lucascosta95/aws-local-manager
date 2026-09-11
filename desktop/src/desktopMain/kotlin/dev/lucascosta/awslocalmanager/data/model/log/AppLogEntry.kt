package dev.lucascosta.awslocalmanager.data.model.log

enum class LogLevel { DEBUG, INFO, WARN, ERROR }

/**
 * One line of the session log.
 *
 * [source] is the part of the app that produced the entry, such as `Setup` or `ProcessRunner`. It
 * is a plain identifier rather than a translated label: the Logs screen is read by people who are
 * debugging, and it doubles as the filter key.
 *
 * [stackTrace] is filled only when an exception was caught, which is the whole reason the screen
 * exists: everywhere else the app reports that something failed without saying what threw.
 *
 * [repeatCount] counts how many times the same entry was logged in a row. The dashboard polls the
 * emulator, so an emulator that is down produces the same warning every few seconds; collapsing
 * those into one entry keeps the screen readable and [timestamp] holds the most recent occurrence.
 */
data class AppLogEntry(
    val id: Long,
    val timestamp: Long,
    val level: LogLevel,
    val source: String,
    val message: String,
    val stackTrace: String? = null,
    val repeatCount: Int = 1,
)
