package dev.lucascosta.awslocalmanager.data.model.resources

import dev.lucascosta.awslocalmanager.data.model.aws.AwsResourceDefinition
import dev.lucascosta.awslocalmanager.data.model.aws.RunningResource
import dev.lucascosta.awslocalmanager.data.model.aws.SuccessSnackbarKey
import dev.lucascosta.awslocalmanager.data.remote.EmulatorDefaults
import dev.lucascosta.awslocalmanager.data.remote.GlueSchemaRegistryCommands

object GlueRegistryResource : AwsResourceDefinition {
    // AWS puts a schema created without a registry into this one, and the emulator does the same.
    const val DEFAULT_REGISTRY = "default-registry"

    override val id = "GLUE_REGISTRY"
    override val displayName = "Glue Registry"
    override val terraformPrefix = "aws_glue_registry"
    override val healthKey = "glue"
    override val creationPriority = 1
    override val isQuickCreatable = true
    override val hasFilePublish = false
    override val publishableViaJson = false
    override val supportsPayloads = false
    override val successSnackbarKey = SuccessSnackbarKey.GENERIC

    override fun createCommand(
        name: String,
        extraProperties: Map<String, String>,
    ): List<String> = GlueSchemaRegistryCommands.createRegistry(name)

    override fun deleteCommand(resource: RunningResource): List<String> = GlueSchemaRegistryCommands.deleteRegistry(resource.name)

    override fun buildArn(name: String) = "arn:aws:glue:${EmulatorDefaults.AWS_REGION}:${EmulatorDefaults.AWS_ACCOUNT_ID}:registry/$name"

    override fun terraformTemplate(label: String) =
        """
        resource "aws_glue_registry" "$label" {
          registry_name = "${label.replace("_", "-")}"
        }
        """.trimIndent()

    override fun publishIdentifier(resource: RunningResource) = resource.name
}
