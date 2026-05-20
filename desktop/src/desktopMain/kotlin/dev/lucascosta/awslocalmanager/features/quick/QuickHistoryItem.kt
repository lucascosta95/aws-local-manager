package dev.lucascosta.awslocalmanager.features.quick

import dev.lucascosta.awslocalmanager.data.model.aws.AwsResourceDefinition
import java.util.UUID

data class QuickHistoryItem(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: String,
    val type: AwsResourceDefinition,
    val name: String,
    val success: Boolean,
)
