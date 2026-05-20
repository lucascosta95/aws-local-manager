package dev.lucascosta.awslocalmanager.data.remote

import aws.sdk.kotlin.services.sns.SnsClient
import aws.sdk.kotlin.services.sns.model.ListTopicsRequest
import aws.sdk.kotlin.services.sns.model.PublishRequest
import aws.smithy.kotlin.runtime.net.url.Url
import dev.lucascosta.awslocalmanager.constants.AppConstants.UNKNOWN

class AwsSnsClient(private val endpointUrl: String) {
    private fun buildClient() =
        SnsClient {
            region = EmulatorDefaults.AWS_REGION
            endpointUrl = Url.parse(this@AwsSnsClient.endpointUrl)
            credentialsProvider = EmulatorConfig.credentialsProvider
        }

    suspend fun listTopics(): Result<List<String>> =
        runCatching {
            buildClient().use { client ->
                val response = client.listTopics(ListTopicsRequest {})
                response.topics?.mapNotNull { it.topicArn } ?: emptyList()
            }
        }

    suspend fun publish(
        topicArn: String,
        message: String,
    ): Result<String> =
        runCatching {
            buildClient().use { client ->
                val response =
                    client.publish(
                        PublishRequest {
                            this.topicArn = topicArn
                            this.message = message
                        },
                    )
                response.messageId ?: UNKNOWN
            }
        }
}
