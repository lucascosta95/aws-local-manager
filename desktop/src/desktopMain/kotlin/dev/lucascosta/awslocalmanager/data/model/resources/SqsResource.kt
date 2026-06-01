package dev.lucascosta.awslocalmanager.data.model.resources

import dev.lucascosta.awslocalmanager.data.model.aws.AwsResourceDefinition
import dev.lucascosta.awslocalmanager.data.model.aws.RunningResource
import dev.lucascosta.awslocalmanager.data.model.aws.SuccessSnackbarKey
import dev.lucascosta.awslocalmanager.data.remote.AwsCommands
import dev.lucascosta.awslocalmanager.data.remote.EmulatorDefaults

object SqsResource : AwsResourceDefinition {
    override val id = "SQS"
    override val displayName = "SQS"
    override val terraformPrefix = "aws_sqs_queue"
    override val healthKey = "sqs"
    override val creationPriority = 3
    override val isQuickCreatable = true
    override val hasFilePublish = false
    override val publishableViaJson = true
    override val supportsPayloads = true
    override val successSnackbarKey = SuccessSnackbarKey.GENERIC

    override fun createCommand(
        name: String,
        extraProperties: Map<String, String>,
    ) = AwsCommands.createSqs(name)

    override fun deleteCommand(resource: RunningResource) = resource.url?.let { AwsCommands.deleteSqs(it) }

    override fun buildArn(name: String) = EmulatorDefaults.sqsArn(name)

    override fun terraformTemplate(label: String) =
        """
        resource "aws_sqs_queue" "$label" {
          name = "$label"
        }
        """.trimIndent()

    override fun publishIdentifier(resource: RunningResource) = resource.url ?: resource.name
}
