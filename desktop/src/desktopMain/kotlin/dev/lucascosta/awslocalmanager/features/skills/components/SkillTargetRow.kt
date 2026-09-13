package dev.lucascosta.awslocalmanager.features.skills.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.lucascosta.awslocalmanager.features.skills.TargetStatus
import dev.lucascosta.awslocalmanager.i18n.LocalSkillsStrings
import dev.lucascosta.awslocalmanager.theme.LocalAppColors

@Composable
internal fun SkillTargetRow(
    status: TargetStatus,
    availableVersion: String,
    isChecked: Boolean,
    isBusy: Boolean,
    onToggle: () -> Unit,
    onUninstall: () -> Unit,
) {
    val strings = LocalSkillsStrings.current
    val colors = LocalAppColors.current

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Checkbox(checked = isChecked, onCheckedChange = { onToggle() }, enabled = !isBusy)

        Column(modifier = Modifier.weight(1f)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(status.target.displayName, style = MaterialTheme.typography.bodyMedium)
                Text(
                    if (status.isDetected) strings.skillsDetected else strings.skillsNotDetected,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (status.isDetected) colors.success else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (status.hasUpdate(availableVersion)) {
                    Text(strings.skillsUpdateBadge, style = MaterialTheme.typography.labelSmall, color = colors.warning)
                } else if (status.isInstalled) {
                    Text(strings.skillsInstalledBadge, style = MaterialTheme.typography.labelSmall, color = colors.info)
                }
                if (status.hasLegacyInstall) {
                    Text(strings.skillsLegacyBadge, style = MaterialTheme.typography.labelSmall, color = colors.warning)
                }
            }
            Text(
                status.path,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                strings.skillsInvocationFmt.replace("{command}", status.invocation),
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (status.isInstalled) {
            IconButton(onClick = onUninstall, enabled = !isBusy) {
                Icon(
                    imageVector = Icons.Outlined.DeleteOutline,
                    contentDescription = strings.skillsUninstall,
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}
