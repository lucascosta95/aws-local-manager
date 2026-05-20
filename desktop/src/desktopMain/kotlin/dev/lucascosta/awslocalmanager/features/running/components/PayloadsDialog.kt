package dev.lucascosta.awslocalmanager.features.running.components

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.lucascosta.awslocalmanager.data.model.aws.SavedPayload
import dev.lucascosta.awslocalmanager.i18n.LocalStrings

@Composable
internal fun PayloadsDialog(
    payloads: List<SavedPayload>,
    onApply: (SavedPayload) -> Unit,
    onClose: () -> Unit,
) {
    val strings = LocalStrings.current
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(strings.runningPayloadsDialogTitle) },
        text = {
            if (payloads.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        strings.runningPayloadsEmpty,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                val expandedItems = remember { mutableStateMapOf<Int, Boolean>() }
                val listState = rememberLazyListState()
                Box(modifier = Modifier.height(400.dp).fillMaxWidth()) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize().padding(end = 12.dp),
                        contentPadding = PaddingValues(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(payloads.indices.toList()) { index ->
                            val payload = payloads[index]
                            val expanded = expandedItems[index] == true
                            PayloadItem(
                                payload = payload,
                                expanded = expanded,
                                onToggle = { expandedItems[index] = !expanded },
                                onUse = { onApply(payload) },
                            )
                        }
                    }
                    VerticalScrollbar(
                        adapter = rememberScrollbarAdapter(listState),
                        modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(end = 2.dp),
                    )
                }
            }
        },
        confirmButton = {
            OutlinedButton(onClick = onClose) {
                Text(strings.runningPayloadsClose)
            }
        },
    )
}
