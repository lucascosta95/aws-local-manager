package dev.lucascosta.awslocalmanager.data.repository

import dev.lucascosta.awslocalmanager.data.model.aws.AwsService
import dev.lucascosta.awslocalmanager.data.remote.EmulatorClient

class ServiceRepository(private val client: EmulatorClient) {
    suspend fun getServices(endpoint: String): Result<List<AwsService>> = client.fetchHealth(endpoint)
}
