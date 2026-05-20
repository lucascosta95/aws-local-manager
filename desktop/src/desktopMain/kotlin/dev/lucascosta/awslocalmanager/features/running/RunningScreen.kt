package dev.lucascosta.awslocalmanager.features.running

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.lucascosta.awslocalmanager.data.model.aws.RunningResource
import dev.lucascosta.awslocalmanager.data.model.aws.SuccessSnackbarKey
import dev.lucascosta.awslocalmanager.data.model.aws.resourceId
import dev.lucascosta.awslocalmanager.features.running.components.DownConfirmationDialog
import dev.lucascosta.awslocalmanager.features.running.components.GroupHeader
import dev.lucascosta.awslocalmanager.features.running.components.HistoryDialog
import dev.lucascosta.awslocalmanager.features.running.components.PayloadsDialog
import dev.lucascosta.awslocalmanager.features.running.components.PublishPanel
import dev.lucascosta.awslocalmanager.features.running.components.ResourceRow
import dev.lucascosta.awslocalmanager.i18n.LocalStrings
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun RunningScreen(
    viewModel: RunningViewModel = koinInject(),
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    val strings = LocalStrings.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.toggleAutoRefresh(false)
        }
    }

    LaunchedEffect(state.publishFeedback) {
        val feedback = state.publishFeedback ?: return@LaunchedEffect
        val message =
            when (feedback) {
                is PublishFeedback.Error -> strings.publisherErrorMessage.replace("{error}", feedback.message)
                is PublishFeedback.Success ->
                    when (feedback.key) {
                        SuccessSnackbarKey.S3 -> strings.publisherS3SuccessMessage
                        SuccessSnackbarKey.DYNAMODB -> strings.publisherDynamoDbSuccessMessage
                        SuccessSnackbarKey.GENERIC -> strings.publisherSuccessMessage
                    }
            }

        scope.launch {
            snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
            viewModel.clearPublishResult()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Surface(tonalElevation = 1.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(strings.runningTitle, style = MaterialTheme.typography.titleSmall)
                        Text(
                            strings.runningSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(strings.runningAutoRefresh, style = MaterialTheme.typography.labelSmall)
                        Switch(
                            checked = state.isAutoRefresh,
                            onCheckedChange = { viewModel.toggleAutoRefresh(it) },
                            modifier = Modifier.height(24.dp),
                        )
                    }

                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Outlined.Refresh, strings.runningRefresh)
                    }

                    BadgedBox(
                        badge = {
                            if (state.publishHistory.isNotEmpty()) {
                                Badge { Text(state.publishHistory.size.toString()) }
                            }
                        },
                    ) {
                        OutlinedButton(
                            onClick = viewModel::toggleHistoryDialog,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        ) {
                            Text(strings.runningHistoryButton, style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    OutlinedButton(
                        onClick = viewModel::selectAll,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    ) {
                        Text(strings.runningSelectAll, style = MaterialTheme.typography.labelSmall)
                    }

                    OutlinedButton(
                        onClick = viewModel::requestDownSelected,
                        enabled = state.selectedResources.isNotEmpty(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    ) {
                        Icon(Icons.Outlined.Delete, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(strings.runningDownSelected, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Row(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(modifier = Modifier.weight(0.55f).fillMaxHeight()) {
                    if (state.isLoading) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                CircularProgressIndicator()
                                Text(
                                    strings.runningLoading,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    } else if (state.projectGroups.isEmpty() && state.unassociated.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    Icons.Outlined.CloudOff,
                                    null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(40.dp),
                                )
                                Text(
                                    strings.runningNoResources,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                )
                                Text(
                                    strings.runningNoResourcesDetail,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                )
                                Text(
                                    strings.runningNoResourcesHint,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    } else {
                        val listState = rememberLazyListState()
                        Box(modifier = Modifier.fillMaxSize()) {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize().padding(end = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                state.projectGroups.forEach { group ->
                                    item(key = "header:${group.projectName}") {
                                        GroupHeader(title = group.projectName)
                                    }
                                    items(group.resources, key = { "res:${it.type.id}:${it.name}" }) { resource ->
                                        val resId = resource.resourceId()
                                        ResourceRow(
                                            resource = resource,
                                            isSelected = resId in state.selectedResources,
                                            isPublishTarget = state.publishTarget == resource,
                                            onToggle = { viewModel.toggleResource(resId) },
                                            onDown = { viewModel.requestDownResource(resource) },
                                            onPublish = publishCallback(resource, viewModel),
                                        )
                                    }
                                }

                                if (state.unassociated.isNotEmpty()) {
                                    item(key = "header:unassociated") {
                                        GroupHeader(title = strings.runningUnassociated)
                                    }
                                    items(state.unassociated, key = { "res:${it.type.id}:${it.name}" }) { resource ->
                                        val resId = resource.resourceId()
                                        ResourceRow(
                                            resource = resource,
                                            isSelected = resId in state.selectedResources,
                                            isPublishTarget = state.publishTarget == resource,
                                            onToggle = { viewModel.toggleResource(resId) },
                                            onDown = { viewModel.requestDownResource(resource) },
                                            onPublish = publishCallback(resource, viewModel),
                                        )
                                    }
                                }
                            }

                            VerticalScrollbar(
                                adapter = rememberScrollbarAdapter(listState),
                                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(end = 2.dp),
                            )
                        }
                    }
                }

                Box(
                    modifier =
                        Modifier.width(1.dp).fillMaxHeight()
                            .background(MaterialTheme.colorScheme.outlineVariant),
                )

                PublishPanel(
                    state = state,
                    viewModel = viewModel,
                    modifier = Modifier.weight(0.45f).fillMaxHeight(),
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

    if (state.showDownConfirmation) {
        DownConfirmationDialog(
            resources = state.pendingDownResources,
            onConfirm = viewModel::confirmDown,
            onDismiss = viewModel::dismissDown,
        )
    }

    if (state.showHistoryDialog) {
        HistoryDialog(
            history = state.publishHistory,
            onToggleExpand = viewModel::togglePublishHistoryItem,
            onClear = viewModel::clearHistory,
            onClose = viewModel::toggleHistoryDialog,
        )
    }

    if (state.showPayloadsDialog) {
        PayloadsDialog(
            payloads = state.filteredPayloads,
            onApply = viewModel::applyPayload,
            onClose = viewModel::closePayloadsDialog,
        )
    }
}

private fun publishCallback(
    resource: RunningResource,
    viewModel: RunningViewModel,
): (() -> Unit)? =
    if (resource.type.publishableViaJson || resource.type.hasFilePublish) {
        { viewModel.selectPublishTarget(resource) }
    } else {
        null
    }
