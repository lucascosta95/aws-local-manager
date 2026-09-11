package dev.lucascosta.awslocalmanager.data.remote

import dev.lucascosta.awslocalmanager.constants.AppConstants.EMULATOR_HEALTH_PATH
import dev.lucascosta.awslocalmanager.constants.AppConstants.HTTP_CONNECT_TIMEOUT_MS
import dev.lucascosta.awslocalmanager.constants.AppConstants.HTTP_REQUEST_TIMEOUT_MS
import dev.lucascosta.awslocalmanager.data.model.aws.AwsService
import dev.lucascosta.awslocalmanager.data.model.health.HealthResponse
import dev.lucascosta.awslocalmanager.data.model.health.ServiceStatus
import dev.lucascosta.awslocalmanager.domain.AppLogger
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class EmulatorClient {
    private companion object {
        const val LOG_SOURCE = "Emulator"
    }

    private val httpClient =
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                    },
                )
            }

            install(HttpTimeout) {
                requestTimeoutMillis = HTTP_REQUEST_TIMEOUT_MS
                connectTimeoutMillis = HTTP_CONNECT_TIMEOUT_MS
            }
        }

    suspend fun fetchHealth(endpoint: String): Result<List<AwsService>> {
        val result =
            runCatching {
                val response = httpClient.get("$endpoint$EMULATOR_HEALTH_PATH")
                val health = response.body<HealthResponse>()
                if (health.services.isEmpty()) {
                    error("Empty services from $EMULATOR_HEALTH_PATH")
                }

                health.services.map { (serviceName, statusText) ->
                    AwsService(
                        name = serviceName,
                        status = ServiceStatus.from(statusText),
                        endpoint = endpoint,
                    )
                }.sortedBy { it.name }
            }

        if (result.isSuccess) return result

        AppLogger.warn(LOG_SOURCE, "Could not read health from $endpoint$EMULATOR_HEALTH_PATH", result.exceptionOrNull())
        return Result.failure(
            Exception("Could not reach emulator at $endpoint", result.exceptionOrNull()),
        )
    }

    suspend fun isReachable(endpoint: String): Boolean {
        return runCatching { httpClient.get("$endpoint$EMULATOR_HEALTH_PATH") }.isSuccess
    }

    fun close() {
        httpClient.close()
    }
}
