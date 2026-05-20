package dev.lucascosta.awslocalmanager.features.running

import dev.lucascosta.awslocalmanager.data.model.aws.AwsResourceDefinition
import dev.lucascosta.awslocalmanager.data.model.aws.PublishResult

internal data class PublishPayload(
    val result: Result<PublishResult>,
    val bodySummary: String,
    val resourceName: String,
    val messageType: AwsResourceDefinition,
)
