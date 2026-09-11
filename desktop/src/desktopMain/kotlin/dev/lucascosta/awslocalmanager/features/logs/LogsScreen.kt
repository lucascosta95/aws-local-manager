package dev.lucascosta.awslocalmanager.features.logs

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import dev.lucascosta.awslocalmanager.features.logs.components.LogFilterBar
import dev.lucascosta.awslocalmanager.features.logs.components.LogRow
import dev.lucascosta.awslocalmanager.i18n.LocalLogsStrings
import org.koin.compose.koinInject

@Composable
fun LogsScreen(
    viewModel: LogsViewModel = koinInject(),
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()

    Column(modifier = modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LogsHeader(state = state, viewModel = viewModel)
        HorizontalDivider()

        LogFilterBar(
            filters = state.filters,
            sources = state.sources,
            onToggleLevel = viewModel::toggleLevel,
            onToggleSource = viewModel::toggleSource,
            onQueryChange = viewModel::setQuery,
            onClearFilters = viewModel::clearFilters,
        )

        LogList(
            state = state,
            onToggleExpanded = viewModel::toggleExpanded,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun LogsHeader(
    state: LogsUiState,
    viewModel: LogsViewModel,
) {
    val strings = LocalLogsStrings.current
    val clipboard = LocalClipboardManager.current

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(strings.logsTitle, style = MaterialTheme.typography.titleMedium)
            Text(
                strings.logsSubtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Text(
            strings.logsCountFmt
                .replace("{visible}", state.visibleCount.toString())
                .replace("{total}", state.totalCount.toString()),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        FilterChip(
            selected = state.autoScroll,
            onClick = viewModel::toggleAutoScroll,
            label = { Text(strings.logsAutoScroll, style = MaterialTheme.typography.labelSmall) },
        )

        OutlinedButton(
            onClick = { clipboard.setText(AnnotatedString(viewModel.visibleAsText())) },
            enabled = state.visible.isNotEmpty(),
        ) {
            Icon(Icons.Outlined.ContentCopy, null, modifier = Modifier.size(14.dp))
            Text(strings.logsCopyAll, modifier = Modifier.padding(start = 6.dp))
        }

        OutlinedButton(onClick = viewModel::clearLog, enabled = state.entries.isNotEmpty()) {
            Icon(Icons.Outlined.DeleteOutline, null, modifier = Modifier.size(14.dp))
            Text(strings.logsClear, modifier = Modifier.padding(start = 6.dp))
        }
    }
}

@Composable
private fun LogList(
    state: LogsUiState,
    onToggleExpanded: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalLogsStrings.current
    val listState = rememberLazyListState()

    LaunchedEffect(state.visible.size, state.autoScroll) {
        if (state.autoScroll && state.visible.isNotEmpty()) {
            listState.scrollToItem(state.visible.size - 1)
        }
    }

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surface),
    ) {
        if (state.visible.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    if (state.entries.isEmpty()) strings.logsEmpty else strings.logsNoMatches,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(end = 12.dp).padding(vertical = 8.dp),
            ) {
                items(state.visible, key = { it.id }) { entry ->
                    LogRow(
                        entry = entry,
                        isExpanded = state.expandedEntryId == entry.id,
                        onToggleExpanded = { onToggleExpanded(entry.id) },
                    )
                }
            }
            VerticalScrollbar(
                adapter = rememberScrollbarAdapter(listState),
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxSize(),
            )
        }
    }
}
