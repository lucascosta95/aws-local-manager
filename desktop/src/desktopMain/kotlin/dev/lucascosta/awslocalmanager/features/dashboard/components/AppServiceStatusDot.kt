package dev.lucascosta.awslocalmanager.features.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.lucascosta.awslocalmanager.data.model.health.AppServiceStatus
import dev.lucascosta.awslocalmanager.i18n.LocalStrings
import dev.lucascosta.awslocalmanager.theme.LocalAppColors

@Composable
fun appServiceStatusLabel(status: AppServiceStatus): String =
    when (status) {
        AppServiceStatus.ACTIVE -> LocalStrings.current.statusActive
        AppServiceStatus.AVAILABLE -> LocalStrings.current.statusAvailable
        AppServiceStatus.ERROR -> LocalStrings.current.statusError
    }

// A service without resources is drawn as a hollow ring so it does not read as "on" next to a service in use.
@Composable
fun AppServiceStatusDot(
    status: AppServiceStatus,
    modifier: Modifier = Modifier,
    size: Dp = 10.dp,
    outline: Color = Color.Transparent,
) {
    val base = modifier.size(size).background(outline, CircleShape).padding(2.dp)
    when (status) {
        AppServiceStatus.ACTIVE -> Box(base.background(LocalAppColors.current.success, CircleShape))
        AppServiceStatus.ERROR -> Box(base.background(MaterialTheme.colorScheme.error, CircleShape))
        AppServiceStatus.AVAILABLE -> Box(base.border(1.5.dp, MaterialTheme.colorScheme.onSurfaceVariant, CircleShape))
    }
}
