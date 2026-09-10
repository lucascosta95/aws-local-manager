package dev.lucascosta.awslocalmanager.features.skills.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import dev.lucascosta.awslocalmanager.i18n.LocalSkillsStrings

@Composable
internal fun InstallConfirmDialog(
    paths: List<String>,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val strings = LocalSkillsStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.skillsConfirmTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(strings.skillsConfirmMessage, style = MaterialTheme.typography.bodyMedium)
                paths.forEach { path ->
                    Text(path, style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
                }
                Text(
                    strings.skillsConfirmWarning,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = { Button(onClick = onConfirm) { Text(strings.skillsInstall) } },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text(strings.skillsCancel) } },
    )
}

@Composable
internal fun SkillPreviewDialog(
    content: String,
    onDismiss: () -> Unit,
) {
    val strings = LocalSkillsStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.skillsPreviewTitle) },
        text = {
            SelectionContainer {
                Column(modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                    Text(content, style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(strings.skillsClose) } },
    )
}
