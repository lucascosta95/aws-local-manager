package dev.lucascosta.awslocalmanager.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.lucascosta.awslocalmanager.i18n.LocalStrings
import dev.lucascosta.awslocalmanager.theme.AppTheme

@Composable
fun TopBar(
    isConnected: Boolean,
    currentTheme: AppTheme,
    currentLanguage: String,
    onThemeToggle: (AppTheme) -> Unit,
    onLanguageSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    Surface(
        modifier = modifier,
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(strings.appName, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            TopBarActions {
                ThemeToggle(currentTheme, onThemeToggle)
                LanguageSelector(currentLanguage, onLanguageSelected)
                ConnectionStatus(isConnected)
            }
        }
    }
}

@Composable
private fun TopBarActions(content: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        content()
    }
}
