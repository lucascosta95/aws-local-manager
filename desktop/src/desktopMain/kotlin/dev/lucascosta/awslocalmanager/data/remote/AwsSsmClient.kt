package dev.lucascosta.awslocalmanager.data.remote

import dev.lucascosta.awslocalmanager.data.model.process.ProcessConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private const val GET_PARAMETERS_BATCH_SIZE = 10

data class SsmParameterInfo(
    val name: String,
    val type: String,
    val version: Long,
    val lastModified: String,
    val value: String? = null,
)

class AwsSsmClient(private val endpointUrl: String) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun listParameters(): Result<List<SsmParameterInfo>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val result =
                    ProcessRunner.run(
                        command = listOf("aws", "ssm", "describe-parameters", "--output", "json"),
                        config = ProcessConfig(envVars = ProcessRunner.awsEnvVars(endpointUrl)),
                    ).getOrThrow()
                if (result.exitCode != 0) {
                    error(result.stderr.ifBlank { "describe-parameters failed (exit ${result.exitCode})" })
                }
                if (result.stdout.isBlank()) {
                    emptyList()
                } else {
                    json.decodeFromString<DescribeParametersResponse>(result.stdout).parameters.map { it.toInfo() }
                }
            }
        }

    suspend fun getParameterValues(names: List<String>): Result<List<SsmParameterInfo>> =
        withContext(Dispatchers.IO) {
            runCatching {
                names.chunked(GET_PARAMETERS_BATCH_SIZE).flatMap { batch -> fetchBatch(batch) }
            }
        }

    private suspend fun fetchBatch(names: List<String>): List<SsmParameterInfo> {
        val command = listOf("aws", "ssm", "get-parameters", "--with-decryption", "--output", "json", "--names") + names
        val result =
            ProcessRunner.run(
                command = command,
                config = ProcessConfig(envVars = ProcessRunner.awsEnvVars(endpointUrl)),
            ).getOrElse { return emptyList() }

        if (result.exitCode != 0 || result.stdout.isBlank()) {
            return emptyList()
        }

        return runCatching {
            json.decodeFromString<GetParametersResponse>(result.stdout).parameters.map { it.toInfo() }
        }.getOrElse { emptyList() }
    }

    @Serializable
    private data class DescribeParametersResponse(
        @SerialName("Parameters") val parameters: List<ParameterDto> = emptyList(),
    )

    @Serializable
    private data class GetParametersResponse(
        @SerialName("Parameters") val parameters: List<ParameterDto> = emptyList(),
    )

    @Serializable
    private data class ParameterDto(
        @SerialName("Name") val name: String,
        @SerialName("Type") val type: String = "",
        @SerialName("Value") val value: String? = null,
        @SerialName("Version") val version: Long = 0,
        @SerialName("LastModifiedDate") val lastModifiedDate: String = "",
    ) {
        fun toInfo() =
            SsmParameterInfo(
                name = name,
                type = type,
                version = version,
                lastModified = lastModifiedDate,
                value = value,
            )
    }
}
