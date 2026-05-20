package dev.lucascosta.awslocalmanager.features.project

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import dev.lucascosta.awslocalmanager.components.openDirectoryChooser
import dev.lucascosta.awslocalmanager.data.model.project.InfraProject
import dev.lucascosta.awslocalmanager.data.model.project.ProjectRunningInfo
import dev.lucascosta.awslocalmanager.i18n.LocalStrings
import dev.lucascosta.awslocalmanager.theme.LocalAppColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun ProjectSelectorScreen(
    onProjectOpen: (InfraProject) -> Unit,
    viewModel: ProjectSelectorViewModel = koinInject(),
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    val strings = LocalStrings.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(strings.projectTitle, style = MaterialTheme.typography.titleMedium)
        Text(
            strings.projectSubtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        HorizontalDivider()

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    strings.projectCurrentDir,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )

                Text(
                    text = if (state.currentDir.isBlank()) strings.projectNoDir else state.currentDir,
                    style = MaterialTheme.typography.bodySmall,
                    color =
                        if (state.currentDir.isBlank()) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                )
            }

            OutlinedButton(onClick = {
                scope.launch(Dispatchers.IO) {
                    val path = openDirectoryChooser()
                    if (path != null) viewModel.selectDirectory(path)
                }
            }) {
                Icon(Icons.Outlined.FolderOpen, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(strings.projectSelectDir)
            }

            IconButton(onClick = viewModel::refresh) {
                Icon(Icons.Outlined.Refresh, strings.projectRefresh)
            }
        }

        if (state.isScanning) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (state.projects.isEmpty() && state.currentDir.isNotBlank()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(strings.projectNoProjects, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.projects, key = { it.name }) { project ->
                    ProjectCard(
                        project = project,
                        runningInfo = state.projectRunningInfo[project.name],
                        onOpen = { onProjectOpen(project) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ProjectCard(
    project: InfraProject,
    runningInfo: ProjectRunningInfo?,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val typesByCount = project.resources.groupBy { it.resourceType?.id ?: it.rawAwsType }

    val typeBreakdown =
        typesByCount.entries
            .sortedBy { it.key }
            .joinToString(" · ") { (typeName, resources) -> "${resources.size} $typeName" }

    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                Icons.Outlined.Folder,
                null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 2.dp),
            )

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(project.name, style = MaterialTheme.typography.titleSmall)
                Text(
                    project.directory.absolutePath,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )

                Text(
                    "${project.resources.size} ${strings.projectResourcesLabel} · $typeBreakdown",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (runningInfo != null) {
                    ProjectRunningStatus(info = runningInfo)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    typesByCount.keys.forEach { typeName ->
                        TypeChip(typeName)
                    }
                }
            }

            OutlinedButton(onClick = onOpen) {
                Text(strings.projectOpen)
            }
        }
    }
}

@Composable
private fun ProjectRunningStatus(
    info: ProjectRunningInfo,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val appColors = LocalAppColors.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier,
    ) {
        if (info.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(12.dp),
                strokeWidth = 1.5.dp,
                color = MaterialTheme.colorScheme.primary,
            )

            Text(
                text = strings.checkingResources,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            )
        } else {
            if (info.totalRunning > 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).background(appColors.success, CircleShape))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = strings.runningCount.replace("{count}", info.totalRunning.toString()),
                        style = MaterialTheme.typography.labelSmall,
                        color = appColors.success,
                    )
                }
            }

            if (info.totalNotRunning > 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f), CircleShape),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = strings.notRunningCount.replace("{count}", info.totalNotRunning.toString()),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    )
                }
            }
        }
    }
}

@Composable
private fun TypeChip(
    typeName: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            typeName,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}
