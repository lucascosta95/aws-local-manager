package dev.lucascosta.awslocalmanager.data.model.resources

import dev.lucascosta.awslocalmanager.data.model.aws.AwsResourceDefinition
import dev.lucascosta.awslocalmanager.data.model.aws.RunningResource
import dev.lucascosta.awslocalmanager.data.model.aws.SuccessSnackbarKey
import dev.lucascosta.awslocalmanager.data.remote.AwsCommands
import dev.lucascosta.awslocalmanager.data.remote.EmulatorDefaults

object SnsResource : AwsResourceDefinition {
    override val id = "SNS"
    override val displayName = "SNS"
    override val terraformPrefix = "aws_sns_topic"
    override val healthKey = "sns"
    override val creationPriority = 0
    override val isQuickCreatable = true
    override val hasFilePublish = false
    override val publishableViaJson = true
    override val supportsPayloads = true
    override val successSnackbarKey = SuccessSnackbarKey.GENERIC

    override fun createCommand(name: String) = AwsCommands.createSns(name)

    override fun deleteCommand(resource: RunningResource) = resource.arn?.let { AwsCommands.deleteSns(it) }

    override fun buildArn(name: String) = EmulatorDefaults.snsArn(name)

    override fun terraformTemplate(label: String) =
        """
        resource "aws_sns_topic" "$label" {
          name = "$label"
        }
        """.trimIndent()

    override fun publishIdentifier(resource: RunningResource) = resource.arn ?: resource.name
}
