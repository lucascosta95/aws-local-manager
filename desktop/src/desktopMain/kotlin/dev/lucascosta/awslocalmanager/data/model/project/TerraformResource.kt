package dev.lucascosta.awslocalmanager.data.model.project

import dev.lucascosta.awslocalmanager.data.model.aws.AwsResourceDefinition

data class TerraformResource(
    val tfLabel: String,
    val awsName: String,
    val resourceType: AwsResourceDefinition?,
    val rawAwsType: String,
    val filePath: String,
    val extraProperties: Map<String, String> = emptyMap(),
) {
    val isSupported: Boolean get() = resourceType?.isSupported == true

    /**
     * Unique within a project. Terraform scopes a label to its resource type, so the same project
     * can hold `aws_sqs_queue "orders"` and `aws_sns_topic "orders"`, and [tfLabel] alone is not
     * enough to tell a list row from another.
     */
    val id: String get() = "$rawAwsType.$tfLabel"
}
