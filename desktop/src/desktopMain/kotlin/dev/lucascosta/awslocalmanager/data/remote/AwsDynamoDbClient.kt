package dev.lucascosta.awslocalmanager.data.remote

import aws.sdk.kotlin.services.dynamodb.DynamoDbClient
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.sdk.kotlin.services.dynamodb.model.ListTablesRequest
import aws.sdk.kotlin.services.dynamodb.model.PutItemRequest
import aws.smithy.kotlin.runtime.net.url.Url
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonObject

class AwsDynamoDbClient(private val endpointUrl: String) {
    private fun buildClient() =
        DynamoDbClient {
            region = EmulatorDefaults.AWS_REGION
            endpointUrl = Url.parse(this@AwsDynamoDbClient.endpointUrl)
            credentialsProvider = EmulatorConfig.credentialsProvider
        }

    suspend fun listTables(): Result<List<String>> =
        runCatching {
            buildClient().use { client ->
                val response = client.listTables(ListTablesRequest {})
                response.tableNames ?: emptyList()
            }
        }

    suspend fun putItem(
        tableName: String,
        jsonItem: String,
    ): Result<Unit> =
        runCatching {
            val jsonObject = Json.parseToJsonElement(jsonItem).jsonObject
            val item = jsonObject.entries.associate { (key, value) -> key to jsonToAttributeValue(value) }
            buildClient().use { client ->
                client.putItem(
                    PutItemRequest {
                        this.tableName = tableName
                        this.item = item
                    },
                )
            }
        }

    private fun jsonToAttributeValue(element: JsonElement): AttributeValue =
        when (element) {
            is JsonNull -> AttributeValue.Null(true)
            is JsonPrimitive ->
                when {
                    element.isString -> AttributeValue.S(element.content)
                    element.booleanOrNull != null -> AttributeValue.Bool(element.booleanOrNull!!)
                    else -> AttributeValue.N(element.content)
                }

            is JsonArray -> AttributeValue.L(element.map { jsonToAttributeValue(it) })
            is JsonObject -> AttributeValue.M(element.entries.associate { (k, v) -> k to jsonToAttributeValue(v) })
        }
}
