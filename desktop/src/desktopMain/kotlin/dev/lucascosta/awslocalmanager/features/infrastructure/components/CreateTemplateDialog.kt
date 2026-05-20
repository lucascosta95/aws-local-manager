package dev.lucascosta.awslocalmanager.features.infrastructure.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.lucascosta.awslocalmanager.data.model.aws.AwsResourceDefinition
import dev.lucascosta.awslocalmanager.data.model.aws.ResourceRegistry
import dev.lucascosta.awslocalmanager.i18n.LocalStrings

@Composable
internal fun CreateTemplateDialog(
    templateType: AwsResourceDefinition,
    fileName: String,
    onTypeChange: (AwsResourceDefinition) -> Unit,
    onFileNameChange: (String) -> Unit,
    onCreate: () -> Unit,
    onDismiss: () -> Unit,
) {
    val strings = LocalStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.infraCreateTemplateTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(strings.infraTemplateType, style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    ResourceRegistry.all().forEach { type ->
                        FilterChip(
                            selected = templateType == type,
                            onClick = { onTypeChange(type) },
                            label = { Text(type.id, style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                }

                OutlinedTextField(
                    value = fileName,
                    onValueChange = onFileNameChange,
                    label = { Text(strings.infraTemplateFileName, style = MaterialTheme.typography.bodySmall) },
                    placeholder = { Text("${templateType.id.lowercase()}-resource") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(onClick = onCreate, enabled = fileName.isNotBlank()) {
                Text(strings.infraCreate)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text(strings.infraCancelButton) }
        },
    )
}
