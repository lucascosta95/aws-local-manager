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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.lucascosta.awslocalmanager.components.CopyButton
import dev.lucascosta.awslocalmanager.components.ResizableTable
import dev.lucascosta.awslocalmanager.components.TableColumn
import dev.lucascosta.awslocalmanager.data.model.inspector.InspectorDetail
import dev.lucascosta.awslocalmanager.features.inspector.handler.GlueSchemaRegistryInspectorHandler
import dev.lucascosta.awslocalmanager.i18n.LocalInspectorStrings
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

private val prettySchemaJson = Json { prettyPrint = true }

@Composable
internal fun GlueSchemaRegistryDetailView(
    detail: InspectorDetail.GlueSchemaRegistryDetail,
    isLoadingSubDetail: Boolean,
    onSelectItem: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalInspectorStrings.current
    val scrollState = rememberScrollState()
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(end = 12.dp).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(strings.inspectorGlueSchemas, style = MaterialTheme.typography.labelLarge)
            ResizableTable(
                columns =
                    listOf(
                        TableColumn(strings.inspectorGlueSchemaColumn, 0.40f),
                        TableColumn(strings.inspectorGlueFormatColumn, 0.18f),
                        TableColumn(strings.inspectorGlueCompatibilityColumn, 0.24f),
                        TableColumn(strings.inspectorGlueLatestVersionColumn, 0.18f),
                    ),
                rows = detail.schemas.map { listOf(it.name, it.dataFormat, it.compatibility, it.latestVersion.toString()) },
                onRowClick = { index -> onSelectItem(detail.schemas[index].name) },
                selectedRowIndex = detail.schemas.indexOfFirst { it.name == detail.selectedSchema }.takeIf { it >= 0 },
                emptyMessage = strings.inspectorGlueNoSchemas,
                modifier = Modifier.fillMaxWidth().height(glueTableHeight(detail.schemas.size)),
            )
            HorizontalDivider()
            GlueSchemaVersionsSection(detail, isLoadingSubDetail, onSelectItem)
        }
        VerticalScrollbar(
            adapter = rememberScrollbarAdapter(scrollState),
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(end = 2.dp),
        )
    }
}

@Composable
private fun GlueSchemaVersionsSection(
    detail: InspectorDetail.GlueSchemaRegistryDetail,
    isLoadingSubDetail: Boolean,
    onSelectItem: (String) -> Unit,
) {
    val strings = LocalInspectorStrings.current
    val schema = detail.selectedSchema
    when {
        isLoadingSubDetail ->
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }

        schema == null ->
            Text(
                strings.inspectorGlueSelectSchema,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

        else -> {
            Text(strings.inspectorGlueVersionsTitle.replace("{schema}", schema), style = MaterialTheme.typography.labelLarge)
            ResizableTable(
                columns =
                    listOf(
                        TableColumn(strings.inspectorGlueVersionColumn, 0.20f),
                        TableColumn(strings.inspectorGlueStatusColumn, 0.30f),
                        TableColumn(strings.inspectorGlueCreatedColumn, 0.50f),
                    ),
                rows = detail.versions.map { listOf(it.versionNumber.toString(), it.status, it.createdTime.take(19).replace("T", " ")) },
                onRowClick = { index ->
                    onSelectItem(GlueSchemaRegistryInspectorHandler.versionItemId(schema, detail.versions[index].versionNumber))
                },
                selectedRowIndex = detail.versions.indexOfFirst { it.versionNumber == detail.selectedVersion }.takeIf { it >= 0 },
                modifier = Modifier.fillMaxWidth().height(glueTableHeight(detail.versions.size)),
            )
            detail.definition?.let { definition -> SchemaDefinitionBlock(definition) }
        }
    }
}

@Composable
private fun SchemaDefinitionBlock(definition: String) {
    val strings = LocalInspectorStrings.current
    val formatted = formatSchemaDefinition(definition)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                strings.inspectorGlueDefinition,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            CopyButton(textToCopy = formatted, contentDescription = strings.inspectorGlueDefinition)
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ) {
            Text(
                formatted,
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            )
        }
    }
}

private fun formatSchemaDefinition(definition: String): String =
    runCatching { prettySchemaJson.encodeToString(JsonElement.serializer(), Json.parseToJsonElement(definition)) }
        .getOrDefault(definition)

private fun glueTableHeight(rowCount: Int): Dp = ((rowCount * 32) + 40).dp.coerceIn(120.dp, 240.dp)
