package dev.lucascosta.awslocalmanager.data.remote

import aws.sdk.kotlin.services.sqs.SqsClient
import aws.sdk.kotlin.services.sqs.model.ListQueuesRequest
import aws.sdk.kotlin.services.sqs.model.SendMessageRequest
import aws.smithy.kotlin.runtime.net.url.Url
import dev.lucascosta.awslocalmanager.constants.AppConstants.UNKNOWN

class AwsSqsClient(private val endpointUrl: String) {
    private fun buildClient() =
        SqsClient {
            region = EmulatorDefaults.AWS_REGION
            endpointUrl = Url.parse(this@AwsSqsClient.endpointUrl)
            credentialsProvider = EmulatorConfig.credentialsProvider
        }

    suspend fun listQueues(): Result<List<String>> =
        runCatching {
            buildClient().use { client ->
                val response = client.listQueues(ListQueuesRequest {})
                response.queueUrls ?: emptyList()
            }
        }

    suspend fun sendMessage(
        queueUrl: String,
        messageBody: String,
    ): Result<String> =
        runCatching {
            buildClient().use { client ->
                val response =
                    client.sendMessage(
                        SendMessageRequest {
                            this.queueUrl = queueUrl
                            this.messageBody = messageBody
                        },
                    )
                response.messageId ?: UNKNOWN
            }
        }
}
