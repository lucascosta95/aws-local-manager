package dev.lucascosta.awslocalmanager.data.model.resources

import dev.lucascosta.awslocalmanager.data.model.aws.AwsResourceDefinition
import dev.lucascosta.awslocalmanager.data.model.aws.RunningResource
import dev.lucascosta.awslocalmanager.data.model.aws.SuccessSnackbarKey
import dev.lucascosta.awslocalmanager.data.remote.AwsCommands

object DynamoDbResource : AwsResourceDefinition {
    override val id = "DYNAMODB"
    override val displayName = "DYNAMODB"
    override val terraformPrefix = "aws_dynamodb_table"
    override val healthKey = "dynamodb"
    override val creationPriority = 2
    override val isQuickCreatable = true
    override val hasFilePublish = false
    override val publishableViaJson = true
    override val supportsPayloads = false
    override val successSnackbarKey = SuccessSnackbarKey.DYNAMODB

    override fun createCommand(name: String) = AwsCommands.createDynamoDb(name)

    override fun deleteCommand(resource: RunningResource) = AwsCommands.deleteDynamoDb(resource.name)

    override fun buildArn(name: String): String? = null

    override fun terraformTemplate(label: String) =
        """
        resource "aws_dynamodb_table" "$label" {
          name           = "$label"
          billing_mode   = "PAY_PER_REQUEST"
          hash_key       = "id"

          attribute {
            name = "id"
            type = "S"
          }
        }
        """.trimIndent()

    override fun publishIdentifier(resource: RunningResource) = resource.name
}
