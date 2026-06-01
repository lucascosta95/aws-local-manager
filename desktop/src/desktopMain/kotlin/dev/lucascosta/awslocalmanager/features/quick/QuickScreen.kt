package dev.lucascosta.awslocalmanager.features.quick

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import dev.lucascosta.awslocalmanager.data.model.aws.ResourceRegistry
import dev.lucascosta.awslocalmanager.data.model.resources.DynamoDbResource
import dev.lucascosta.awslocalmanager.data.model.resources.ElastiCacheEngine
import dev.lucascosta.awslocalmanager.data.model.resources.ElastiCacheResource
import dev.lucascosta.awslocalmanager.data.model.resources.SqsResource
import dev.lucascosta.awslocalmanager.i18n.LocalStrings
import dev.lucascosta.awslocalmanager.theme.LocalAppColors
import org.koin.compose.koinInject

@Composable
fun QuickScreen(
    viewModel: QuickViewModel = koinInject(),
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    val strings = LocalStrings.current

    Row(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(strings.quickTitle, style = MaterialTheme.typography.titleMedium)
            Text(
                strings.quickSubtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HorizontalDivider()

            Text(
                strings.quickType,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ResourceRegistry.all().filter { it.isQuickCreatable }.forEach { type ->
                    FilterChip(
                        selected = state.selectedType == type,
                        onClick = { viewModel.setType(type) },
                        label = { Text(type.id, style = MaterialTheme.typography.labelSmall) },
                    )
                }
            }

            OutlinedTextField(
                value = state.resourceName,
                onValueChange = viewModel::setName,
                label = { Text(strings.quickName, style = MaterialTheme.typography.bodySmall) },
                placeholder = { Text(strings.quickNameHint, style = MaterialTheme.typography.bodySmall) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            when (state.selectedType) {
                SqsResource ->
                    SqsOptions(
                        createDlq = state.createDlq,
                        dlqMaxReceive = state.dlqMaxReceiveCount,
                        onCreateDlqChange = viewModel::setCreateDlq,
                        onMaxReceiveChange = viewModel::setDlqMaxReceiveCount,
                    )

                DynamoDbResource ->
                    DynamoOptions(
                        partitionKey = state.partitionKey,
                        keyType = state.partitionKeyType,
                        onKeyChange = viewModel::setPartitionKey,
                        onTypeChange = viewModel::setPartitionKeyType,
                    )

                ElastiCacheResource ->
                    ElastiCacheOptions(
                        engine = state.elastiCacheEngine,
                        onEngineChange = viewModel::setElastiCacheEngine,
                    )

                else -> {}
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = viewModel::create,
                enabled = state.resourceName.isNotBlank() && !state.isCreating,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.isCreating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(strings.quickCreating)
                } else {
                    Text(strings.quickCreate)
                }
            }
        }

        HorizontalDivider(modifier = Modifier.fillMaxHeight().width(1.dp))

        Column(
            modifier = Modifier.weight(1f).fillMaxHeight().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(strings.quickHistoryTitle, style = MaterialTheme.typography.titleSmall)
            HorizontalDivider()

            if (state.history.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(strings.quickHistoryEmpty, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                val listState = rememberLazyListState()
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize().padding(end = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(state.history, key = { it.id }) { item ->
                            HistoryRow(item = item)
                        }
                    }
                    VerticalScrollbar(
                        adapter = rememberScrollbarAdapter(listState),
                        modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                    )
                }
            }
        }
    }
}

@Composable
private fun SqsOptions(
    createDlq: Boolean,
    dlqMaxReceive: Int,
    onCreateDlqChange: (Boolean) -> Unit,
    onMaxReceiveChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Checkbox(checked = createDlq, onCheckedChange = onCreateDlqChange)
            Text(strings.quickDlqAuto, style = MaterialTheme.typography.bodyMedium)
        }
        if (createDlq) {
            OutlinedTextField(
                value = dlqMaxReceive.toString(),
                onValueChange = { it.toIntOrNull()?.let { v -> onMaxReceiveChange(v.coerceIn(1, 100)) } },
                label = { Text(strings.quickDlqMaxReceive, style = MaterialTheme.typography.bodySmall) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun DynamoOptions(
    partitionKey: String,
    keyType: DynamoKeyType,
    onKeyChange: (String) -> Unit,
    onTypeChange: (DynamoKeyType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = partitionKey,
                onValueChange = onKeyChange,
                label = { Text(strings.quickPartitionKey, style = MaterialTheme.typography.bodySmall) },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
        }
        Text(strings.quickPartitionKeyType, style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            DynamoKeyType.entries.forEach { type ->
                FilterChip(
                    selected = keyType == type,
                    onClick = { onTypeChange(type) },
                    label = { Text(type.name, style = MaterialTheme.typography.labelSmall) },
                )
            }
        }
    }
}

@Composable
private fun ElastiCacheOptions(
    engine: ElastiCacheEngine,
    onEngineChange: (ElastiCacheEngine) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(strings.quickElastiCacheEngine, style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ElastiCacheEngine.entries.forEach { e ->
                FilterChip(
                    selected = engine == e,
                    onClick = { onEngineChange(e) },
                    label = { Text(e.cliValue, style = MaterialTheme.typography.labelSmall) },
                )
            }
        }
    }
}

@Composable
private fun HistoryRow(
    item: QuickHistoryItem,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val appColors = LocalAppColors.current

    Row(
        modifier =
            modifier.fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = if (item.success) Icons.Outlined.CheckCircle else Icons.Outlined.Error,
            contentDescription = null,
            tint = if (item.success) appColors.success else MaterialTheme.colorScheme.error,
            modifier = Modifier.size(16.dp),
        )

        Text(
            item.timestamp,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Text(
            item.type.id,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )

        Text(item.name, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))

        Text(
            if (item.success) strings.quickSuccess else strings.quickError,
            style = MaterialTheme.typography.labelSmall,
            color = if (item.success) appColors.success else MaterialTheme.colorScheme.error,
        )
    }
}
