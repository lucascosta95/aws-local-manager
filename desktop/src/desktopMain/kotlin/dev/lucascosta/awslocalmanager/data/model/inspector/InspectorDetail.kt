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
}
