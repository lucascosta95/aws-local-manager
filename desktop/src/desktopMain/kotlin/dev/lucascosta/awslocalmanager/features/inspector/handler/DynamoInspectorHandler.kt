package dev.lucascosta.awslocalmanager.features.inspector.handler

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.ui.graphics.vector.ImageVector
import aws.sdk.kotlin.services.dynamodb.DynamoDbClient
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.sdk.kotlin.services.dynamodb.model.DescribeTableRequest
import aws.sdk.kotlin.services.dynamodb.model.ListTablesRequest
import aws.sdk.kotlin.services.dynamodb.model.ScanRequest
import aws.smithy.kotlin.runtime.net.url.Url
import dev.lucascosta.awslocalmanager.data.model.inspector.InspectorDetail
import dev.lucascosta.awslocalmanager.data.model.inspector.InspectorResource
import dev.lucascosta.awslocalmanager.data.remote.EmulatorConfig
import dev.lucascosta.awslocalmanager.data.remote.EmulatorDefaults

class DynamoInspectorHandler : InspectorServiceHandler {
    override val serviceKey: String = "dynamodb"
    override val displayName: String = "DynamoDB"
    override val icon: ImageVector = Icons.Outlined.TableChart

    private val pageTokens = mutableMapOf<String, Map<String, AttributeValue>?>()

    private fun buildClient(endpoint: String) =
        DynamoDbClient {
            region = EmulatorDefaults.AWS_REGION
            endpointUrl = Url.parse(endpoint)
            credentialsProvider = EmulatorConfig.credentialsProvider
        }

    override suspend fun loadResources(endpoint: String): List<InspectorResource> =
        buildClient(endpoint).use { client ->
            val tables = client.listTables(ListTablesRequest {}).tableNames ?: emptyList()
            tables.map { tableName ->
                val itemCount =
                    runCatching {
                        client.describeTable(DescribeTableRequest { this.tableName = tableName })
                            .table?.itemCount ?: 0L
                    }.getOrElse { 0L }
                InspectorResource(
                    id = tableName,
                    name = tableName,
                    summaryType = "dynamo",
                    summaryCount = itemCount,
                )
            }
        }

    override suspend fun loadDetail(
        endpoint: String,
        resource: InspectorResource,
    ): InspectorDetail {
        pageTokens.remove(resource.id)
        return scanPage(endpoint, resource, exclusiveStartKey = null, existingItems = emptyList())
    }

    override suspend fun loadMore(
        endpoint: String,
        resource: InspectorResource,
        currentDetail: InspectorDetail,
    ): InspectorDetail? {
        val dynamoDetail = currentDetail as? InspectorDetail.DynamoDetail ?: return null
        if (!dynamoDetail.hasMore) return null
        val lastKey = pageTokens[resource.id] ?: return null
        return scanPage(endpoint, resource, exclusiveStartKey = lastKey, existingItems = dynamoDetail.items)
    }

    private suspend fun scanPage(
        endpoint: String,
        resource: InspectorResource,
        exclusiveStartKey: Map<String, AttributeValue>?,
        existingItems: List<Map<String, String>>,
    ): InspectorDetail.DynamoDetail =
        buildClient(endpoint).use { client ->
            val response =
                client.scan(
                    ScanRequest {
                        tableName = resource.id
                        limit = PAGE_SIZE
                        if (exclusiveStartKey != null) this.exclusiveStartKey = exclusiveStartKey
                    },
                )
            val rawItems = response.items ?: emptyList()
            val newItems =
                rawItems.map { item ->
                    item.entries.associate { (k, v) -> k to attributeValueToString(v) }
                }
            val allItems = existingItems + newItems
            val columns = allItems.flatMap { it.keys }.distinct()

            val lastKey = response.lastEvaluatedKey
            if (lastKey != null) pageTokens[resource.id] = lastKey else pageTokens.remove(resource.id)

            InspectorDetail.DynamoDetail(
                items = allItems,
                columns = columns,
                hasMore = lastKey != null,
            )
        }

    private fun attributeValueToString(value: AttributeValue): String =
        when (value) {
            is AttributeValue.S -> value.value
            is AttributeValue.N -> value.value
            is AttributeValue.Bool -> value.value.toString()
            is AttributeValue.Null -> "null"
            is AttributeValue.L -> "[${value.value.joinToString(", ") { attributeValueToString(it) }}]"
            is AttributeValue.M -> "{${value.value.entries.joinToString(", ") { (k, v) -> "$k: ${attributeValueToString(v)}" }}}"
            is AttributeValue.Ss -> value.value.toString()
            is AttributeValue.Ns -> value.value.toString()
            is AttributeValue.Bs -> "[binary set]"
            is AttributeValue.B -> "[binary]"
            else -> value.toString()
        }

    companion object {
        private const val PAGE_SIZE = 50
    }
}
