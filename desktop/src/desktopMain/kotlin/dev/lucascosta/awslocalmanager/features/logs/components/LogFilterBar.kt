package dev.lucascosta.awslocalmanager.features.logs.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.lucascosta.awslocalmanager.data.model.log.LogLevel
import dev.lucascosta.awslocalmanager.features.logs.LogFilters
import dev.lucascosta.awslocalmanager.i18n.LocalLogsStrings

/**
 * Search and chips above the list.
 *
 * The source list grows as new parts of the app log for the first time, so the chips scroll
 * sideways in a fixed-height row instead of wrapping: a wrapping row kept stealing height from the
 * list itself, which is the part worth showing.
 */
@Composable
internal fun LogFilterBar(
    filters: LogFilters,
    sources: List<String>,
    onToggleLevel: (LogLevel) -> Unit,
    onToggleSource: (String) -> Unit,
    onQueryChange: (String) -> Unit,
    onClearFilters: () -> Unit,
) {
    val strings = LocalLogsStrings.current

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = filters.query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = { Text(strings.logsSearchPlaceholder, style = MaterialTheme.typography.bodySmall) },
                leadingIcon = { Icon(Icons.Outlined.Search, null, modifier = Modifier.size(16.dp)) },
                trailingIcon = {
                    if (filters.query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Outlined.Close, null, modifier = Modifier.size(16.dp))
                        }
                    }
                },
                textStyle = MaterialTheme.typography.bodySmall,
            )

            LogLevel.entries.forEach { level ->
                ChipToggle(
                    label = level.name,
                    isSelected = level in filters.levels,
                    onClick = { onToggleLevel(level) },
                )
            }
        }

        if (sources.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().height(36.dp).horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    strings.logsSource,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                sources.forEach { source ->
                    ChipToggle(
                        label = source,
                        isSelected = source in filters.sources,
                        onClick = { onToggleSource(source) },
                    )
                }
                if (filters.isActive) {
                    TextButton(onClick = onClearFilters) {
                        Text(strings.logsClearFilters, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChipToggle(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        modifier = Modifier.height(30.dp),
    )
}
