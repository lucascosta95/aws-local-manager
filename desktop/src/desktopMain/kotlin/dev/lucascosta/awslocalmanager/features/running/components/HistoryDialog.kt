package dev.lucascosta.awslocalmanager.features.running.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.lucascosta.awslocalmanager.data.model.aws.MessageHistoryItem
import dev.lucascosta.awslocalmanager.i18n.LocalStrings

@Composable
internal fun HistoryDialog(
    history: List<MessageHistoryItem>,
    onToggleExpand: (String) -> Unit,
    onClear: () -> Unit,
    onClose: () -> Unit,
) {
    val strings = LocalStrings.current
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(strings.runningHistoryDialogTitle) },
        text = {
            if (history.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        strings.runningHistoryEmpty,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                Box(modifier = Modifier.height(400.dp).fillMaxWidth()) {
                    MessageHistoryPanel(
                        history = history,
                        onToggleExpand = onToggleExpand,
                        showTitle = false,
                    )
                }
            }
        },
        confirmButton = {
            OutlinedButton(onClick = onClose) {
                Text(strings.runningHistoryClose)
            }
        },
        dismissButton = {
            if (history.isNotEmpty()) {
                OutlinedButton(
                    onClick = onClear,
                    colors =
                        ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                ) {
                    Text(strings.runningHistoryClear)
                }
            }
        },
    )
}
