package dev.lucascosta.awslocalmanager.data.model.resources

import dev.lucascosta.awslocalmanager.data.model.aws.AwsResourceDefinition
import dev.lucascosta.awslocalmanager.data.model.aws.RunningResource
import dev.lucascosta.awslocalmanager.data.model.aws.SuccessSnackbarKey

object SnsSubscriptionResource : AwsResourceDefinition {
    override val id = "SNS_SUBSCRIPTION"
    override val displayName = "SNS_SUBSCRIPTION"
    override val terraformPrefix = "aws_sns_topic_subscription"
    override val healthKey = "sns"
    override val creationPriority = Int.MAX_VALUE
    override val isCheckable = false
    override val isQuickCreatable = false
    override val hasFilePublish = false
    override val publishableViaJson = false
    override val supportsPayloads = false
    override val successSnackbarKey = SuccessSnackbarKey.GENERIC

    override fun createCommand(name: String): List<String>? = null

    override fun deleteCommand(resource: RunningResource): List<String>? = null

    override fun buildArn(name: String): String? = null

    override fun terraformTemplate(label: String) = ""

    override fun publishIdentifier(resource: RunningResource) = resource.name
}
