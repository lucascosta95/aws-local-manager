package dev.lucascosta.awslocalmanager.features.infrastructure.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.lucascosta.awslocalmanager.data.model.aws.ResourceRunningStatus
import dev.lucascosta.awslocalmanager.data.model.project.TerraformResource
import dev.lucascosta.awslocalmanager.features.infrastructure.ResourceOpStatus
import dev.lucascosta.awslocalmanager.i18n.LocalStrings
import dev.lucascosta.awslocalmanager.theme.LocalAppColors

@Composable
internal fun ResourceRow(
    resource: TerraformResource,
    isSelected: Boolean,
    status: ResourceOpStatus,
    runningStatus: ResourceRunningStatus,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onToggle,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
            ),
        border =
            if (isSelected) {
                BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
            } else {
                null
            },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Checkbox(checked = isSelected, onCheckedChange = null, modifier = Modifier.size(20.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(resource.tfLabel, style = MaterialTheme.typography.bodyMedium)

                Text(
                    "${resource.resourceType?.id ?: resource.rawAwsType} · ${resource.awsName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }

            if (!resource.isSupported) {
                UnsupportedChip()
            } else {
                ResourceRunningChip(runningStatus)
                StatusChip(status)
            }
        }
    }
}

@Composable
internal fun UnsupportedChip(modifier: Modifier = Modifier) {
    val strings = LocalStrings.current
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier,
    ) {
        Text(
            strings.infraResourceUnsupported,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun ResourceRunningChip(
    status: ResourceRunningStatus,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val appColors = LocalAppColors.current
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        when (status) {
            ResourceRunningStatus.CHECKING -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(10.dp),
                    strokeWidth = 1.5.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    strings.infraResourceChecking,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            ResourceRunningStatus.RUNNING -> {
                Box(modifier = Modifier.size(8.dp).background(appColors.success, CircleShape))
                Spacer(Modifier.width(4.dp))
                Text(
                    strings.infraResourceRunning,
                    style = MaterialTheme.typography.labelSmall,
                    color = appColors.success,
                )
            }
            ResourceRunningStatus.NOT_RUNNING -> {
                Box(
                    modifier =
                        Modifier.size(8.dp).background(
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            CircleShape,
                        ),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    strings.infraResourceNotRunning,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                )
            }
            ResourceRunningStatus.UNKNOWN -> {}
        }
    }
}

@Composable
internal fun StatusChip(status: ResourceOpStatus) {
    val appColors = LocalAppColors.current
    val strings = LocalStrings.current
    when (status) {
        ResourceOpStatus.IDLE -> {}
        ResourceOpStatus.PENDING ->
            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
        ResourceOpStatus.SUCCESS ->
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = appColors.success.copy(alpha = 0.15f),
            ) {
                Text(
                    strings.infraResourceSuccess,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = appColors.success,
                )
            }
        ResourceOpStatus.ERROR ->
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
            ) {
                Text(
                    strings.infraResourceError,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
    }
}
