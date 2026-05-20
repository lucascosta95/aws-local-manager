package dev.lucascosta.awslocalmanager.features.inspector.handler

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.ui.graphics.vector.ImageVector
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.ListBucketsRequest
import aws.sdk.kotlin.services.s3.model.ListObjectsV2Request
import aws.smithy.kotlin.runtime.net.url.Url
import dev.lucascosta.awslocalmanager.data.model.inspector.InspectorDetail
import dev.lucascosta.awslocalmanager.data.model.inspector.InspectorResource
import dev.lucascosta.awslocalmanager.data.model.inspector.S3InspectorObject
import dev.lucascosta.awslocalmanager.data.remote.EmulatorConfig
import dev.lucascosta.awslocalmanager.data.remote.EmulatorDefaults

class S3InspectorHandler : InspectorServiceHandler {
    override val serviceKey: String = "s3"
    override val displayName: String = "S3"
    override val icon: ImageVector = Icons.Outlined.FolderOpen

    private fun buildClient(endpoint: String) =
        S3Client {
            region = EmulatorDefaults.AWS_REGION
            endpointUrl = Url.parse(endpoint)
            forcePathStyle = true
            credentialsProvider = EmulatorConfig.credentialsProvider
        }

    override suspend fun loadResources(endpoint: String): List<InspectorResource> =
        buildClient(endpoint).use { client ->
            val buckets = client.listBuckets(ListBucketsRequest {}).buckets ?: emptyList()
            buckets.mapNotNull { bucket ->
                val name = bucket.name ?: return@mapNotNull null
                InspectorResource(
                    id = name,
                    name = name,
                    summaryType = "s3",
                )
            }
        }

    override suspend fun loadDetail(endpoint: String, resource: InspectorResource): InspectorDetail =
        listObjects(endpoint, bucket = resource.id, prefix = resource.currentPath)

    private suspend fun listObjects(
        endpoint: String,
        bucket: String,
        prefix: String,
    ): InspectorDetail.S3Detail =
        buildClient(endpoint).use { client ->
            val response = client.listObjectsV2(
                ListObjectsV2Request {
                    this.bucket = bucket
                    this.delimiter = "/"
                    if (prefix.isNotBlank()) this.prefix = prefix
                },
            )

            val prefixEntries = (response.commonPrefixes ?: emptyList()).mapNotNull { cp ->
                val fullPrefix = cp.prefix ?: return@mapNotNull null
                val displayName = fullPrefix.removePrefix(prefix).trimEnd('/')
                S3InspectorObject(
                    key = fullPrefix,
                    displayName = displayName,
                    sizeBytes = 0,
                    lastModified = "",
                    isPrefix = true,
                )
            }

            val objectEntries = (response.contents ?: emptyList()).mapNotNull { obj ->
                val key = obj.key ?: return@mapNotNull null
                if (key == prefix) return@mapNotNull null
                val displayName = key.removePrefix(prefix)
                S3InspectorObject(
                    key = key,
                    displayName = displayName,
                    sizeBytes = obj.size ?: 0L,
                    lastModified = obj.lastModified?.toString() ?: "",
                    isPrefix = false,
                )
            }

            InspectorDetail.S3Detail(
                entries = prefixEntries + objectEntries,
                currentPrefix = prefix,
            )
        }
}
