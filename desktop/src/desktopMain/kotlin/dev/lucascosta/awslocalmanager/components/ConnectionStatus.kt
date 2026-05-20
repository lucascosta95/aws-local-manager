package dev.lucascosta.awslocalmanager.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import dev.lucascosta.awslocalmanager.constants.AppConstants.CONNECTION_DOT
import dev.lucascosta.awslocalmanager.i18n.LocalStrings
import dev.lucascosta.awslocalmanager.theme.LocalAppColors

@Composable
fun ConnectionStatus(
    isConnected: Boolean,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val appColors = LocalAppColors.current
    val dotColor by animateColorAsState(
        targetValue = if (isConnected) appColors.success else MaterialTheme.colorScheme.error,
        animationSpec = tween(300),
        label = CONNECTION_DOT,
    )

    Row(
        modifier =
            modifier
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor),
        )

        Text(
            text = if (isConnected) strings.topBarConnectionOnline else strings.topBarConnectionOffline,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
