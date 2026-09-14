package dev.lucascosta.awslocalmanager.data.remote

import dev.lucascosta.awslocalmanager.constants.AppConstants.EMULATOR_DOCKER_NETWORK
import dev.lucascosta.awslocalmanager.constants.AppConstants.KAFKA_PROXY_IMAGE
import dev.lucascosta.awslocalmanager.constants.AppConstants.LOOPBACK_HOST
import dev.lucascosta.awslocalmanager.constants.AppConstants.MSK_BROKER_PORT
import dev.lucascosta.awslocalmanager.data.model.process.ProcessConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.InetAddress
import java.net.ServerSocket

data class KafkaHostProxy(
    val container: String,
    val cluster: String,
    val broker: String,
    val hostPort: Int,
    val isRunning: Boolean,
) {
    val hostAddress: String get() = "localhost:$hostPort"
}

class KafkaHostProxyClient {
    private companion object {
        const val ROLE_LABEL = "aws-local-manager.role=kafka-host-proxy"
        const val CLUSTER_LABEL = "aws-local-manager.msk-cluster"
        const val BROKER_LABEL = "aws-local-manager.msk-broker"
        const val PORT_LABEL = "aws-local-manager.host-port"
        const val CONTAINER_PREFIX = "aws-local-manager-kafka-"
        const val RUNNING_STATE = "running"
        const val FIELD_COUNT = 5

        // The first start pulls the proxy image, which takes far longer than a regular docker command.
        const val START_TIMEOUT_SECONDS = 600L
    }

    suspend fun list(): List<KafkaHostProxy> =
        withContext(Dispatchers.IO) {
            val format =
                listOf(
                    ".Names",
                    ".State",
                    label(CLUSTER_LABEL),
                    label(BROKER_LABEL),
                    label(PORT_LABEL),
                ).joinToString("\t") { "{{$it}}" }
            ProcessRunner.run(listOf("docker", "ps", "-a", "--filter", "label=$ROLE_LABEL", "--format", format))
                .getOrNull()
                ?.takeIf { it.exitCode == 0 }
                ?.stdout
                ?.lineSequence()
                ?.mapNotNull(::parseProxy)
                ?.toList()
                .orEmpty()
        }

    suspend fun start(
        cluster: String,
        broker: String,
        hostPort: Int,
    ): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val result =
                    ProcessRunner.run(
                        startCommand(cluster, broker, hostPort),
                        ProcessConfig(timeoutSeconds = START_TIMEOUT_SECONDS),
                    ).getOrThrow()
                if (result.exitCode != 0) {
                    error(result.stderr.ifBlank { "docker run for the Kafka proxy of $cluster failed (exit ${result.exitCode})" })
                }
            }
        }

    suspend fun remove(container: String) {
        ProcessRunner.run(listOf("docker", "rm", "-f", container))
    }

    fun isHostPortFree(port: Int): Boolean =
        try {
            ServerSocket(port, 1, InetAddress.getByName(LOOPBACK_HOST)).use { true }
        } catch (_: IOException) {
            false
        }

    private fun startCommand(
        cluster: String,
        broker: String,
        hostPort: Int,
    ): List<String> =
        buildList {
            addAll(listOf("docker", "run", "-d", "--name", "$CONTAINER_PREFIX$cluster", "--network", EMULATOR_DOCKER_NETWORK))
            addAll(listOf("--label", ROLE_LABEL, "--label", "$CLUSTER_LABEL=$cluster"))
            addAll(listOf("--label", "$BROKER_LABEL=$broker", "--label", "$PORT_LABEL=$hostPort"))
            addAll(listOf("-p", "$LOOPBACK_HOST:$hostPort:$hostPort", KAFKA_PROXY_IMAGE, "server"))
            addAll(listOf("--bootstrap-server-mapping", "$broker:$MSK_BROKER_PORT,0.0.0.0:$hostPort,localhost:$hostPort"))
        }

    private fun parseProxy(line: String): KafkaHostProxy? {
        val fields = line.split("\t")
        if (fields.size != FIELD_COUNT) return null
        val (container, state, cluster) = fields
        val (broker, port) = fields.drop(3)
        return KafkaHostProxy(
            container = container,
            cluster = cluster,
            broker = broker,
            hostPort = port.toIntOrNull() ?: return null,
            isRunning = state == RUNNING_STATE,
        )
    }

    private fun label(name: String) = ".Label \"$name\""
}
