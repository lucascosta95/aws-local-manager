package dev.lucascosta.awslocalmanager.features.running.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.Icons.AutoMirrored.Outlined
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.lucascosta.awslocalmanager.components.CopyButton
import dev.lucascosta.awslocalmanager.data.model.aws.RunningResource
import dev.lucascosta.awslocalmanager.i18n.LocalStrings

@Composable
internal fun GroupHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.Outlined.Folder, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        HorizontalDivider(modifier = Modifier.weight(1f))
    }
}

@Composable
internal fun ResourceRow(
    resource: RunningResource,
    isSelected: Boolean,
    isPublishTarget: Boolean,
    onToggle: () -> Unit,
    onDown: () -> Unit,
    onPublish: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    Row(
        modifier =
            modifier.fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(
                    when {
                        isPublishTarget -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                        isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        else -> MaterialTheme.colorScheme.surface
                    },
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Checkbox(checked = isSelected, onCheckedChange = { onToggle() }, modifier = Modifier.size(18.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(resource.name, style = MaterialTheme.typography.bodySmall)
            val detail = resource.arn ?: resource.url ?: ""
            if (detail.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        detail,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    CopyButton(
                        textToCopy = detail,
                        contentDescription = strings.copyAddress,
                    )
                }
            }
        }

        ResourceTypeChip(resource.type)

        if (onPublish != null) {
            OutlinedButton(
                onClick = onPublish,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
            ) {
                Icon(Outlined.Send, null, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(2.dp))
                Text(strings.runningPublishTitle, style = MaterialTheme.typography.labelSmall)
            }
        }

        OutlinedButton(
            onClick = onDown,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
        ) {
            Icon(Icons.Outlined.Delete, null, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(2.dp))
            Text(strings.runningDown, style = MaterialTheme.typography.labelSmall)
        }
    }
}
