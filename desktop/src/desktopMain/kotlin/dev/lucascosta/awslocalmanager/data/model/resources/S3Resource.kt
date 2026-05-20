package dev.lucascosta.awslocalmanager.data.model.resources

import dev.lucascosta.awslocalmanager.data.model.aws.AwsResourceDefinition
import dev.lucascosta.awslocalmanager.data.model.aws.RunningResource
import dev.lucascosta.awslocalmanager.data.model.aws.SuccessSnackbarKey
import dev.lucascosta.awslocalmanager.data.remote.AwsCommands

object S3Resource : AwsResourceDefinition {
    override val id = "S3"
    override val displayName = "S3"
    override val terraformPrefix = "aws_s3_bucket"
    override val healthKey = "s3"
    override val creationPriority = 1
    override val isQuickCreatable = true
    override val hasFilePublish = true
    override val publishableViaJson = false
    override val supportsPayloads = false
    override val successSnackbarKey = SuccessSnackbarKey.S3

    override fun createCommand(name: String) = AwsCommands.createS3(name)

    override fun deleteCommand(resource: RunningResource) = AwsCommands.deleteS3(resource.name)

    override fun buildArn(name: String): String? = null

    override fun terraformTemplate(label: String) =
        """
        resource "aws_s3_bucket" "$label" {
          bucket = "$label"
        }
        """.trimIndent()

    override fun publishIdentifier(resource: RunningResource) = resource.name
}
