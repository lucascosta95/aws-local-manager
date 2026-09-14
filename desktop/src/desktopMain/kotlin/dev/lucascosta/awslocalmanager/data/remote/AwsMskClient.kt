package dev.lucascosta.awslocalmanager.data.remote

import dev.lucascosta.awslocalmanager.data.model.process.ProcessConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

data class MskClusterInfo(
    val name: String,
    val arn: String,
    val type: String,
    val state: String,
    val kafkaVersion: String?,
    val brokerNodes: Int?,
    val instanceType: String?,
) {
    val isActive: Boolean get() = state == ACTIVE_STATE

    companion object {
        const val ACTIVE_STATE = "ACTIVE"
    }
}

class AwsMskClient(private val endpointUrl: String) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun listClusters(): Result<List<MskClusterInfo>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val stdout = runKafkaCommand(listOf("list-clusters-v2"))
                if (stdout.isBlank()) {
                    emptyList()
                } else {
                    json.decodeFromString<ListClustersResponse>(stdout).clusters.map { it.toInfo() }
                }
            }
        }

    suspend fun findCluster(name: String): Result<MskClusterInfo?> = listClusters().map { clusters -> clusters.find { it.name == name } }

    private suspend fun runKafkaCommand(arguments: List<String>): String {
        val result =
            ProcessRunner.run(
                command = listOf("aws", "kafka") + arguments + listOf("--output", "json"),
                config = ProcessConfig(envVars = ProcessRunner.awsEnvVars(endpointUrl)),
            ).getOrThrow()
        if (result.exitCode != 0) {
            error(result.stderr.ifBlank { "kafka ${arguments.first()} failed (exit ${result.exitCode})" })
        }
        return result.stdout
    }

    @Serializable
    private data class ListClustersResponse(
        @SerialName("ClusterInfoList") val clusters: List<ClusterDto> = emptyList(),
    )

    @Serializable
    private data class ClusterDto(
        @SerialName("ClusterName") val name: String,
        @SerialName("ClusterArn") val arn: String,
        @SerialName("ClusterType") val type: String = "",
        @SerialName("State") val state: String = "",
        @SerialName("Provisioned") val provisioned: ProvisionedDto? = null,
    ) {
        fun toInfo() =
            MskClusterInfo(
                name = name,
                arn = arn,
                type = type,
                state = state,
                kafkaVersion = provisioned?.softwareInfo?.kafkaVersion,
                brokerNodes = provisioned?.brokerNodes,
                instanceType = provisioned?.brokerNodeGroupInfo?.instanceType,
            )
    }

    @Serializable
    private data class ProvisionedDto(
        @SerialName("BrokerNodeGroupInfo") val brokerNodeGroupInfo: BrokerNodeGroupInfoDto? = null,
        @SerialName("CurrentBrokerSoftwareInfo") val softwareInfo: SoftwareInfoDto? = null,
        @SerialName("NumberOfBrokerNodes") val brokerNodes: Int? = null,
    )

    @Serializable
    private data class BrokerNodeGroupInfoDto(
        @SerialName("InstanceType") val instanceType: String? = null,
    )

    @Serializable
    private data class SoftwareInfoDto(
        @SerialName("KafkaVersion") val kafkaVersion: String? = null,
    )
}
