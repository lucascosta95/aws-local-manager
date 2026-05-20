package dev.lucascosta.awslocalmanager.features.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.lucascosta.awslocalmanager.BuildConfig
import dev.lucascosta.awslocalmanager.components.openDirectoryChooser
import dev.lucascosta.awslocalmanager.constants.AppConstants.ENGLISH
import dev.lucascosta.awslocalmanager.constants.AppConstants.PORTUGUESE
import dev.lucascosta.awslocalmanager.i18n.LocalStrings
import dev.lucascosta.awslocalmanager.theme.AppTheme
import dev.lucascosta.awslocalmanager.theme.LocalAppColors
import org.koin.compose.koinInject

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = koinInject()) {
    val state by viewModel.state.collectAsState()
    val strings = LocalStrings.current
    val appColors = LocalAppColors.current
    val scrollState = rememberScrollState()

    val pollingOptions =
        listOf(
            5 to strings.settingsPolling5s,
            10 to strings.settingsPolling10s,
            30 to strings.settingsPolling30s,
            0 to strings.settingsPollingManual,
        )

    val languageOptions =
        listOf(
            PORTUGUESE to strings.settingsLanguagePtBr,
            ENGLISH to strings.settingsLanguageEnUs,
        )

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text(
            strings.settingsTitle,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )

        SettingsSection(title = strings.settingsEndpoint) {
            OutlinedTextField(
                value = state.endpointDraft,
                onValueChange = viewModel::updateEndpointDraft,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(strings.settingsEndpointHint, style = MaterialTheme.typography.bodyMedium) },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium,
            )
        }

        SettingsSection(title = strings.settingsTheme) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(AppTheme.DARK to strings.settingsThemeDark, AppTheme.LIGHT to strings.settingsThemeLight)
                    .forEach { (theme, label) ->
                        FilterChip(
                            selected = state.preferences.theme == theme,
                            onClick = { viewModel.setTheme(theme) },
                            label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                        )
                    }
            }
        }

        SettingsSection(title = strings.settingsLanguage) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                languageOptions.forEach { (tag, label) ->
                    FilterChip(
                        selected = state.preferences.language == tag,
                        onClick = { viewModel.setLanguage(tag) },
                        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                    )
                }
            }
        }

        SettingsSection(title = strings.settingsPollingInterval) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                pollingOptions.forEach { (seconds, label) ->
                    FilterChip(
                        selected = state.pollingIntervalDraft == seconds,
                        onClick = { viewModel.updatePollingDraft(seconds) },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                    )
                }
            }
        }

        SettingsSection(
            title = strings.settingsMaxHistory,
            subtitle = strings.settingsMaxHistoryLabel.replace("{count}", state.maxHistoryDraft.toString()),
        ) {
            Slider(
                value = state.maxHistoryDraft.toFloat(),
                onValueChange = { viewModel.updateMaxHistoryDraft(it.toInt()) },
                valueRange = 10f..100f,
                steps = 8,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        HorizontalDivider()

        SettingsSection(title = strings.settingsProjectsDir, subtitle = strings.settingsProjectsDirHint) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedTextField(
                    value = state.projectsDirDraft,
                    onValueChange = viewModel::updateProjectsDirDraft,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium,
                )
                OutlinedButton(onClick = {
                    viewModel.selectProjectsDir { openDirectoryChooser() }
                }) {
                    Icon(Icons.Outlined.FolderOpen, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(strings.settingsSelectDir, style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        SettingsSection(title = strings.settingsAutoCheckEnv) {
            Switch(
                checked = state.autoCheckEnvDraft,
                onCheckedChange = viewModel::updateAutoCheckEnvDraft,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(onClick = viewModel::save) {
                Text(strings.settingsSave)
            }

            OutlinedButton(onClick = viewModel::resetToDefaults) {
                Text(strings.settingsReset)
            }

            AnimatedVisibility(
                visible = state.isSaved,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Text(
                    strings.settingsSaved,
                    style = MaterialTheme.typography.labelMedium,
                    color = appColors.success,
                )
            }
        }

        Text(
            text = "AWS Local Manager v${BuildConfig.APP_VERSION}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )

        if (subtitle != null) {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        content()
    }
}
