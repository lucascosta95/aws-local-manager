package dev.lucascosta.awslocalmanager.features.logs

import dev.lucascosta.awslocalmanager.BaseViewModel
import dev.lucascosta.awslocalmanager.data.model.log.AppLogEntry
import dev.lucascosta.awslocalmanager.data.model.log.LogLevel
import dev.lucascosta.awslocalmanager.domain.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

class LogsViewModel : BaseViewModel() {
    private val filters = MutableStateFlow(LogFilters())
    private val autoScroll = MutableStateFlow(true)
    private val expandedEntryId = MutableStateFlow<Long?>(null)

    val state: StateFlow<LogsUiState> =
        combine(AppLogger.entries, filters, autoScroll, expandedEntryId, ::buildState)
            .stateIn(scope, SharingStarted.Eagerly, LogsUiState())

    fun toggleLevel(level: LogLevel) {
        filters.update { current ->
            current.copy(levels = current.levels.toggled(level))
        }
    }

    fun toggleSource(source: String) {
        filters.update { current ->
            current.copy(sources = current.sources.toggled(source))
        }
    }

    fun setQuery(query: String) {
        filters.update { it.copy(query = query) }
    }

    fun visibleAsText(): String = state.value.visible.joinToString("\n") { it.asPlainText() }

    fun clearFilters() {
        filters.value = LogFilters()
    }

    fun setAutoScroll(enabled: Boolean) {
        autoScroll.value = enabled
    }

    fun toggleExpanded(entryId: Long) {
        expandedEntryId.update { if (it == entryId) null else entryId }
    }

    fun clearLog() {
        AppLogger.clear()
        expandedEntryId.value = null
    }

    private fun buildState(
        entries: List<AppLogEntry>,
        activeFilters: LogFilters,
        isAutoScroll: Boolean,
        expandedId: Long?,
    ): LogsUiState =
        LogsUiState(
            entries = entries,
            visible = entries.filter(activeFilters::matches),
            sources = entries.map { it.source }.distinct().sorted(),
            filters = activeFilters,
            autoScroll = isAutoScroll,
            expandedEntryId = expandedId,
        )

    private fun <T> Set<T>.toggled(value: T): Set<T> = if (value in this) this - value else this + value
}
