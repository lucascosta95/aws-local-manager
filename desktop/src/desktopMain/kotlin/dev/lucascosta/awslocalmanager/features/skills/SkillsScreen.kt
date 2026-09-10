package dev.lucascosta.awslocalmanager.features.skills

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.lucascosta.awslocalmanager.data.model.skill.CatalogSource
import dev.lucascosta.awslocalmanager.features.skills.components.InstallConfirmDialog
import dev.lucascosta.awslocalmanager.features.skills.components.SkillListItem
import dev.lucascosta.awslocalmanager.features.skills.components.SkillPreviewDialog
import dev.lucascosta.awslocalmanager.features.skills.components.SkillTargetRow
import dev.lucascosta.awslocalmanager.i18n.LocalSkillsStrings
import dev.lucascosta.awslocalmanager.theme.LocalAppColors
import org.koin.compose.koinInject

@Composable
fun SkillsScreen(
    viewModel: SkillsViewModel = koinInject(),
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    val strings = LocalSkillsStrings.current

    Column(modifier = modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SkillsHeader(state = state, onRefresh = viewModel::load)
        HorizontalDivider()

        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(32.dp))
            }
        } else if (state.skills.isEmpty()) {
            Text(strings.skillsEmpty, style = MaterialTheme.typography.bodyMedium)
        } else {
            Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LazyColumn(
                    modifier = Modifier.width(280.dp).fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.skills) { entry ->
                        SkillListItem(
                            entry = entry,
                            isSelected = entry.id == state.selectedSkill?.id,
                            onClick = { viewModel.selectSkill(entry) },
                        )
                    }
                }
                VerticalDivider()
                SkillDetail(state = state, viewModel = viewModel, modifier = Modifier.weight(1f))
            }
        }
    }

    val entry = state.selectedSkill
    if (state.showConfirmDialog && entry != null) {
        InstallConfirmDialog(
            paths = state.targets.filter { it.target.id in state.selectedTargets }.map { it.path },
            onConfirm = viewModel::confirmInstall,
            onDismiss = viewModel::dismissConfirm,
        )
    }

    state.previewContent?.let { content ->
        SkillPreviewDialog(content = content, onDismiss = viewModel::dismissPreview)
    }
}

@Composable
private fun SkillsHeader(
    state: SkillsUiState,
    onRefresh: () -> Unit,
) {
    val strings = LocalSkillsStrings.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(strings.skillsTitle, style = MaterialTheme.typography.titleMedium)
            Text(
                strings.skillsSubtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        AssistChip(
            onClick = onRefresh,
            label = {
                Text(
                    if (state.source == CatalogSource.REMOTE) strings.skillsSourceRemote else strings.skillsSourceBundled,
                    style = MaterialTheme.typography.labelSmall,
                )
            },
        )
        IconButton(onClick = onRefresh) {
            Icon(imageVector = Icons.Outlined.Refresh, contentDescription = strings.skillsRefresh)
        }
    }
}

@Composable
private fun SkillDetail(
    state: SkillsUiState,
    viewModel: SkillsViewModel,
    modifier: Modifier = Modifier,
) {
    val strings = LocalSkillsStrings.current
    val entry = state.selectedSkill

    if (entry == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(strings.skillsSelectHint, style = MaterialTheme.typography.bodyMedium)
        }
        return
    }

    Column(
        modifier = modifier.fillMaxHeight().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(entry.name, style = MaterialTheme.typography.titleSmall)
        Text(
            strings.skillsVersionFmt.replace("{version}", entry.version),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(entry.description, style = MaterialTheme.typography.bodyMedium)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = viewModel::requestInstall,
                enabled = !state.isInstalling && state.selectedTargets.isNotEmpty(),
            ) {
                Icon(imageVector = Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                Text(strings.skillsInstall, modifier = Modifier.padding(start = 8.dp))
            }
            OutlinedButton(onClick = viewModel::showPreview) { Text(strings.skillsPreview) }
        }

        HorizontalDivider()

        Text(strings.skillsTargets, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Text(
            strings.skillsTargetsHint,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        state.targets.forEach { status ->
            SkillTargetRow(
                status = status,
                availableVersion = entry.version,
                isChecked = status.target.id in state.selectedTargets,
                isBusy = state.isInstalling,
                onToggle = { viewModel.toggleTarget(status.target.id) },
                onUninstall = { viewModel.uninstall(status.target) },
            )
        }

        SkillFeedback(state = state)
    }
}

@Composable
private fun SkillFeedback(state: SkillsUiState) {
    val strings = LocalSkillsStrings.current
    val colors = LocalAppColors.current
    val feedback = state.feedback

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (feedback != null && feedback.installedPaths.isNotEmpty()) {
            Text(
                strings.skillsInstalledFmt
                    .replace("{name}", feedback.skillName)
                    .replace("{count}", feedback.installedPaths.size.toString()),
                style = MaterialTheme.typography.bodySmall,
                color = colors.success,
            )
        }
        if (feedback != null && feedback.removedPaths.isNotEmpty()) {
            Text(
                strings.skillsRemovedFmt
                    .replace("{name}", feedback.skillName)
                    .replace("{count}", feedback.removedPaths.size.toString()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (feedback != null && feedback.failedTargets.isNotEmpty()) {
            Text(
                strings.skillsFailedFmt.replace("{targets}", feedback.failedTargets.joinToString()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        if (state.error != null) {
            Text(strings.skillsErrorContent, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
    }
}
