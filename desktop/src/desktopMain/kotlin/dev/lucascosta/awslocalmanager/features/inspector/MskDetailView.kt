package dev.lucascosta.awslocalmanager.features.inspector

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.lucascosta.awslocalmanager.components.CopyButton
import dev.lucascosta.awslocalmanager.components.ResizableTable
import dev.lucascosta.awslocalmanager.components.TableColumn
import dev.lucascosta.awslocalmanager.constants.AppConstants.MSK_BROKER_PORT
import dev.lucascosta.awslocalmanager.constants.AppConstants.SCHEMA_REGISTRY_PORT
import dev.lucascosta.awslocalmanager.data.model.inspector.InspectorDetail
import dev.lucascosta.awslocalmanager.data.model.inspector.MskInspectorRecord
import dev.lucascosta.awslocalmanager.i18n.LocalInspectorStrings
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val recordTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault())

@Composable
internal fun MskDetailView(
    detail: InspectorDetail.MskDetail,
    isLoadingSubDetail: Boolean,
    onSelectTopic: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(end = 12.dp).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            MskClusterSummary(detail)
            HorizontalDivider()
            if (detail.brokerContainer == null) {
                MutedText(LocalInspectorStrings.current.inspectorMskNotActive)
            } else {
                MskTopicsSection(detail, onSelectTopic)
                MskConsumerGroupsSection(detail)
                MskSchemasSection(detail)
                HorizontalDivider()
                MskRecordsSection(detail, isLoadingSubDetail)
            }
        }
        VerticalScrollbar(
            adapter = rememberScrollbarAdapter(scrollState),
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(end = 2.dp),
        )
    }
}

@Composable
private fun MskClusterSummary(detail: InspectorDetail.MskDetail) {
    val strings = LocalInspectorStrings.current
    val copyableLabels =
        setOfNotNull(
            strings.inspectorMskBrokerAddress,
            strings.inspectorMskSchemaRegistryNetworkAddress,
            strings.inspectorMskHostAddress.takeIf { detail.hostAddress != null },
            strings.inspectorMskSchemaRegistryHostAddress.takeIf { detail.schemaRegistryHostAddress != null },
        )
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        listOfNotNull(
            strings.inspectorMskType to detail.clusterType.ifBlank { null },
            strings.inspectorMskState to detail.state.ifBlank { null },
            strings.inspectorMskKafkaVersion to detail.kafkaVersion,
            strings.inspectorMskBrokers to detail.brokerNodes?.toString(),
            strings.inspectorMskHostAddress to detail.brokerContainer?.let { detail.hostAddress ?: strings.inspectorMskHostAddressPending },
            strings.inspectorMskBrokerAddress to detail.brokerContainer?.let { "$it:$MSK_BROKER_PORT" },
            strings.inspectorMskSchemaRegistryHostAddress to
                detail.brokerContainer?.let { detail.schemaRegistryHostAddress ?: strings.inspectorMskHostAddressPending },
            strings.inspectorMskSchemaRegistryNetworkAddress to detail.brokerContainer?.let { "http://$it:$SCHEMA_REGISTRY_PORT" },
            strings.inspectorMskDockerNetwork to detail.dockerNetwork,
        ).forEach { (label, value) ->
            if (value != null) {
                SummaryRow(label = label, value = value, copyable = label in copyableLabels)
            }
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
    copyable: Boolean,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.widthIn(min = 210.dp),
        )
        Text(value, style = MaterialTheme.typography.bodySmall)
        if (copyable) {
            CopyButton(textToCopy = value, contentDescription = label)
        }
    }
}

@Composable
private fun MskTopicsSection(
    detail: InspectorDetail.MskDetail,
    onSelectTopic: (String) -> Unit,
) {
    val strings = LocalInspectorStrings.current
    SectionTitle(strings.inspectorMskTopics)
    ResizableTable(
        columns =
            listOf(
                TableColumn(strings.inspectorMskTopicColumn, 0.60f),
                TableColumn(strings.inspectorMskPartitionsColumn, 0.20f),
                TableColumn(strings.inspectorMskReplicasColumn, 0.20f),
            ),
        rows = detail.topics.map { listOf(it.name, it.partitions.toString(), it.replicas.toString()) },
        onRowClick = { index -> onSelectTopic(detail.topics[index].name) },
        selectedRowIndex = detail.topics.indexOfFirst { it.name == detail.selectedTopic }.takeIf { it >= 0 },
        emptyMessage = strings.inspectorMskNoTopics,
        modifier = Modifier.fillMaxWidth().height(tableHeight(detail.topics.size)),
    )
}

