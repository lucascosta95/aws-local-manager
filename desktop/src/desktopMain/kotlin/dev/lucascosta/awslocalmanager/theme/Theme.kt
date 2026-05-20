package dev.lucascosta.awslocalmanager.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalAppColors = staticCompositionLocalOf { AppSemanticColors() }

private fun semanticColorsFor(darkTheme: Boolean): AppSemanticColors =
    if (darkTheme) {
        AppSemanticColors(
            success = Color(0xFFA6E3A1),
            onSuccess = Color(0xFF0A2A08),
            warning = Color(0xFFF9E2AF),
            onWarning = Color(0xFF2A1A00),
            info = Color(0xFF89B4FA),
            onInfo = Color(0xFF001A3A),
        )
    } else {
        AppSemanticColors(
            success = Color(0xFF2E7D32),
            onSuccess = Color(0xFFFFFFFF),
            warning = Color(0xFFE65100),
            onWarning = Color(0xFFFFFFFF),
            info = Color(0xFF1565C0),
            onInfo = Color(0xFFFFFFFF),
        )
    }

@Composable
fun DesktopAppTheme(
    appTheme: AppTheme = AppTheme.DARK,
    content: @Composable () -> Unit,
) {
    val darkTheme = appTheme == AppTheme.DARK
    CompositionLocalProvider(LocalAppColors provides semanticColorsFor(darkTheme)) {
        MaterialTheme(
            colorScheme = if (darkTheme) AppDarkColorScheme else AppLightColorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}
