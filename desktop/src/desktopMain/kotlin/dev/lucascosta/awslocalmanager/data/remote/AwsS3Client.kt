package dev.lucascosta.awslocalmanager.data.remote

import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.ListBucketsRequest
import aws.sdk.kotlin.services.s3.model.PutObjectRequest
import aws.smithy.kotlin.runtime.content.ByteStream
import aws.smithy.kotlin.runtime.net.url.Url
import dev.lucascosta.awslocalmanager.data.model.aws.S3Upload

class AwsS3Client(private val endpointUrl: String) {
    private fun buildClient() =
        S3Client {
            region = EmulatorDefaults.AWS_REGION
            endpointUrl = Url.parse(this@AwsS3Client.endpointUrl)
            forcePathStyle = true
            credentialsProvider = EmulatorConfig.credentialsProvider
        }

    suspend fun listBuckets(): Result<List<String>> =
        runCatching {
            buildClient().use { client ->
                val response = client.listBuckets(ListBucketsRequest {})
                response.buckets?.mapNotNull { it.name } ?: emptyList()
            }
        }

    suspend fun uploadFile(upload: S3Upload): Result<String> =
        runCatching {
            buildClient().use { client ->
                client.putObject(
                    PutObjectRequest {
                        this.bucket = upload.bucketName
                        this.key = upload.key
                        this.contentType = upload.contentType
                        this.body = ByteStream.fromBytes(upload.bytes)
                    },
                )
                upload.key
            }
        }
}
