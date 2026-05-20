package dev.lucascosta.awslocalmanager.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.lucascosta.awslocalmanager.constants.AppConstants.MAX_RELEASE_NOTES_LENGTH
import dev.lucascosta.awslocalmanager.data.model.update.ReleaseInfo
import dev.lucascosta.awslocalmanager.i18n.LocalStrings
import dev.lucascosta.awslocalmanager.util.stripMarkdown

@Composable
fun UpdateDialog(
    release: ReleaseInfo,
    onDownload: () -> Unit,
    onSkip: () -> Unit,
    onDismiss: () -> Unit,
) {
    val strings = LocalStrings.current
    val notes =
        release.releaseNotes
            .takeIf { it.isNotBlank() }
            ?.stripMarkdown()
            ?.let { if (it.length > MAX_RELEASE_NOTES_LENGTH) it.take(MAX_RELEASE_NOTES_LENGTH) + "…" else it }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.updateDialogTitle) },
        text = {
            Column {
                Text(
                    text = strings.updateDialogSubtitle.format(release.version),
                    style = MaterialTheme.typography.titleSmall,
                )
                if (notes != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = notes,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onDownload) { Text(strings.updateDialogDownload) }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onSkip) { Text(strings.updateDialogSkip) }
                OutlinedButton(onClick = onDismiss) { Text(strings.updateDialogDismiss) }
            }
        },
    )
}
