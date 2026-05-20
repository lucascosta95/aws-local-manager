package dev.lucascosta.awslocalmanager.data.model.resources

import dev.lucascosta.awslocalmanager.data.model.aws.AwsResourceDefinition
import dev.lucascosta.awslocalmanager.data.model.aws.RunningResource
import dev.lucascosta.awslocalmanager.data.model.aws.SuccessSnackbarKey
import dev.lucascosta.awslocalmanager.data.remote.AwsCommands
import dev.lucascosta.awslocalmanager.data.remote.EmulatorDefaults

object StepFunctionsResource : AwsResourceDefinition {
    override val id = "STEP_FUNCTIONS"
    override val displayName = "STEP_FUNCTIONS"
    override val terraformPrefix = "aws_sfn_state_machine"
    override val healthKey = "states"
    override val creationPriority = 3
    override val isQuickCreatable = false
    override val hasFilePublish = false
    override val publishableViaJson = true
    override val supportsPayloads = false
    override val successSnackbarKey = SuccessSnackbarKey.GENERIC

    override fun createCommand(name: String) = AwsCommands.createStateMachine(name)

    override fun deleteCommand(resource: RunningResource) =
        (resource.arn ?: buildArn(resource.name)).let { AwsCommands.deleteStepFunctions(it) }

    override fun buildArn(name: String) = EmulatorDefaults.stepFunctionsArn(name)

    override fun terraformTemplate(label: String) =
        """
        resource "aws_sfn_state_machine" "$label" {
          name     = "$label"
          role_arn = "${EmulatorDefaults.iamRoleArn("stepfunctions-role")}"
          definition = jsonencode({
            Comment = "$label",
            StartAt = "HelloWorld",
            States  = {
              HelloWorld = { Type = "Pass", End = true }
            }
          })
        }
        """.trimIndent()

    override fun publishIdentifier(resource: RunningResource) = resource.arn ?: resource.name
}
