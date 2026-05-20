package dev.lucascosta.awslocalmanager.features.infrastructure.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.lucascosta.awslocalmanager.i18n.LocalStrings

@Composable
internal fun ActionButtons(
    isRunning: Boolean,
    selectedCount: Int,
    allUnsupported: Boolean,
    nothingSelected: Boolean,
    onUpDirect: () -> Unit,
    onCreateTemplate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = onUpDirect,
            enabled = !isRunning && !allUnsupported && !nothingSelected,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Outlined.PlayArrow, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(
                if (selectedCount > 1) strings.infraUpAllDirect else strings.infraUpDirect,
                style = MaterialTheme.typography.labelSmall,
            )
        }

        TextButton(
            onClick = onCreateTemplate,
            enabled = !isRunning,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Outlined.Add, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(strings.infraCreateTemplate, style = MaterialTheme.typography.labelSmall)
        }
    }
}
