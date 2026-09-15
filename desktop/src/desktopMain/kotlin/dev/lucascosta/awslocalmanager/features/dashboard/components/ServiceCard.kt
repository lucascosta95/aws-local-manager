package dev.lucascosta.awslocalmanager.features.dashboard.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.TooltipPlacement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import dev.lucascosta.awslocalmanager.data.model.aws.AwsService
import dev.lucascosta.awslocalmanager.data.model.health.AppServiceStatus
import dev.lucascosta.awslocalmanager.domain.AwsServiceType
import dev.lucascosta.awslocalmanager.i18n.LocalDashboardStrings
import org.jetbrains.compose.resources.painterResource

private const val STATUS_TOOLTIP_DELAY_MS = 300

@Composable
fun ServiceCard(
    service: AwsService,
    appStatus: AppServiceStatus?,
    modifier: Modifier = Modifier,
) {
    val caption = LocalDashboardStrings.current.serviceCaptions[service.name.lowercase()]

    Card(
        modifier = modifier.fillMaxWidth().height(64.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ServiceIcon(service, appStatus)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = service.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (caption != null) {
                    Text(
                        text = caption,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ServiceIcon(
    service: AwsService,
    appStatus: AppServiceStatus?,
) {
    TooltipArea(
        tooltip = { appStatus?.let { StatusTooltip(appServiceStatusLabel(it)) } },
        delayMillis = STATUS_TOOLTIP_DELAY_MS,
        tooltipPlacement = TooltipPlacement.CursorPoint(offset = DpOffset(0.dp, 16.dp)),
    ) {
        Box {
            ServiceIconImage(service)
            if (appStatus != null) {
                AppServiceStatusDot(
                    status = appStatus,
                    size = 12.dp,
                    outline = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.align(Alignment.TopEnd).offset(x = 3.dp, y = (-3).dp),
                )
            }
        }
    }
}

@Composable
private fun StatusTooltip(label: String) {
    Surface(shape = RoundedCornerShape(6.dp), tonalElevation = 4.dp, shadowElevation = 4.dp) {
        Text(label, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ServiceIconImage(service: AwsService) {
    val iconResource = AwsServiceType.from(service.name)?.icon
    Box(
        modifier =
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (iconResource != null) {
            Image(painter = painterResource(iconResource), contentDescription = service.displayName, modifier = Modifier.size(26.dp))
        } else {
            Icon(
                imageVector = Icons.Default.Cloud,
                contentDescription = service.displayName,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}
