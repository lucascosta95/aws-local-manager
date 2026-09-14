package dev.lucascosta.awslocalmanager.data.remote

import dev.lucascosta.awslocalmanager.constants.AppConstants.EMULATOR_DOCKER_NETWORK
import dev.lucascosta.awslocalmanager.constants.AppConstants.KAFKA_PROXY_FIRST_HOST_PORT
import dev.lucascosta.awslocalmanager.constants.AppConstants.KAFKA_PROXY_IMAGE
import dev.lucascosta.awslocalmanager.constants.AppConstants.KAFKA_PROXY_LAST_HOST_PORT
import dev.lucascosta.awslocalmanager.constants.AppConstants.LOOPBACK_HOST
import dev.lucascosta.awslocalmanager.constants.AppConstants.MSK_BROKER_PORT
import dev.lucascosta.awslocalmanager.constants.AppConstants.SCHEMA_REGISTRY_PORT
import dev.lucascosta.awslocalmanager.constants.AppConstants.SCHEMA_REGISTRY_PROXY_FIRST_HOST_PORT
import dev.lucascosta.awslocalmanager.constants.AppConstants.SCHEMA_REGISTRY_PROXY_IMAGE
import dev.lucascosta.awslocalmanager.constants.AppConstants.SCHEMA_REGISTRY_PROXY_LAST_HOST_PORT
import dev.lucascosta.awslocalmanager.data.model.process.ProcessConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.InetAddress
import java.net.ServerSocket

enum class HostProxyKind(
    val role: String,
    val containerPrefix: String,
    val hostPorts: IntRange,
) {
    // The broker embeds its own address in Kafka responses, so this proxy has to rewrite them, not just forward bytes.
    KAFKA("kafka-host-proxy", "aws-local-manager-kafka-", KAFKA_PROXY_FIRST_HOST_PORT..KAFKA_PROXY_LAST_HOST_PORT) {
        override fun addressFor(hostPort: Int) = "localhost:$hostPort"

        override fun containerArguments(
            broker: String,
            hostPort: Int,
        ) = listOf(
            KAFKA_PROXY_IMAGE,
            "server",
            "--bootstrap-server-mapping",
            "$broker:$MSK_BROKER_PORT,0.0.0.0:$hostPort,localhost:$hostPort",
        )
    },
    SCHEMA_REGISTRY(
        "schema-registry-host-proxy",
        "aws-local-manager-schema-registry-",
        SCHEMA_REGISTRY_PROXY_FIRST_HOST_PORT..SCHEMA_REGISTRY_PROXY_LAST_HOST_PORT,
    ) {
        override fun addressFor(hostPort: Int) = "http://localhost:$hostPort"

        override fun containerArguments(
            broker: String,
            hostPort: Int,
        ) = listOf(
            SCHEMA_REGISTRY_PROXY_IMAGE,
            "tcp-listen:$hostPort,fork,reuseaddr",
            "tcp-connect:$broker:$SCHEMA_REGISTRY_PORT",
        )
    },
    ;

    abstract fun addressFor(hostPort: Int): String

    abstract fun containerArguments(
        broker: String,
        hostPort: Int,
    ): List<String>
}

data class HostProxy(
    val kind: HostProxyKind,
    val container: String,
    val cluster: String,
    val broker: String,
    val hostPort: Int,
    val isRunning: Boolean,
) {
    val hostAddress: String get() = kind.addressFor(hostPort)
}

class HostProxyClient {
    private companion object {
        const val ROLE_LABEL = "aws-local-manager.role"
        const val CLUSTER_LABEL = "aws-local-manager.msk-cluster"
        const val BROKER_LABEL = "aws-local-manager.msk-broker"
        const val PORT_LABEL = "aws-local-manager.host-port"
        const val RUNNING_STATE = "running"
        const val FIELD_COUNT = 6

        // The first start pulls the proxy image, which takes far longer than a regular docker command.
        const val START_TIMEOUT_SECONDS = 600L
    }

    suspend fun list(): List<HostProxy> =
        withContext(Dispatchers.IO) {
            val fields = listOf(".Names", ".State", label(ROLE_LABEL), label(CLUSTER_LABEL), label(BROKER_LABEL), label(PORT_LABEL))
            val format = fields.joinToString("\t") { "{{$it}}" }
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
        kind: HostProxyKind,
        cluster: String,
        broker: String,
        hostPort: Int,
    ): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val command = startCommand(kind, cluster, broker, hostPort)
                val result = ProcessRunner.run(command, ProcessConfig(timeoutSeconds = START_TIMEOUT_SECONDS)).getOrThrow()
                if (result.exitCode != 0) {
                    error(result.stderr.ifBlank { "docker run for the ${kind.role} of $cluster failed (exit ${result.exitCode})" })
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
        kind: HostProxyKind,
        cluster: String,
        broker: String,
        hostPort: Int,
    ): List<String> =
        buildList {
            addAll(listOf("docker", "run", "-d", "--name", "${kind.containerPrefix}$cluster", "--network", EMULATOR_DOCKER_NETWORK))
            addAll(listOf("--label", "$ROLE_LABEL=${kind.role}", "--label", "$CLUSTER_LABEL=$cluster"))
            addAll(listOf("--label", "$BROKER_LABEL=$broker", "--label", "$PORT_LABEL=$hostPort"))
            addAll(listOf("-p", "$LOOPBACK_HOST:$hostPort:$hostPort"))
            addAll(kind.containerArguments(broker, hostPort))
        }

    private fun parseProxy(line: String): HostProxy? {
        val fields = line.split("\t")
        if (fields.size != FIELD_COUNT) return null
        val (container, state, role) = fields
        val (cluster, broker, port) = fields.drop(3)
        return HostProxy(
            kind = HostProxyKind.entries.firstOrNull { it.role == role } ?: return null,
            container = container,
            cluster = cluster,
            broker = broker,
            hostPort = port.toIntOrNull() ?: return null,
            isRunning = state == RUNNING_STATE,
        )
    }

    private fun label(name: String) = ".Label \"$name\""
}
