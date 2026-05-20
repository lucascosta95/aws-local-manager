package dev.lucascosta.awslocalmanager.features.inspector.handler

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Message
import androidx.compose.ui.graphics.vector.ImageVector
import aws.sdk.kotlin.services.sqs.SqsClient
import aws.sdk.kotlin.services.sqs.model.GetQueueAttributesRequest
import aws.sdk.kotlin.services.sqs.model.ListQueuesRequest
import aws.sdk.kotlin.services.sqs.model.PurgeQueueRequest
import aws.sdk.kotlin.services.sqs.model.QueueAttributeName
import aws.sdk.kotlin.services.sqs.model.ReceiveMessageRequest
import aws.smithy.kotlin.runtime.net.url.Url
import dev.lucascosta.awslocalmanager.data.model.inspector.InspectorDetail
import dev.lucascosta.awslocalmanager.data.model.inspector.InspectorResource
import dev.lucascosta.awslocalmanager.data.model.inspector.SqsInspectorMessage
import dev.lucascosta.awslocalmanager.data.remote.EmulatorConfig
import dev.lucascosta.awslocalmanager.data.remote.EmulatorDefaults

class SqsInspectorHandler : InspectorServiceHandler {
    override val serviceKey: String = "sqs"
    override val displayName: String = "SQS"
    override val icon: ImageVector = Icons.AutoMirrored.Outlined.Message

    private fun buildClient(endpoint: String) =
        SqsClient {
            region = EmulatorDefaults.AWS_REGION
            endpointUrl = Url.parse(endpoint)
            credentialsProvider = EmulatorConfig.credentialsProvider
        }

    override suspend fun loadResources(endpoint: String): List<InspectorResource> =
        buildClient(endpoint).use { client ->
            val urls = client.listQueues(ListQueuesRequest {}).queueUrls ?: emptyList()
            urls.map { url ->
                val queueName = url.substringAfterLast("/")
                val approxCount = runCatching {
                    client.getQueueAttributes(
                        GetQueueAttributesRequest {
                            queueUrl = url
                            attributeNames = listOf(QueueAttributeName.ApproximateNumberOfMessages)
                        },
                    ).attributes?.get(QueueAttributeName.ApproximateNumberOfMessages) ?: "0"
                }.getOrElse { "?" }
                InspectorResource(
                    id = url,
                    name = queueName,
                    summaryType = "sqs",
                    summaryCount = approxCount.toLongOrNull(),
                )
            }
        }

    override suspend fun loadDetail(endpoint: String, resource: InspectorResource): InspectorDetail =
        buildClient(endpoint).use { client ->
            val response = client.receiveMessage(
                ReceiveMessageRequest {
                    queueUrl = resource.id
                    maxNumberOfMessages = 10
                    visibilityTimeout = 0
                    waitTimeSeconds = 0
                    messageAttributeNames = listOf("All")
                },
            )
            val messages = (response.messages ?: emptyList()).map { msg ->
                SqsInspectorMessage(
                    messageId = msg.messageId ?: "",
                    body = msg.body ?: "",
                    attributes = msg.attributes?.entries
                        ?.associate { (k, v) -> k.value to v } ?: emptyMap(),
                )
            }
            InspectorDetail.SqsDetail(messages = messages, queueUrl = resource.id)
        }

    override suspend fun performAction(
        endpoint: String,
        resource: InspectorResource,
        actionId: String,
    ): Result<Unit> =
        if (actionId == ACTION_PURGE) {
            runCatching {
                buildClient(endpoint).use { client ->
                    client.purgeQueue(PurgeQueueRequest { queueUrl = resource.id })
                }
            }
        } else {
            super.performAction(endpoint, resource, actionId)
        }

    companion object {
        const val ACTION_PURGE = "purge"
    }
}
