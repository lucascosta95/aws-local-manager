package dev.lucascosta.awslocalmanager.features.inspector

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.lucascosta.awslocalmanager.components.ResizableTable
import dev.lucascosta.awslocalmanager.components.TableColumn
import dev.lucascosta.awslocalmanager.data.model.inspector.InspectorDetail
import dev.lucascosta.awslocalmanager.data.model.inspector.InspectorResource
import dev.lucascosta.awslocalmanager.data.model.inspector.SfnInspectorExecution
import dev.lucascosta.awslocalmanager.data.model.inspector.SqsInspectorMessage
import dev.lucascosta.awslocalmanager.features.inspector.handler.SqsInspectorHandler
import dev.lucascosta.awslocalmanager.i18n.InspectorStrings
import dev.lucascosta.awslocalmanager.i18n.LocalInspectorStrings
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.koin.compose.koinInject

@Composable
fun InspectorScreen(
    viewModel: InspectorViewModel = koinInject(),
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    val strings = LocalInspectorStrings.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(state.actionSuccess) {
        if (state.actionSuccess) {
            scope.launch {
                snackbarHostState.showSnackbar(strings.inspectorActionSuccess, duration = SnackbarDuration.Short)
                viewModel.clearActionState()
            }
        }
    }

    LaunchedEffect(state.actionError) {
        val error = state.actionError ?: return@LaunchedEffect
        scope.launch {
            snackbarHostState.showSnackbar(
                strings.inspectorActionError.replace("{error}", error),
                duration = SnackbarDuration.Short,
            )
            viewModel.clearActionState()
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(strings.inspectorTitle, style = MaterialTheme.typography.titleSmall)
                            if (state.isLoadingResources) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    strokeWidth = 1.5.dp,
                                )
                            }
                        }
                        Text(
                            strings.inspectorSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Outlined.Refresh, strings.inspectorRefresh)
                    }
                }
            }

            if (state.handlers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        strings.inspectorNoHandlers,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                return@Column
            }

            val selectedIndex = state.handlers.indexOf(state.selectedHandler).coerceAtLeast(0)

            Column(modifier = Modifier.fillMaxWidth()) {
                val tabScrollState = rememberScrollState()
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(tabScrollState),
                ) {
                    state.handlers.forEachIndexed { index, handler ->
                        val isSelected = index == selectedIndex
                        val contentColor =
                            if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        Row(
                            modifier =
                                Modifier
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = ripple(bounded = true),
                                        onClick = { viewModel.selectHandler(handler) },
                                    )
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                handler.icon,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = contentColor,
                            )
                            Text(
                                handler.displayName,
                                style = MaterialTheme.typography.labelLarge,
                                color = contentColor,
                            )
                            if (isSelected && state.resources.isNotEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                ) {
                                    Text(
                                        "${state.resources.size}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                    )
                                }
                            }
                        }
                    }
                }
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(MaterialTheme.colorScheme.primary),
                )
            }

            Row(
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) {
                ResourceListPanel(
                    state = state,
                    onSelectResource = viewModel::selectResource,
                    onRetry = viewModel::refresh,
                    modifier = Modifier.width(280.dp).fillMaxHeight(),
                )

                Box(
                    modifier =
                        Modifier.width(1.dp).fillMaxHeight()
                            .background(MaterialTheme.colorScheme.outlineVariant),
                )

                DetailPanel(
                    state = state,
                    viewModel = viewModel,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun ResourceListPanel(
    state: InspectorUiState,
    onSelectResource: (InspectorResource) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalInspectorStrings.current

    Box(modifier = modifier) {
        when {
            state.isLoadingResources -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }

            state.resourcesError != null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    ErrorCard(
                        error = state.resourcesError,
                        onRetry = onRetry,
                        strings = strings,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }

            state.resources.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        strings.inspectorNoResources,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            else -> {
                val listState = rememberLazyListState()
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize().padding(end = 12.dp),
                        contentPadding = PaddingValues(vertical = 4.dp),
                    ) {
                        items(state.resources, key = { it.id }) { resource ->
                            ResourceListItem(
                                resource = resource,
                                isSelected = state.selectedResource?.id == resource.id,
                                onClick = { onSelectResource(resource) },
                            )
                        }
                    }
                    VerticalScrollbar(
                        adapter = rememberScrollbarAdapter(listState),
                        modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(end = 2.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ResourceListItem(
    resource: InspectorResource,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val strings = LocalInspectorStrings.current
    val summaryText = localizedSummary(resource, strings)
    val summaryColor =
        if (resource.summaryCount != null && resource.summaryCount > 0L) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 2.dp)
                .height(IntrinsicSize.Min)
                .clip(RoundedCornerShape(6.dp))
                .background(
                    if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true),
                    onClick = onClick,
                ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isSelected) {
            Box(
                modifier =
                    Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.primary),
            )
        } else {
            Spacer(modifier = Modifier.width(4.dp))
        }
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
        ) {
            Text(
                resource.name,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                summaryText,
                style = MaterialTheme.typography.labelSmall,
                color = summaryColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (isSelected) {
            Icon(
                Icons.Outlined.ChevronRight,
                null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp).padding(end = 8.dp),
            )
        }
    }
}

@Composable
private fun DetailPanel(
    state: InspectorUiState,
    viewModel: InspectorViewModel,
    modifier: Modifier = Modifier,
) {
    val strings = LocalInspectorStrings.current

    Box(modifier = modifier) {
        when {
            state.selectedResource == null -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    state.selectedHandler?.let { handler ->
                        Icon(
                            handler.icon,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                    Text(
                        strings.inspectorSelectResource,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    state.selectedHandler?.let { handler ->
                        Spacer(Modifier.height(4.dp))
                        Text(
                            strings.inspectorSelectResourceHint.replace("{service}", handler.displayName),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        )
                    }
                }
            }

            state.isLoadingDetail -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    Spacer(Modifier.height(12.dp))
                    Text(
                        strings.inspectorLoadingDetail.replace("{name}", state.selectedResource?.name ?: ""),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            state.detailError != null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    ErrorCard(
                        error = state.detailError,
                        onRetry = viewModel::retryDetail,
                        strings = strings,
                        modifier = Modifier.padding(24.dp),
                    )
                }
            }

            state.detail != null -> {
                when (val detail = state.detail!!) {
                    is InspectorDetail.SqsDetail ->
                        SqsDetailView(
                            detail = detail,
                            isLoadingAction = state.isLoadingSubDetail,
                            onPurge = { viewModel.performAction(SqsInspectorHandler.ACTION_PURGE) },
                            modifier = Modifier.fillMaxSize(),
                        )

                    is InspectorDetail.StepFunctionsDetail ->
                        StepFunctionsDetailView(
                            detail = detail,
                            isLoadingSubDetail = state.isLoadingSubDetail,
                            onSelectExecution = { viewModel.selectDetailItem(it.executionArn) },
                            modifier = Modifier.fillMaxSize(),
                        )

                    is InspectorDetail.DynamoDetail ->
                        DynamoDetailView(
                            detail = detail,
                            isLoadingMore = state.isLoadingSubDetail,
                            onLoadMore = viewModel::loadMoreItems,
                            modifier = Modifier.fillMaxSize(),
                        )

                    is InspectorDetail.S3Detail ->
                        S3DetailView(
                            detail = detail,
                            resourceName = state.selectedResource?.name ?: "",
                            onNavigate = viewModel::navigateToPath,
                            onNavigateUp = {
                                val prefix = detail.currentPrefix
                                val parent =
                                    prefix.trimEnd('/').substringBeforeLast('/').let {
                                        if (it.isEmpty()) "" else "$it/"
                                    }
                                viewModel.navigateToPath(parent)
                            },
                            modifier = Modifier.fillMaxSize(),
                        )

                    is InspectorDetail.ElastiCacheDetail ->
                        ElastiCacheDetailView(
                            detail = detail,
                            isLoadingMore = state.isLoadingSubDetail,
                            onLoadMore = viewModel::loadMoreItems,
                            modifier = Modifier.fillMaxSize(),
                        )
                }
            }

            else -> Unit
        }
    }
}

@Composable
private fun SqsDetailView(
    detail: InspectorDetail.SqsDetail,
    isLoadingAction: Boolean,
    onPurge: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalInspectorStrings.current

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                run {
                    val count = detail.messages.size
                    val template =
                        if (count == 1) {
                            strings.inspectorSqsMessagesSingular
                        } else {
                            strings.inspectorSqsMessagesPlural
                        }

                    template.replace("{count}", count.toString())
                },
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.weight(1f),
            )
            OutlinedButton(
                onClick = onPurge,
                enabled = !isLoadingAction && detail.messages.isNotEmpty(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            ) {
                if (isLoadingAction) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(4.dp))
                }
                Text(strings.inspectorSqsPurge, style = MaterialTheme.typography.labelSmall)
            }
        }

        HorizontalDivider()

        if (detail.messages.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Outlined.Info,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            detail.queueUrl,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Text(
                    strings.inspectorSqsEmpty,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            val listState = rememberLazyListState()
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(end = 12.dp)) {
                    items(detail.messages, key = { it.messageId }) { message ->
                        SqsMessageItem(message = message)
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
                VerticalScrollbar(
                    adapter = rememberScrollbarAdapter(listState),
                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(end = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun SqsMessageItem(message: SqsInspectorMessage) {
    val strings = LocalInspectorStrings.current
    var expanded by remember(message.messageId) { mutableStateOf(false) }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true),
                    onClick = { expanded = !expanded },
                )
                .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                strings.inspectorSqsMessageId.replace("{id}", message.messageId.take(16) + "…"),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = if (expanded) Icons.AutoMirrored.Outlined.ArrowBack else Icons.Outlined.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Text(
            text =
                if (expanded) {
                    message.body
                } else {
                    message.body.take(120)
                        .let { if (message.body.length > 120) "$it…" else it }
                },
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurface,
        )

        if (expanded && message.attributes.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            message.attributes.forEach { (k, v) ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(k, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Text(
                        v,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun StepFunctionsDetailView(
    detail: InspectorDetail.StepFunctionsDetail,
    isLoadingSubDetail: Boolean,
    onSelectExecution: (SfnInspectorExecution) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalInspectorStrings.current

    if (detail.executions.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                strings.inspectorSfnEmpty,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    val scrollState = rememberScrollState()
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(end = 12.dp)
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("RUNNING", "SUCCEEDED", "FAILED", "TIMED_OUT", "ABORTED").forEach { status ->
                        val count = detail.statusCounts[status] ?: 0
                        if (count > 0) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = sfnStatusColor(status).copy(alpha = 0.15f),
                            ) {
                                Text(
                                    "${localizedSfnStatus(status, strings)} $count",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = sfnStatusColor(status),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }
                        }
                    }
                }

                val tableHeightDp = ((detail.executions.size * 32) + 40).coerceIn(120, 300).dp
                ResizableTable(
                    columns =
                        listOf(
                            TableColumn(
                                header = strings.inspectorSfnColumnExecution,
                                initialWidthFraction = 0.50f,
                                minWidthDp = 120.dp,
                            ),
                            TableColumn(
                                header = strings.inspectorSfnColumnStatus,
                                initialWidthFraction = 0.20f,
                                minWidthDp = 80.dp,
                            ),
                            TableColumn(
                                header = strings.inspectorSfnColumnStart,
                                initialWidthFraction = 0.30f,
                                minWidthDp = 100.dp,
                            ),
                        ),
                    rows =
                        detail.executions.map { exec ->
                            listOf(
                                exec.name,
                                localizedSfnStatus(exec.status, strings),
                                exec.startDate.take(19).replace("T", " "),
                            )
                        },
                    onRowClick = { i -> onSelectExecution(detail.executions[i]) },
                    selectedRowIndex =
                        detail.executions
                            .indexOfFirst { it.executionArn == detail.selectedExecution?.executionArn }
                            .takeIf { it >= 0 },
                    emptyMessage = strings.inspectorSfnEmpty,
                    modifier = Modifier.fillMaxWidth().height(tableHeightDp),
                )
            }

            when {
                detail.selectedExecution == null -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            strings.inspectorSfnSelectExecution,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                isLoadingSubDetail -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }

                else -> {
                    SfnJsonBlock(
                        label = strings.inspectorSfnInput,
                        content = detail.executionInput,
                        noDataText = strings.inspectorSfnNoData,
                    )
                    SfnJsonBlock(
                        label = strings.inspectorSfnOutput,
                        content = detail.executionOutput,
                        noDataText = strings.inspectorSfnNoData,
                    )
                }
            }
        }

        VerticalScrollbar(
            adapter = rememberScrollbarAdapter(scrollState),
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(end = 2.dp),
        )
    }
}

@Composable
private fun sfnStatusColor(status: String): Color =
    when (status) {
        "RUNNING" -> MaterialTheme.colorScheme.primary
        "SUCCEEDED" -> Color(0xFF4CAF50)
        "FAILED", "ABORTED" -> MaterialTheme.colorScheme.error
        "TIMED_OUT" -> Color(0xFFFFC107)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

@Composable
private fun SfnJsonBlock(
    label: String,
    content: String?,
    noDataText: String,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ) {
            Text(
                text = if (content != null) formatJson(content) else noDataText,
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

private val prettyJson = Json { prettyPrint = true }

private fun formatJson(raw: String): String =
    runCatching {
        val element = Json.parseToJsonElement(raw)
        prettyJson.encodeToString(JsonElement.serializer(), element)
    }.getOrDefault(raw)

@Composable
private fun DynamoDetailView(
    detail: InspectorDetail.DynamoDetail,
    isLoadingMore: Boolean,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalInspectorStrings.current

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                run {
                    val count = detail.items.size
                    val template =
                        if (count == 1) {
                            strings.inspectorDynamoItemsSingular
                        } else {
                            strings.inspectorDynamoItemsPlural
                        }

                    template.replace("{count}", count.toString())
                },
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.weight(1f),
            )
        }
        HorizontalDivider()

        if (detail.items.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    strings.inspectorDynamoEmpty,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            val tableColumns = buildDynamoColumns(detail.columns)
            val tableRows = detail.items.map { item -> detail.columns.map { col -> item[col] ?: "" } }

            ResizableTable(
                columns = tableColumns,
                rows = tableRows,
                modifier = Modifier.weight(1f),
            )

            if (detail.hasMore) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    OutlinedButton(
                        onClick = onLoadMore,
                        enabled = !isLoadingMore,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    ) {
                        if (isLoadingMore) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(4.dp))
                        }
                        Text(strings.inspectorDynamoLoadMore, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

private fun buildDynamoColumns(headers: List<String>): List<TableColumn> {
    if (headers.isEmpty()) {
        return emptyList()
    }

    val fraction = 1f / headers.size
    return headers.map { TableColumn(header = it, initialWidthFraction = fraction) }
}

@Composable
private fun S3DetailView(
    detail: InspectorDetail.S3Detail,
    resourceName: String,
    onNavigate: (String) -> Unit,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalInspectorStrings.current

    Column(modifier = modifier) {
        S3Breadcrumb(
            bucketName = resourceName,
            prefix = detail.currentPrefix,
            onNavigate = onNavigate,
            onNavigateUp = onNavigateUp,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        )
        HorizontalDivider()

        ResizableTable(
            columns =
                listOf(
                    TableColumn(header = strings.inspectorS3ColumnName, initialWidthFraction = 0.55f),
                    TableColumn(header = strings.inspectorS3ColumnSize, initialWidthFraction = 0.20f),
                    TableColumn(header = strings.inspectorS3ColumnDate, initialWidthFraction = 0.25f),
                ),
            rows =
                detail.entries.map { obj ->
                    listOf(
                        (if (obj.isPrefix) "📁 " else "") + obj.displayName,
                        if (obj.isPrefix) "—" else formatBytes(obj.sizeBytes),
                        if (obj.isPrefix) "—" else obj.lastModified.take(19).replace("T", " "),
                    )
                },
            onRowClick = { i ->
                val obj = detail.entries[i]
                if (obj.isPrefix) onNavigate(obj.key)
            },
            emptyMessage = strings.inspectorS3Empty,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        )
    }
}

@Composable
private fun S3Breadcrumb(
    bucketName: String,
    prefix: String,
    onNavigate: (String) -> Unit,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (prefix.isNotBlank()) {
            IconButton(onClick = onNavigateUp, modifier = Modifier.size(24.dp)) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        }
        Text(
            bucketName,
            style = MaterialTheme.typography.labelMedium,
            color = if (prefix.isBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary,
            modifier = if (prefix.isNotBlank()) Modifier.clickable { onNavigate("") } else Modifier,
        )
        val parts = prefix.trimEnd('/').split('/').filter { it.isNotBlank() }
        parts.forEachIndexed { index, part ->
            Icon(
                Icons.Outlined.ChevronRight,
                null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val partPrefix = parts.take(index + 1).joinToString("/") + "/"
            val isLast = index == parts.lastIndex
            Text(
                part,
                style = MaterialTheme.typography.labelMedium,
                color = if (isLast) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary,
                modifier = if (!isLast) Modifier.clickable { onNavigate(partPrefix) } else Modifier,
            )
        }
    }
}

@Composable
private fun ErrorCard(
    error: String,
    onRetry: () -> Unit,
    strings: InspectorStrings,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                Icons.Outlined.Info,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.error,
            )
            Text(
                error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
            OutlinedButton(
                onClick = onRetry,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            ) {
                Text(strings.inspectorRetry, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

private fun localizedSummary(
    resource: InspectorResource,
    strings: InspectorStrings,
): String =
    when (resource.summaryType) {
        "sqs" -> {
            val count = resource.summaryCount
            when {
                count == null -> strings.inspectorSummarySqsUnknown
                count == 1L -> strings.inspectorSummarySqsSingular.replace("{count}", "1")
                else -> strings.inspectorSummarySqsPlural.replace("{count}", count.toString())
            }
        }

        "dynamo" -> {
            val count = resource.summaryCount ?: 0L
            if (count == 1L) {
                strings.inspectorSummaryDynamoSingular.replace("{count}", "1")
            } else {
                strings.inspectorSummaryDynamoPlural.replace("{count}", count.toString())
            }
        }

        "sfn" -> strings.inspectorSummarySfn
        "s3" -> strings.inspectorSummaryS3
        "redis", "memcached" -> strings.inspectorSummaryElastiCache
        else -> ""
    }

private fun localizedSfnStatus(
    status: String,
    strings: InspectorStrings,
): String =
    when (status) {
        "RUNNING" -> strings.inspectorSfnStatusRunning
        "SUCCEEDED" -> strings.inspectorSfnStatusSucceeded
        "FAILED" -> strings.inspectorSfnStatusFailed
        "TIMED_OUT" -> strings.inspectorSfnStatusTimedOut
        "ABORTED" -> strings.inspectorSfnStatusAborted
        else -> status
    }

private fun formatBytes(bytes: Long): String =
    when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${"%.1f".format(bytes / 1024.0)} KB"
        bytes < 1024 * 1024 * 1024 -> "${"%.1f".format(bytes / (1024.0 * 1024))} MB"
        else -> "${"%.1f".format(bytes / (1024.0 * 1024 * 1024))} GB"
    }

@Composable
private fun ElastiCacheDetailView(
    detail: InspectorDetail.ElastiCacheDetail,
    isLoadingMore: Boolean,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalInspectorStrings.current

    if (detail.engine.isBlank() && detail.status.isBlank()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                strings.inspectorElastiCacheEmpty,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    Column(modifier = modifier) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            listOfNotNull(
                strings.inspectorElastiCacheEngine to detail.engine.ifBlank { null },
                strings.inspectorElastiCacheStatus to detail.status.ifBlank { null },
                strings.inspectorElastiCacheNodeType to detail.nodeType.ifBlank { null },
                strings.inspectorElastiCacheNodes to detail.numNodes.takeIf { it > 0 }?.toString(),
                strings.inspectorElastiCacheVersion to detail.engineVersion.ifBlank { null },
                strings.inspectorElastiCacheEndpoint to detail.endpoint,
                strings.inspectorElastiCachePort to detail.port?.toString(),
            ).forEach { (label, value) ->
                if (value != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            label,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.widthIn(min = 120.dp),
                        )
                        Text(value, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        HorizontalDivider()

        Text(
            strings.inspectorElastiCacheKeys,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        HorizontalDivider()

        if (detail.cacheEntries.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    strings.inspectorElastiCacheNoKeys,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            val tableColumns =
                listOf(
                    TableColumn(strings.inspectorElastiCacheKeyColumn, 0.40f),
                    TableColumn(strings.inspectorElastiCacheValueColumn, 0.45f),
                    TableColumn(strings.inspectorElastiCacheTtlColumn, 0.15f),
                )
            val tableRows =
                detail.cacheEntries.map { entry ->
                    listOf(
                        entry.key,
                        entry.value ?: "",
                        when (val ttl = entry.ttl) {
                            null -> strings.inspectorElastiCacheNoExpiry
                            -2L -> "-"
                            else -> "${ttl}s"
                        },
                    )
                }

            ResizableTable(
                columns = tableColumns,
                rows = tableRows,
                modifier = Modifier.weight(1f),
            )
        }

        if (detail.hasMore) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                OutlinedButton(
                    onClick = onLoadMore,
                    enabled = !isLoadingMore,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                ) {
                    if (isLoadingMore) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(strings.inspectorElastiCacheLoadMore, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
