package dev.lucascosta.awslocalmanager.data.model.aws

data class SnsSubscription(
    val tfLabel: String,
    val topicRef: String,
    val protocol: String,
    val endpointRef: String,
    val rawMessageDelivery: Boolean,
    val filterPolicy: String?,
    val filterPolicyScope: String?,
)
