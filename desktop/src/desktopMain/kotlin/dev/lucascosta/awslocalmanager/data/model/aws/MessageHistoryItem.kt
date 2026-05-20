package dev.lucascosta.awslocalmanager.data.model.aws

data class MessageHistoryItem(
    val id: String,
    val timestamp: Long = System.currentTimeMillis(),
    val type: AwsResourceDefinition,
    val resourceName: String,
    val jsonBody: String,
    val result: PublishResult,
    val isExpanded: Boolean = false,
)
