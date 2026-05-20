package dev.lucascosta.awslocalmanager.data.repository

import dev.lucascosta.awslocalmanager.data.model.aws.AwsResourceDefinition
import dev.lucascosta.awslocalmanager.data.model.aws.PublishResult
import dev.lucascosta.awslocalmanager.data.model.aws.S3Upload
import dev.lucascosta.awslocalmanager.data.model.resources.DynamoDbResource
import dev.lucascosta.awslocalmanager.data.model.resources.SnsResource
import dev.lucascosta.awslocalmanager.data.model.resources.SqsResource
import dev.lucascosta.awslocalmanager.data.model.resources.StepFunctionsResource
import dev.lucascosta.awslocalmanager.data.remote.AwsDynamoDbClient
import dev.lucascosta.awslocalmanager.data.remote.AwsS3Client
import dev.lucascosta.awslocalmanager.data.remote.AwsSnsClient
import dev.lucascosta.awslocalmanager.data.remote.AwsSqsClient
import dev.lucascosta.awslocalmanager.data.remote.AwsStepFunctionsClient

class MessageRepository(
    private val snsClient: AwsSnsClient,
    private val sqsClient: AwsSqsClient,
    private val s3Client: AwsS3Client,
    private val dynamoDbClient: AwsDynamoDbClient,
    private val stepFunctionsClient: AwsStepFunctionsClient,
) {
    suspend fun publish(
        type: AwsResourceDefinition,
        resource: String,
        message: String,
    ): Result<PublishResult>? =
        when (type) {
            SnsResource ->
                snsClient.publish(resource, message).map { id ->
                    PublishResult(success = true, messageId = id)
                }.recoverCatching { e ->
                    PublishResult(success = false, error = e.message)
                }

            SqsResource ->
                sqsClient.sendMessage(resource, message).map { id ->
                    PublishResult(success = true, messageId = id)
                }.recoverCatching { e ->
                    PublishResult(success = false, error = e.message)
                }

            DynamoDbResource ->
                dynamoDbClient.putItem(resource, message).map {
                    PublishResult(success = true)
                }.recoverCatching { e ->
                    PublishResult(success = false, error = e.message)
                }

            StepFunctionsResource ->
                stepFunctionsClient.startExecution(resource, message).map { executionArn ->
                    PublishResult(success = true, messageId = executionArn)
                }.recoverCatching { e ->
                    PublishResult(success = false, error = e.message)
                }

            else -> null
        }

    suspend fun uploadS3(
        bucket: String,
        key: String,
        bytes: ByteArray,
        contentType: String,
    ): Result<PublishResult> =
        s3Client.uploadFile(S3Upload(bucket, key, bytes, contentType)).map { uploadedKey ->
            PublishResult(success = true, messageId = uploadedKey)
        }.recoverCatching { e ->
            PublishResult(success = false, error = e.message)
        }
}
