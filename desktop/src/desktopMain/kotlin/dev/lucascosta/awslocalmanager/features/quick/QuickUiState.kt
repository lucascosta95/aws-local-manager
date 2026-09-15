package dev.lucascosta.awslocalmanager.features.quick

import dev.lucascosta.awslocalmanager.constants.AppConstants.EMPTY_STRING
import dev.lucascosta.awslocalmanager.data.model.aws.AwsResourceDefinition
import dev.lucascosta.awslocalmanager.data.model.resources.ElastiCacheEngine
import dev.lucascosta.awslocalmanager.data.model.resources.GlueSchemaDataFormat
import dev.lucascosta.awslocalmanager.data.model.resources.GlueSchemaResource
import dev.lucascosta.awslocalmanager.data.model.resources.MskTopicResource
import dev.lucascosta.awslocalmanager.data.model.resources.SqsResource
import dev.lucascosta.awslocalmanager.data.model.resources.SsmParameterResource
import dev.lucascosta.awslocalmanager.data.model.resources.SsmParameterType

data class QuickUiState(
    val selectedType: AwsResourceDefinition = SqsResource,
    val resourceName: String = EMPTY_STRING,
    val createDlq: Boolean = true,
    val dlqMaxReceiveCount: Int = 5,
    val partitionKey: String = "id",
    val partitionKeyType: DynamoKeyType = DynamoKeyType.STRING,
    val elastiCacheEngine: ElastiCacheEngine = ElastiCacheEngine.REDIS,
    val parameterValue: String = EMPTY_STRING,
    val parameterType: SsmParameterType = SsmParameterType.STRING,
    val mskClusters: List<String> = emptyList(),
    val selectedMskCluster: String? = null,
    val topicPartitions: Int = MskTopicResource.DEFAULT_PARTITIONS,
    val glueRegistries: List<String> = emptyList(),
    val selectedGlueRegistry: String? = null,
    val glueDataFormat: GlueSchemaDataFormat = GlueSchemaDataFormat.AVRO,
    val glueCompatibility: String = GlueSchemaResource.DEFAULT_COMPATIBILITY,
    val glueSchemaDefinition: String = EMPTY_STRING,
    val pendingChild: PendingChildResource? = null,
    val isCreating: Boolean = false,
    val history: List<QuickHistoryItem> = emptyList(),
) {
    val canCreate: Boolean
        get() =
            resourceName.isNotBlank() &&
                !isCreating &&
                (selectedType != SsmParameterResource || parameterValue.isNotBlank()) &&
                (selectedType != MskTopicResource || selectedMskCluster != null) &&
                (selectedType != GlueSchemaResource || (selectedGlueRegistry != null && glueSchemaDefinition.isNotBlank()))
}

// A topic or schema whose creation was interrupted to create its parent first; the form returns to it afterwards.
data class PendingChildResource(
    val type: AwsResourceDefinition,
    val name: String,
)
