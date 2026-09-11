package dev.lucascosta.awslocalmanager.features.logs.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.lucascosta.awslocalmanager.components.CopyButton
import dev.lucascosta.awslocalmanager.data.model.log.AppLogEntry
import dev.lucascosta.awslocalmanager.data.model.log.LogLevel
import dev.lucascosta.awslocalmanager.features.logs.asPlainText
import dev.lucascosta.awslocalmanager.features.logs.formattedTime
import dev.lucascosta.awslocalmanager.i18n.LocalLogsStrings
import dev.lucascosta.awslocalmanager.theme.JetBrainsMonoFontFamily
import dev.lucascosta.awslocalmanager.theme.LocalAppColors

private val TIME_COLUMN = 92.dp
private val LEVEL_COLUMN = 46.dp
private val SOURCE_COLUMN = 118.dp
private val ACTION_COLUMN = 28.dp

@Composable
internal fun LogRow(
    entry: AppLogEntry,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
) {
    val strings = LocalLogsStrings.current
    val levelColor = levelColor(entry.level)
    val mono = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFontFamily)
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val hasTrace = entry.stackTrace != null

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .hoverable(interactionSource)
                .background(if (isHovered) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f) else Color.Transparent)
                .let { if (hasTrace) it.clickable(onClick = onToggleExpanded) else it }
                .padding(horizontal = 10.dp, vertical = 2.dp),
    ) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                entry.formattedTime(),
                style = mono,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                maxLines = 1,
                modifier = Modifier.width(TIME_COLUMN),
            )
            Text(entry.level.name, style = mono, color = levelColor, maxLines = 1, modifier = Modifier.width(LEVEL_COLUMN))
            Text(
                entry.source,
                style = mono,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.width(SOURCE_COLUMN),
            )
            Text(entry.message, style = mono, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            if (entry.repeatCount > 1) {
                Text(
                    strings.logsRepeatFmt.replace("{count}", entry.repeatCount.toString()),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            // Reserved so the row keeps its width whether or not the pointer is over it.
            Box(modifier = Modifier.size(ACTION_COLUMN), contentAlignment = Alignment.Center) {
                if (isHovered) {
                    CopyButton(textToCopy = entry.asPlainText(), contentDescription = strings.logsCopyEntry)
                }
            }
        }

        if (hasTrace && !isExpanded) {
            Text(
                strings.logsStackTraceHint,
                style = MaterialTheme.typography.labelSmall,
                color = levelColor.copy(alpha = 0.75f),
                modifier = Modifier.padding(start = TIME_COLUMN + LEVEL_COLUMN + 20.dp),
            )
        }

        if (isExpanded) {
            entry.stackTrace?.let { trace ->
                SelectionContainer {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(start = TIME_COLUMN + LEVEL_COLUMN + 20.dp, top = 4.dp, bottom = 6.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                                .horizontalScroll(rememberScrollState())
                                .padding(8.dp),
                    ) {
                        Text(trace, style = mono, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
