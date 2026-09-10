package dev.lucascosta.awslocalmanager.data.model.resources

import dev.lucascosta.awslocalmanager.data.model.aws.AwsResourceDefinition
import dev.lucascosta.awslocalmanager.data.model.aws.RunningResource
import dev.lucascosta.awslocalmanager.data.model.aws.SuccessSnackbarKey
import dev.lucascosta.awslocalmanager.data.remote.EmulatorDefaults
import dev.lucascosta.awslocalmanager.data.remote.SsmCommands

object SsmParameterResource : AwsResourceDefinition {
    const val VALUE_PROPERTY = "value"
    const val TYPE_PROPERTY = "type"

    override val id = "SSM"
    override val displayName = "SSM Parameter Store"
    override val terraformPrefix = "aws_ssm_parameter"
    override val healthKey = "ssm"
    override val creationPriority = 0
    override val isQuickCreatable = true
    override val hasFilePublish = false
    override val publishableViaJson = false
    override val supportsPayloads = false
    override val successSnackbarKey = SuccessSnackbarKey.GENERIC

    override fun createCommand(
        name: String,
        extraProperties: Map<String, String>,
    ): List<String> {
        val value = extraProperties[VALUE_PROPERTY].orEmpty()
        val type = extraProperties[TYPE_PROPERTY] ?: SsmParameterType.STRING.cliValue
        return SsmCommands.putParameter(name, value, type)
    }

    override fun deleteCommand(resource: RunningResource) = SsmCommands.deleteParameter(resource.name)

    override fun buildArn(name: String) = EmulatorDefaults.ssmParameterArn(name)

    override fun terraformTemplate(label: String) =
        """
        resource "aws_ssm_parameter" "$label" {
          name  = "/${label.replace("_", "/")}"
          type  = "String"
          value = "change-me"
        }
        """.trimIndent()

    override fun publishIdentifier(resource: RunningResource) = resource.name
}
