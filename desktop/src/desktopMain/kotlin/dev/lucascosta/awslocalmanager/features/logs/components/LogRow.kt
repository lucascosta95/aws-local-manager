package dev.lucascosta.awslocalmanager.features.logs.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.lucascosta.awslocalmanager.components.CopyButton
import dev.lucascosta.awslocalmanager.data.model.log.AppLogEntry
import dev.lucascosta.awslocalmanager.data.model.log.LogLevel
import dev.lucascosta.awslocalmanager.features.logs.asPlainText
import dev.lucascosta.awslocalmanager.features.logs.formattedTime
import dev.lucascosta.awslocalmanager.i18n.LocalLogsStrings
import dev.lucascosta.awslocalmanager.theme.JetBrainsMonoFontFamily
import dev.lucascosta.awslocalmanager.theme.LocalAppColors

@Composable
internal fun LogRow(
    entry: AppLogEntry,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
) {
    val strings = LocalLogsStrings.current
    val levelColor = levelColor(entry.level)
    val mono = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFontFamily)

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .let { if (entry.stackTrace != null) it.clickable(onClick = onToggleExpanded) else it }
                .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                entry.formattedTime(),
                style = mono,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            )
            Text(
                entry.level.name,
                style = mono,
                color = levelColor,
                modifier = Modifier.width(48.dp),
            )
            Text(
                entry.source,
                style = mono,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(132.dp),
            )
            SelectionContainer(modifier = Modifier.weight(1f)) {
                Text(entry.message, style = mono, color = MaterialTheme.colorScheme.onSurface)
            }
            if (entry.repeatCount > 1) {
                Text(
                    "x${entry.repeatCount}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (entry.stackTrace != null && !isExpanded) {
                Text(
                    strings.logsStackTraceHint,
                    style = MaterialTheme.typography.labelSmall,
                    color = levelColor.copy(alpha = 0.8f),
                )
            }
            CopyButton(textToCopy = entry.asPlainText(), contentDescription = strings.logsCopyEntry)
        }

        if (isExpanded) {
            entry.stackTrace?.let { trace ->
                SelectionContainer {
                    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                        Text(
                            trace,
                            style = mono,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier =
                                Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                    .padding(8.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun levelColor(level: LogLevel): Color {
    val colors = LocalAppColors.current
    return when (level) {
        LogLevel.DEBUG -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
        LogLevel.INFO -> colors.info
        LogLevel.WARN -> colors.warning
        LogLevel.ERROR -> MaterialTheme.colorScheme.error
    }
}
