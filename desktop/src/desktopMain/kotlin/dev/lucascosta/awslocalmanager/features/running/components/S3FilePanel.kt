package dev.lucascosta.awslocalmanager.features.running.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.lucascosta.awslocalmanager.components.openFileDialog
import dev.lucascosta.awslocalmanager.i18n.LocalStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun S3FilePanel(
    selectedFilePath: String?,
    s3Key: String,
    onFileSelected: (String) -> Unit,
    onFileClear: () -> Unit,
    onKeyChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val scope = rememberCoroutineScope()

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            OutlinedButton(
                onClick = {
                    scope.launch(Dispatchers.IO) {
                        val path = openFileDialog()
                        if (path != null) onFileSelected(path)
                    }
                },
            ) {
                Icon(Icons.Outlined.AttachFile, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(strings.publisherS3SelectFile, style = MaterialTheme.typography.labelMedium)
            }

            if (selectedFilePath != null) {
                Text(
                    selectedFilePath.substringAfterLast("/").substringAfterLast("\\"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                IconButton(onClick = onFileClear, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                }
            } else {
                Text(
                    strings.publisherS3NoFileSelected,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        OutlinedTextField(
            value = s3Key,
            onValueChange = onKeyChanged,
            label = { Text(strings.publisherS3KeyLabel, style = MaterialTheme.typography.bodySmall) },
            placeholder = { Text(strings.publisherS3KeyHint, style = MaterialTheme.typography.bodySmall) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium,
        )
    }
}
