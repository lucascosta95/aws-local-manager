package dev.lucascosta.awslocalmanager.features.infrastructure.components

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import dev.lucascosta.awslocalmanager.data.remote.ProcessLine
import dev.lucascosta.awslocalmanager.i18n.LocalStrings
import dev.lucascosta.awslocalmanager.theme.JetBrainsMonoFontFamily
import dev.lucascosta.awslocalmanager.theme.LocalAppColors

@Composable
internal fun LogPanel(
    lines: List<ProcessLine>,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val listState = rememberLazyListState()
    val appColors = LocalAppColors.current

    LaunchedEffect(lines.size) {
        if (lines.isNotEmpty()) listState.animateScrollToItem(lines.size - 1)
    }

    Box(
        modifier =
            modifier
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surface),
    ) {
        if (lines.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    strings.infraLogEmpty,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFontFamily),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(end = 12.dp).padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                itemsIndexed(lines, key = { idx, _ -> idx }) { _, line ->
                    val color =
                        when {
                            line.isError -> MaterialTheme.colorScheme.error
                            line.text.contains("success", ignoreCase = true) ||
                                line.text.contains("created", ignoreCase = true) ||
                                line.text.contains("apply complete", ignoreCase = true) ||
                                line.text.contains("✓") -> appColors.success

                            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.87f)
                        }

                    Text(
                        line.text,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFontFamily),
                        color = color,
                    )
                }
            }

            VerticalScrollbar(
                adapter = rememberScrollbarAdapter(listState),
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(end = 2.dp),
            )
        }
    }
}
