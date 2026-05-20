package dev.lucascosta.awslocalmanager.features.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import dev.lucascosta.awslocalmanager.data.model.health.CheckStatus
import dev.lucascosta.awslocalmanager.data.model.health.PrerequisiteCheck
import dev.lucascosta.awslocalmanager.i18n.LocalStrings
import dev.lucascosta.awslocalmanager.theme.LocalAppColors
import org.koin.compose.koinInject

@Composable
fun SetupScreen(
    onContinue: () -> Unit,
    onNavigateToDashboard: () -> Unit,
    viewModel: SetupViewModel = koinInject(),
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    val strings = LocalStrings.current

    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(strings.setupTitle, style = MaterialTheme.typography.titleMedium)
            Text(
                strings.setupSubtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            HorizontalDivider()

            state.checks.forEach { check ->
                CheckItem(
                    check = check,
                    onFix = { viewModel.fixItem(check.id) },
                )
            }

            if (state.fixLogLines.isNotEmpty()) {
                HorizontalDivider()
                Text(strings.setupFixLog, style = MaterialTheme.typography.labelMedium)
                state.fixLogLines.forEach { line ->
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (!state.isChecking) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (state.allOk) strings.setupEnvReady else strings.setupSomeIssues,
                    style = MaterialTheme.typography.bodySmall,
                    color =
                        if (state.allOk) {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                )
            }
        }

        HorizontalDivider()
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val checksFinished by remember(state.checks) {
                derivedStateOf { state.checks.none { it.status == CheckStatus.CHECKING } }
            }

            val hasProblems by remember(state.checks) {
                derivedStateOf { state.checks.any { it.status != CheckStatus.OK } }
            }

            if (checksFinished && hasProblems) {
                Text(
                    strings.setupContinueAnywayWarning,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!state.isChecking) {
                    OutlinedButton(onClick = viewModel::runChecks) {
                        Icon(Icons.Outlined.Refresh, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(strings.setupCheckAgain)
                    }
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        Text(strings.setupChecking, style = MaterialTheme.typography.bodySmall)
                    }
                }

                if (state.allOk) {
                    Button(onClick = onNavigateToDashboard) {
                        Text(strings.setupGoToDashboard)
                    }
                } else {
                    Button(onClick = onContinue) {
                        Text(strings.setupContinueAnyway)
                    }
                }
            }
        }
    }
}

@Composable
private fun CheckItem(
    check: PrerequisiteCheck,
    onFix: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val appColors = LocalAppColors.current

    val checkName =
        when (check.id) {
            SetupViewModel.ID_DOCKER_INSTALLED -> strings.setupCheckDockerInstalled
            SetupViewModel.ID_DOCKER_RUNNING -> strings.setupCheckDockerRunning
            SetupViewModel.ID_EMULATOR_IMAGE -> strings.emulatorImageCheckName
            SetupViewModel.ID_EMULATOR_RUNNING -> strings.emulatorRunningCheckName
            SetupViewModel.ID_AWS_CLI -> strings.setupCheckAwsCli
            else -> check.id
        }

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(44.dp)
                .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
            when {
                check.isFixing || check.status == CheckStatus.CHECKING ->
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)

                check.status == CheckStatus.OK ->
                    Icon(Icons.Outlined.CheckCircle, null, tint = appColors.success, modifier = Modifier.size(20.dp))

                check.status == CheckStatus.UNKNOWN ->
                    Icon(
                        Icons.AutoMirrored.Outlined.HelpOutline,
                        null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.size(20.dp),
                    )

                else ->
                    Icon(
                        Icons.Outlined.Cancel,
                        null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp),
                    )
            }
        }

        Spacer(Modifier.width(12.dp))

        Text(
            text = checkName,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )

        Box(modifier = Modifier.widthIn(min = 180.dp), contentAlignment = Alignment.CenterEnd) {
            when {
                check.isFixing ->
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)

                check.status == CheckStatus.CHECKING ->
                    Unit

                check.status == CheckStatus.OK ->
                    Text(strings.setupStatusOk, color = appColors.success, style = MaterialTheme.typography.labelMedium)

                check.canAutoFix ->
                    FixableStatusRow(check = check, onFix = onFix, strings = strings)

                else -> {
                    val label = resolveStatusLabel(check, strings)
                    label?.let {
                        Text(
                            it,
                            color =
                                if (check.status == CheckStatus.UNKNOWN) {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                } else {
                                    MaterialTheme.colorScheme.error
                                },
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FixableStatusRow(
    check: PrerequisiteCheck,
    onFix: () -> Unit,
    strings: dev.lucascosta.awslocalmanager.i18n.Strings,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
    ) {
        val label = resolveStatusLabel(check, strings)
        label?.let {
            Text(
                it,
                color =
                    if (check.status == CheckStatus.UNKNOWN) {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                style = MaterialTheme.typography.labelSmall,
            )
            Spacer(Modifier.width(8.dp))
        }

        Button(onClick = onFix) { Text(strings.setupFix, maxLines = 1, softWrap = false) }
    }
}

private fun resolveStatusLabel(
    check: PrerequisiteCheck,
    strings: dev.lucascosta.awslocalmanager.i18n.Strings,
): String? =
    check.detail ?: when (check.status) {
        CheckStatus.MISSING -> strings.setupStatusMissing
        CheckStatus.NOT_RUNNING -> resolveNotRunningLabel(check.id, strings)
        CheckStatus.UNKNOWN -> strings.setupStatusUnknown
        CheckStatus.ERROR -> resolveErrorLabel(check.id, strings)
        else -> null
    }

private fun resolveNotRunningLabel(
    checkId: String,
    strings: dev.lucascosta.awslocalmanager.i18n.Strings,
): String =
    when (checkId) {
        "emulator_image" -> strings.emulatorImageNotFound
        "emulator_running" -> strings.emulatorNotRunning
        else -> strings.setupStatusNotRunning
    }

private fun resolveErrorLabel(
    checkId: String,
    strings: dev.lucascosta.awslocalmanager.i18n.Strings,
): String =
    when (checkId) {
        "emulator_running" -> strings.emulatorStartError
        else -> strings.setupStatusNotRunning
    }
