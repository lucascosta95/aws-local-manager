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
}
