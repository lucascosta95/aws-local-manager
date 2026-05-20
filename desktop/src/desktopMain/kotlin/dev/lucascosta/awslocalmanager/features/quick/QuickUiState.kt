package dev.lucascosta.awslocalmanager.features.quick

import dev.lucascosta.awslocalmanager.constants.AppConstants.EMPTY_STRING
import dev.lucascosta.awslocalmanager.data.model.aws.AwsResourceDefinition
import dev.lucascosta.awslocalmanager.data.model.resources.SqsResource

data class QuickUiState(
    val selectedType: AwsResourceDefinition = SqsResource,
    val resourceName: String = EMPTY_STRING,
    val createDlq: Boolean = true,
    val dlqMaxReceiveCount: Int = 5,
    val partitionKey: String = "id",
    val partitionKeyType: DynamoKeyType = DynamoKeyType.STRING,
    val isCreating: Boolean = false,
    val history: List<QuickHistoryItem> = emptyList(),
)
