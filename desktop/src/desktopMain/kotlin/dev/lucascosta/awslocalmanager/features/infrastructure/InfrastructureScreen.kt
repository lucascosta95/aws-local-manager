package dev.lucascosta.awslocalmanager.features.infrastructure

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOff
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.lucascosta.awslocalmanager.data.model.aws.ResourceRunningStatus
import dev.lucascosta.awslocalmanager.data.model.project.InfraLogStrings
import dev.lucascosta.awslocalmanager.data.model.project.InfraProject
import dev.lucascosta.awslocalmanager.features.infrastructure.components.ActionButtons
import dev.lucascosta.awslocalmanager.features.infrastructure.components.CreateTemplateDialog
import dev.lucascosta.awslocalmanager.features.infrastructure.components.InfraHeader
import dev.lucascosta.awslocalmanager.features.infrastructure.components.LogPanel
import dev.lucascosta.awslocalmanager.features.infrastructure.components.ResourceListHeader
import dev.lucascosta.awslocalmanager.features.infrastructure.components.ResourceRow
import dev.lucascosta.awslocalmanager.i18n.LocalStrings
import org.koin.compose.koinInject

@Composable
fun InfrastructureScreen(
    project: InfraProject,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: InfrastructureViewModel = koinInject()
    LaunchedEffect(project) { viewModel.loadProject(project) }

    val state by viewModel.state.collectAsState()
    val strings = LocalStrings.current
    val logStrings = InfraLogStrings.from(strings)
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.templateCreatedFile) {
        state.templateCreatedFile?.let { fileName ->
            snackbarHostState.showSnackbar(strings.infraTemplateCreatedFmt.format(fileName))
            viewModel.clearTemplateFeedback()
        }
    }

    if (state.project == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(Icons.Outlined.FolderOff, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(strings.infraNoProject, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = onBack) { Text(strings.infraBack) }
            }
        }
        return
    }

    val project = state.project ?: return
    val visibleResources by viewModel.visibleResources.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            InfraHeader(
                projectName = project.name,
                projectPath = project.directory.absolutePath,
                onBack = onBack,
                onRefresh = viewModel::refresh,
            )

            Row(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(
                    modifier = Modifier.weight(0.55f).fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ResourceListHeader(
                        totalCount = project.resources.size,
                        selectedCount = state.selectedResources.size,
                        typeFilter = state.typeFilter,
                        availableTypes = state.availableTypes,
                        onSelectAll = viewModel::selectAll,
                        onDeselectAll = viewModel::deselectAll,
                        onFilterChange = viewModel::setTypeFilter,
                    )

                    if (visibleResources.isEmpty()) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(strings.infraNoResources, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        val listState = rememberLazyListState()
                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize().padding(end = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                items(visibleResources, key = { it.tfLabel }) { resource ->
                                    ResourceRow(
                                        resource = resource,
                                        isSelected = resource.tfLabel in state.selectedResources,
                                        status = state.resourceStatuses[resource.tfLabel] ?: ResourceOpStatus.IDLE,
                                        runningStatus =
                                            if (state.hasRunOperation) {
                                                ResourceRunningStatus.UNKNOWN
                                            } else {
                                                state.runningStatus[resource.tfLabel]
                                                    ?: ResourceRunningStatus.UNKNOWN
                                            },
                                        onToggle = { viewModel.toggleResource(resource.tfLabel) },
                                    )
                                }
                            }

                            VerticalScrollbar(
                                adapter = rememberScrollbarAdapter(listState),
                                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier.weight(0.45f).fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val unsupportedSelectedCount by remember(state.selectedResources, state.project) {
                        derivedStateOf {
                            state.project?.resources?.count {
                                it.tfLabel in state.selectedResources && !it.isSupported
                            } ?: 0
                        }
                    }
                    val allSelectedUnsupported by remember(unsupportedSelectedCount, state.selectedResources) {
                        derivedStateOf {
                            state.selectedResources.isNotEmpty() && unsupportedSelectedCount == state.selectedResources.size
                        }
                    }

                    ActionButtons(
                        isRunning = state.isRunning,
                        selectedCount = state.selectedResources.size,
                        allUnsupported = allSelectedUnsupported,
                        nothingSelected = state.selectedResources.isEmpty(),
                        onUpDirect = { viewModel.upAllDirect(logStrings) },
                        onCreateTemplate = viewModel::showCreateTemplate,
                    )

                    if (allSelectedUnsupported) {
                        UnsupportedSelectionWarning(message = strings.infraAllSelectedUnsupportedWarning)
                    } else if (unsupportedSelectedCount > 0) {
                        UnsupportedSelectionWarning(
                            message = strings.infraUnsupportedSelectionWarning.replace(
                                "{count}",
                                unsupportedSelectedCount.toString(),
                            ),
                        )
                    }

                    HorizontalDivider()

                    Text(
                        strings.infraLogTitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )

                    LogPanel(
                        lines = state.logLines,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

    if (state.showCreateTemplateDialog) {
        CreateTemplateDialog(
            templateType = state.templateType,
            fileName = state.templateFileName,
            onTypeChange = viewModel::setTemplateType,
            onFileNameChange = viewModel::setTemplateFileName,
            onCreate = viewModel::createTemplate,
            onDismiss = viewModel::dismissCreateTemplate,
        )
    }
}

@Composable
private fun UnsupportedSelectionWarning(message: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }
}
