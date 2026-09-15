package dev.lucascosta.awslocalmanager.features.inspector.handler

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schema
import androidx.compose.ui.graphics.vector.ImageVector
import dev.lucascosta.awslocalmanager.data.model.inspector.GlueInspectorSchema
import dev.lucascosta.awslocalmanager.data.model.inspector.GlueInspectorSchemaVersion
import dev.lucascosta.awslocalmanager.data.model.inspector.InspectorDetail
import dev.lucascosta.awslocalmanager.data.model.inspector.InspectorResource
import dev.lucascosta.awslocalmanager.data.remote.AwsGlueSchemaRegistryClient

class GlueSchemaRegistryInspectorHandler(
    private val clientFactory: (String) -> AwsGlueSchemaRegistryClient = ::AwsGlueSchemaRegistryClient,
) : InspectorServiceHandler {
    companion object {
        const val SUMMARY_TYPE = "glue-registry"
        private const val VERSION_SEPARATOR = "#"

        fun versionItemId(
            schema: String,
            versionNumber: Long,
        ) = "$schema$VERSION_SEPARATOR$versionNumber"
    }

    override val serviceKey: String = "glue"
    override val displayName: String = "Glue Schema Registry"
    override val icon: ImageVector = Icons.Outlined.Schema

    override suspend fun loadResources(endpoint: String): List<InspectorResource> {
        val client = clientFactory(endpoint)
        return client.listRegistries().getOrElse { return emptyList() }
            .map { registry ->
                val schemaCount = client.listSchemaNames(registry).getOrNull()?.size?.toLong()
                InspectorResource(id = registry, name = registry, summaryType = SUMMARY_TYPE, summaryCount = schemaCount)
            }
            .sortedBy { it.name }
    }

    override suspend fun loadDetail(
        endpoint: String,
        resource: InspectorResource,
    ): InspectorDetail {
        val client = clientFactory(endpoint)
        val schemas =
            client.listSchemaNames(resource.id).getOrThrow().map { name ->
                val info = client.getSchema(resource.id, name).getOrThrow()
                GlueInspectorSchema(info.name, info.dataFormat, info.compatibility, info.latestVersion)
            }
        return InspectorDetail.GlueSchemaRegistryDetail(registry = resource.id, schemas = schemas.sortedBy { it.name })
    }

    override suspend fun loadSubDetail(
        endpoint: String,
        resource: InspectorResource,
        currentDetail: InspectorDetail,
        subItemId: String,
    ): InspectorDetail? {
        val detail = currentDetail as? InspectorDetail.GlueSchemaRegistryDetail ?: return null
        val client = clientFactory(endpoint)
        val schema = subItemId.substringBefore(VERSION_SEPARATOR)
        val versions =
            if (schema == detail.selectedSchema && detail.versions.isNotEmpty()) {
                detail.versions
            } else {
                client.listSchemaVersions(detail.registry, schema).getOrThrow().map {
                    GlueInspectorSchemaVersion(it.versionNumber, it.status, it.createdTime)
                }
            }
        val versionNumber = subItemId.substringAfter(VERSION_SEPARATOR, "").toLongOrNull() ?: versions.firstOrNull()?.versionNumber
        val definition = versionNumber?.let { client.getSchemaDefinition(detail.registry, schema, it).getOrThrow() }
        return detail.copy(selectedSchema = schema, versions = versions, selectedVersion = versionNumber, definition = definition)
    }
}
