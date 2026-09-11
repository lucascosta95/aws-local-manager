package dev.lucascosta.awslocalmanager.features.logs

import dev.lucascosta.awslocalmanager.data.model.log.AppLogEntry
import dev.lucascosta.awslocalmanager.data.model.log.LogLevel

/**
 * What the Logs screen is showing.
 *
 * An empty [levels] or [sources] means no filtering on that axis rather than nothing selected, so
 * the screen opens showing everything and narrows only once the user asks for it.
 */
data class LogFilters(
    val levels: Set<LogLevel> = emptySet(),
    val sources: Set<String> = emptySet(),
    val query: String = "",
) {
    val isActive: Boolean get() = levels.isNotEmpty() || sources.isNotEmpty() || query.isNotBlank()

    fun matches(entry: AppLogEntry): Boolean =
        (levels.isEmpty() || entry.level in levels) &&
            (sources.isEmpty() || entry.source in sources) &&
            (query.isBlank() || containsQuery(entry))

    private fun containsQuery(entry: AppLogEntry): Boolean =
        entry.message.contains(query, ignoreCase = true) ||
            entry.source.contains(query, ignoreCase = true) ||
            entry.stackTrace?.contains(query, ignoreCase = true) == true
}

data class LogsUiState(
    val entries: List<AppLogEntry> = emptyList(),
    val visible: List<AppLogEntry> = emptyList(),
    val sources: List<String> = emptyList(),
    val filters: LogFilters = LogFilters(),
    val autoScroll: Boolean = true,
    val expandedEntryId: Long? = null,
) {
    val totalCount: Int get() = entries.size

    val visibleCount: Int get() = visible.size
}