@Composable
private fun MskConsumerGroupsSection(detail: InspectorDetail.MskDetail) {
    val strings = LocalInspectorStrings.current
    SectionTitle(strings.inspectorMskConsumerGroups)
    ResizableTable(
        columns =
            listOf(
                TableColumn(strings.inspectorMskGroupColumn, 0.40f),
                TableColumn(strings.inspectorMskState, 0.20f),
                TableColumn(strings.inspectorMskMembersColumn, 0.20f),
                TableColumn(strings.inspectorMskLagColumn, 0.20f),
            ),
        rows = detail.consumerGroups.map { listOf(it.name, it.state, it.members.toString(), it.totalLag.toString()) },
        emptyMessage = strings.inspectorMskNoGroups,
        modifier = Modifier.fillMaxWidth().height(tableHeight(detail.consumerGroups.size)),
    )
}

@Composable
private fun MskSchemasSection(detail: InspectorDetail.MskDetail) {
    val strings = LocalInspectorStrings.current
    SectionTitle(strings.inspectorMskSchemas)
    ResizableTable(
        columns =
            listOf(
                TableColumn(strings.inspectorMskSubjectColumn, 0.50f),
                TableColumn(strings.inspectorMskVersionColumn, 0.15f),
                TableColumn(strings.inspectorMskSchemaIdColumn, 0.15f),
                TableColumn(strings.inspectorMskSchemaTypeColumn, 0.20f),
            ),
        rows = detail.schemas.map { listOf(it.subject, it.version.toString(), it.id.toString(), it.type) },
        emptyMessage = strings.inspectorMskNoSchemas,
        onRowCopy = { index -> detail.schemas[index].schema },
        modifier = Modifier.fillMaxWidth().height(tableHeight(detail.schemas.size)),
    )
}

@Composable
private fun MskRecordsSection(
    detail: InspectorDetail.MskDetail,
    isLoadingSubDetail: Boolean,
) {
    val strings = LocalInspectorStrings.current
    val selectedTopic = detail.selectedTopic
    when {
        isLoadingSubDetail ->
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }

        selectedTopic == null -> MutedText(strings.inspectorMskSelectTopic)

        else -> {
            SectionTitle(strings.inspectorMskMessagesTitle.replace("{topic}", selectedTopic))
            ResizableTable(
                columns =
                    listOf(
                        TableColumn(strings.inspectorMskPartitionColumn, 0.12f),
                        TableColumn(strings.inspectorMskOffsetColumn, 0.10f),
                        TableColumn(strings.inspectorMskTimestampColumn, 0.14f),
                        TableColumn(strings.inspectorMskKeyColumn, 0.14f),
                        TableColumn(strings.inspectorMskValueColumn, 0.36f),
                        TableColumn(strings.inspectorMskHeadersColumn, 0.14f),
                    ),
                rows = detail.records.map { it.toRow() },
                emptyMessage = strings.inspectorMskNoMessages,
                onRowCopy = { index -> detail.records[index].toJson() },
                modifier = Modifier.fillMaxWidth().height(tableHeight(detail.records.size, maxHeight = 480.dp)),
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge)
}

@Composable
private fun MutedText(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

private fun tableHeight(
    rowCount: Int,
    maxHeight: Dp = 240.dp,
): Dp = ((rowCount * 32) + 40).dp.coerceIn(120.dp, maxHeight)

private fun MskInspectorRecord.toRow(): List<String> =
    listOf(
        partition.toString(),
        offset.toString(),
        recordTimeFormatter.format(Instant.ofEpochMilli(timestamp)),
        key,
        value,
        headers.entries.joinToString(", ") { (name, headerValue) -> "$name=$headerValue" },
    )

private fun MskInspectorRecord.toJson(): String =
    buildJsonObject {
        put("partition", partition)
        put("offset", offset)
        put("timestamp", timestamp)
        put("key", key)
        put("value", value)
        put("headers", buildJsonObject { headers.forEach { (name, headerValue) -> put(name, JsonPrimitive(headerValue)) } })
    }.toString()
