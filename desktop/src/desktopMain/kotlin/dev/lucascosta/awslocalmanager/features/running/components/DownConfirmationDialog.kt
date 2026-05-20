package dev.lucascosta.awslocalmanager.features.running.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import dev.lucascosta.awslocalmanager.data.model.aws.RunningResource
import dev.lucascosta.awslocalmanager.i18n.LocalStrings

@Composable
internal fun DownConfirmationDialog(
    resources: List<RunningResource>,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val strings = LocalStrings.current
    val names = resources.joinToString("\n") { "  • ${it.name} (${it.type.id})" }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.runningConfirmDown) },
        text = { Text(strings.runningConfirmDownMessage.replace("{resources}", names)) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            ) { Text(strings.infraConfirmButton) }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text(strings.infraCancelButton) }
        },
    )
}
