package dev.lucascosta.awslocalmanager.data.model.aws

data class RunningResource(
    val name: String,
    val type: AwsResourceDefinition,
    val arn: String?,
    val url: String?,
    val projectName: String?,
)

fun RunningResource.resourceId() = "${type.id}:$name"
