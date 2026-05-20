package dev.lucascosta.awslocalmanager.domain

import dev.lucascosta.awslocalmanager.data.model.health.AppServiceStatus
import dev.lucascosta.awslocalmanager.data.model.resources.S3Resource
import dev.lucascosta.awslocalmanager.data.remote.AwsS3Client
import java.util.concurrent.ConcurrentHashMap

class S3HealthProbe(
    private val clientFactory: (String) -> AwsS3Client = ::AwsS3Client,
) : ServiceHealthProbe {
    override val resourceType = S3Resource

    private val clientCache = ConcurrentHashMap<String, AwsS3Client>()

    private fun clientFor(endpoint: String): AwsS3Client =
        clientCache.computeIfAbsent(endpoint, clientFactory)

    override suspend fun check(endpoint: String): AppServiceStatus = probeResources { clientFor(endpoint).listBuckets().getOrThrow() }
}
