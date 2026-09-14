package dev.lucascosta.awslocalmanager.data.model.inspector

data class SqsInspectorMessage(
    val messageId: String,
    val body: String,
    val attributes: Map<String, String>,
)

data class SfnInspectorExecution(
    val executionArn: String,
    val name: String,
    val status: String,
    val startDate: String,
    val stopDate: String?,
)

data class S3InspectorObject(
    val key: String,
    val displayName: String,
    val sizeBytes: Long,
    val lastModified: String,
    val isPrefix: Boolean,
)

data class SsmInspectorParameter(
    val name: String,
    val type: String,
    val value: String,
    val version: Long,
)

data class MskInspectorTopic(
    val name: String,
    val partitions: Int,
    val replicas: Int,
)

data class MskInspectorRecord(
    val partition: Int,
    val offset: Long,
    val timestamp: Long,
    val key: String,
    val value: String,
    val headers: Map<String, String>,
)

data class MskInspectorSchema(
    val subject: String,
    val version: Int,
    val id: Int,
    val type: String,
    val schema: String,
)

data class MskInspectorGroup(
    val name: String,
    val state: String,
    val members: Int,
    val totalLag: Long,
)

sealed class InspectorDetail {
    data class SqsDetail(
        val messages: List<SqsInspectorMessage>,
        val queueUrl: String,
    ) : InspectorDetail()

    data class StepFunctionsDetail(
        val executions: List<SfnInspectorExecution>,
        val statusCounts: Map<String, Int>,
        val selectedExecution: SfnInspectorExecution? = null,
        val executionInput: String? = null,
        val executionOutput: String? = null,
    ) : InspectorDetail()

    data class DynamoDetail(
        val items: List<Map<String, String>>,
        val columns: List<String>,
        val hasMore: Boolean = false,
    ) : InspectorDetail()

    data class S3Detail(
        val entries: List<S3InspectorObject>,
        val currentPrefix: String = "",
    ) : InspectorDetail()

    data class SsmDetail(
        val path: String,
        val parameters: List<SsmInspectorParameter>,
    ) : InspectorDetail()

    data class ElastiCacheDetail(
        val clusterId: String,
        val engine: String,
        val status: String,
        val numNodes: Int,
        val nodeType: String,
        val engineVersion: String,
        val endpoint: String?,
        val port: Int?,
        val cacheEntries: List<CacheEntry> = emptyList(),
        val hasMore: Boolean = false,
        val cursor: String = "0",
    ) : InspectorDetail()

    data class MskDetail(
        val clusterName: String,
        val clusterType: String,
        val state: String,
        val kafkaVersion: String?,
        val brokerNodes: Int?,
        val brokerContainer: String?,
        val hostAddress: String?,
        val schemaRegistryHostAddress: String?,
        val dockerNetwork: String,
        val topics: List<MskInspectorTopic> = emptyList(),
        val consumerGroups: List<MskInspectorGroup> = emptyList(),
        val schemas: List<MskInspectorSchema> = emptyList(),
        val selectedTopic: String? = null,
        val records: List<MskInspectorRecord> = emptyList(),
    ) : InspectorDetail()
}
