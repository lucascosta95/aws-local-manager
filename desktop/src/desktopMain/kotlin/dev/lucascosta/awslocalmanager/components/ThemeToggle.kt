package dev.lucascosta.awslocalmanager.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import dev.lucascosta.awslocalmanager.i18n.LocalStrings
import dev.lucascosta.awslocalmanager.theme.AppTheme

@Composable
fun ThemeToggle(
    currentTheme: AppTheme,
    onToggle: (AppTheme) -> Unit,
) {
    val strings = LocalStrings.current
    IconButton(
        onClick = {
            onToggle(if (currentTheme == AppTheme.DARK) AppTheme.LIGHT else AppTheme.DARK)
        },
    ) {
        Icon(
            imageVector = if (currentTheme == AppTheme.DARK) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
            contentDescription = strings.topBarThemeToggle,
            tint = MaterialTheme.colorScheme.onBackground,
        )
    }
}
