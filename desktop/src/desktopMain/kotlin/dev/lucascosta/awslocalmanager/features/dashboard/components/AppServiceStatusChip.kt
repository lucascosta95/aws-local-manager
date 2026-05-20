package dev.lucascosta.awslocalmanager.features.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import dev.lucascosta.awslocalmanager.data.model.health.AppServiceStatus
import dev.lucascosta.awslocalmanager.i18n.LocalStrings
import dev.lucascosta.awslocalmanager.theme.LocalAppColors

@Composable
fun AppServiceStatusChip(status: AppServiceStatus) {
    val strings = LocalStrings.current
    val appColors = LocalAppColors.current

    val (bgColor, textColor, label) =
        when (status) {
            AppServiceStatus.ACTIVE ->
                Triple(appColors.success.copy(alpha = 0.2f), appColors.success, strings.statusActive)

            AppServiceStatus.AVAILABLE ->
                Triple(appColors.warning.copy(alpha = 0.2f), appColors.warning, strings.statusAvailable)

            AppServiceStatus.ERROR ->
                Triple(
                    MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
                    MaterialTheme.colorScheme.error,
                    strings.statusError,
                )
        }

    Box(
        modifier =
            Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(bgColor)
                .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
        )
    }
}
