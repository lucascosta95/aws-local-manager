package dev.lucascosta.awslocalmanager.data.model.resources

import dev.lucascosta.awslocalmanager.data.model.aws.AwsResourceDefinition
import dev.lucascosta.awslocalmanager.data.model.aws.RunningResource
import dev.lucascosta.awslocalmanager.data.model.aws.SuccessSnackbarKey
import dev.lucascosta.awslocalmanager.data.remote.GlueSchemaDefinition
import dev.lucascosta.awslocalmanager.data.remote.GlueSchemaRegistryCommands

object GlueSchemaResource : AwsResourceDefinition {
    const val REGISTRY_PROPERTY = "registry"
    const val REGISTRY_REFERENCE_PROPERTY = "registry_arn"
    const val DATA_FORMAT_PROPERTY = "data_format"
    const val COMPATIBILITY_PROPERTY = "compatibility"
    const val DEFINITION_PROPERTY = "schema_definition"
    const val DEFAULT_COMPATIBILITY = "BACKWARD"
    val COMPATIBILITY_MODES = listOf("NONE", "DISABLED", "BACKWARD", "BACKWARD_ALL", "FORWARD", "FORWARD_ALL", "FULL", "FULL_ALL")
    private const val NAME_SEPARATOR = "/"

    override val id = "GLUE_SCHEMA"
    override val displayName = "Glue Schema"
    override val terraformPrefix = "aws_glue_schema"
    override val healthKey = "glue"
    override val creationPriority = 2
    override val isQuickCreatable = true
    override val hasFilePublish = false
    override val publishableViaJson = false
    override val supportsPayloads = false
    override val successSnackbarKey = SuccessSnackbarKey.GENERIC

    fun qualifiedName(
        registry: String,
        schema: String,
    ) = "$registry$NAME_SEPARATOR$schema"

    fun registryOf(qualifiedName: String) = qualifiedName.substringBefore(NAME_SEPARATOR)

    fun schemaOf(qualifiedName: String) = qualifiedName.substringAfter(NAME_SEPARATOR)

    fun definitionFrom(
        qualifiedName: String,
        extraProperties: Map<String, String>,
    ) = GlueSchemaDefinition(
        registry = registryOf(qualifiedName),
        schema = schemaOf(qualifiedName),
        dataFormat = extraProperties[DATA_FORMAT_PROPERTY] ?: GlueSchemaDataFormat.AVRO.name,
        compatibility = extraProperties[COMPATIBILITY_PROPERTY] ?: DEFAULT_COMPATIBILITY,
        definition = extraProperties[DEFINITION_PROPERTY].orEmpty(),
    )

    override fun createCommand(
        name: String,
        extraProperties: Map<String, String>,
    ): List<String>? = null

    override fun deleteCommand(resource: RunningResource): List<String> =
        GlueSchemaRegistryCommands.deleteSchema(registryOf(resource.name), schemaOf(resource.name))

    override fun buildArn(name: String): String? = null

    override fun terraformTemplate(label: String): String {
        val registryLabel = "${label}_registry"
        val schemaTemplate =
            """
            resource "aws_glue_schema" "$label" {
              schema_name       = "${label.replace("_", "-")}"
              registry_arn      = aws_glue_registry.$registryLabel.arn
              data_format       = "AVRO"
              compatibility     = "BACKWARD"
              schema_definition = <<EOF
            {
              "type": "record",
              "name": "Event",
              "fields": [
                { "name": "id", "type": "string" }
              ]
            }
            EOF
            }
            """.trimIndent()
        return GlueRegistryResource.terraformTemplate(registryLabel) + "\n\n" + schemaTemplate
    }

    override fun publishIdentifier(resource: RunningResource) = resource.name
}
