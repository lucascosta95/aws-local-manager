package dev.lucascosta.awslocalmanager.data.model.aws

data class PublishResult(
    val success: Boolean,
    val messageId: String? = null,
    val error: String? = null,
)
