package dev.lucascosta.awslocalmanager.data.remote

import dev.lucascosta.awslocalmanager.data.model.process.ProcessConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.Base64

data class KafkaTopicInfo(
    val name: String,
    val partitions: Int,
    val replicas: Int,
)

data class KafkaRecord(
    val partition: Int,
    val offset: Long,
    val timestamp: Long,
    val key: String,
    val value: KafkaRecordPayload,
    val headers: Map<String, String>,
)

data class KafkaSchemaSubject(
    val subject: String,
    val version: Int,
    val id: Int,
    val type: String,
    val schema: String,
)

data class KafkaConsumerGroup(
    val name: String,
    val state: String,
    val members: Int,
    val totalLag: Long,
)

class KafkaBrokerClient {
    private companion object {
        const val SERVICE_LABEL = "io.floci.service=msk"
        const val RESOURCE_LABEL = "io.floci.resource-id"
        const val TOPIC_ALREADY_EXISTS = "TOPIC_ALREADY_EXISTS"

        // Kafka tooling reserves a leading underscore for internal topics, such as _schemas, which backs the schema registry.
        const val INTERNAL_TOPIC_PREFIX = "_"
        val producedPattern = Regex("""Produced to partition (\d+) at offset (\d+)""")

        // Base64 keeps binary keys, values and headers on a single space-separated line.
        const val RECORD_FORMAT = "%p %o %d %k{base64} %v{base64} %h{%k{base64}:%v{base64},}\\n"
        const val RECORD_REQUIRED_FIELDS = 5
    }

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun findBrokerContainer(clusterName: String): String? = listBrokerContainers()[clusterName]

    suspend fun listBrokerContainers(): Map<String, String> =
        withContext(Dispatchers.IO) {
            val command =
                listOf("docker", "ps", "--filter", "label=$SERVICE_LABEL", "--format", "{{.Label \"$RESOURCE_LABEL\"}}\t{{.Names}}")
            ProcessRunner.run(command)
                .getOrNull()
                ?.takeIf { it.exitCode == 0 }
                ?.stdout
                ?.lineSequence()
                ?.mapNotNull { line -> line.split("\t").takeIf { it.size == 2 && it.all(String::isNotBlank) } }
                ?.associate { (cluster, container) -> cluster.trim() to container.trim() }
                .orEmpty()
        }

    suspend fun listTopics(container: String): Result<List<KafkaTopicInfo>> =
        runRpk(container, listOf("topic", "list", "--format", "json")).map { stdout ->
            json.decodeFromString<List<TopicDto>>(stdout.ifBlank { "[]" })
                .filterNot { it.name.startsWith(INTERNAL_TOPIC_PREFIX) }
                .map { KafkaTopicInfo(it.name, it.partitions, it.replicas) }
        }

    suspend fun createTopic(
        container: String,
        topic: String,
        partitions: Int,
    ): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val arguments = listOf("topic", "create", topic, "--partitions", partitions.toString(), "--replicas", "1")
                val result = ProcessRunner.run(KafkaBrokerCommands.rpk(container, arguments)).getOrThrow()
                val output = result.stdout + result.stderr
                if (result.exitCode != 0 && TOPIC_ALREADY_EXISTS !in output) {
                    error(output.lineSequence().lastOrNull { it.isNotBlank() } ?: "rpk topic create failed (exit ${result.exitCode})")
                }
            }
        }

    suspend fun produce(
        container: String,
        topic: String,
        value: String,
    ): Result<String> =
        runRpk(container, listOf("topic", "produce", topic), stdin = "$value\n").map { stdout ->
            val match = producedPattern.find(stdout) ?: error(stdout.ifBlank { "rpk topic produce returned no confirmation" })
            "${match.groupValues[1]}@${match.groupValues[2]}"
        }

    suspend fun consumeLatest(
        container: String,
        topic: String,
        limit: Int,
    ): Result<List<KafkaRecord>> =
        runRpk(container, listOf("topic", "describe", topic, "--print-partitions", "--format", "json")).mapCatching { stdout ->
            json.decodeFromString<List<TopicDescriptionDto>>(stdout)
                .flatMap { it.partitions }
                .filter { it.highWatermark > it.logStartOffset }
                .flatMap { partition -> consumePartitionTail(container, topic, partition, limit).getOrThrow() }
                .sortedWith(compareByDescending<KafkaRecord> { it.timestamp }.thenByDescending { it.offset })
                .take(limit)
        }

    private suspend fun consumePartitionTail(
        container: String,
        topic: String,
        partition: PartitionDto,
        limit: Int,
    ): Result<List<KafkaRecord>> {
        val from = maxOf(partition.logStartOffset, partition.highWatermark - limit)
        val arguments =
            listOf(
                "topic",
                "consume",
                topic,
                "--partitions",
                partition.partition.toString(),
                "--offset",
                "$from:${partition.highWatermark}",
                "--use-schema-registry=value",
                "--format",
                RECORD_FORMAT,
            )
        return runRpkCapturingBoth(container, arguments).mapCatching { output ->
            output.lineSequence().filter { it.isNotBlank() }.mapNotNull(::parseRecord).toList()
        }
    }

    private fun parseRecord(line: String): KafkaRecord? {
        val fields = line.split(" ")
        if (fields.size < RECORD_REQUIRED_FIELDS) return null
        val (partition, offset, timestamp) = fields
        val (key, value) = fields.drop(3)
        // Output is trimmed, so a last record without headers loses its trailing separator and the headers field.
        val headers = fields.getOrElse(RECORD_REQUIRED_FIELDS) { "" }
        return KafkaRecord(
            partition = partition.toIntOrNull() ?: return null,
            offset = offset.toLongOrNull() ?: return null,
            timestamp = timestamp.toLongOrNull() ?: return null,
            key = decodeText(key),
            value = KafkaRecordPayload.fromBase64(value),
            headers =
                headers.split(",").filter { it.contains(":") }.associate { header ->
                    decodeText(header.substringBefore(":")) to decodeText(header.substringAfter(":"))
                },
        )
    }

    private fun decodeText(base64: String) = String(Base64.getDecoder().decode(base64), Charsets.UTF_8)

    suspend fun listConsumerGroups(container: String): Result<List<KafkaConsumerGroup>> =
        runRpk(container, listOf("group", "list", "--format", "json")).mapCatching { stdout ->
            val names = json.decodeFromString<List<GroupListDto>>(stdout.ifBlank { "[]" }).map { it.group }
            if (names.isEmpty()) {
                emptyList()
            } else {
                val described = runRpk(container, listOf("group", "describe") + names + listOf("--format", "json")).getOrThrow()
                json.decodeFromString<List<GroupDescriptionDto>>(described).map { it.toGroup() }
            }
        }

    suspend fun listSchemaSubjects(container: String): Result<List<KafkaSchemaSubject>> =
        runRpk(container, listOf("registry", "schema", "list", "--format", "json")).mapCatching { stdout ->
            json.decodeFromString<List<SchemaVersionDto>>(stdout.ifBlank { "[]" })
                .groupBy { it.subject }
                .map { (_, versions) -> versions.maxBy { it.version } }
                .sortedBy { it.subject }
                .map { latest ->
                    val arguments = listOf("registry", "schema", "get", latest.subject, "--schema-version", "latest", "--print-schema")
                    val schema = runRpk(container, arguments).getOrThrow()
                    KafkaSchemaSubject(latest.subject, latest.version, latest.id, latest.type, schema)
                }
        }

    // With --use-schema-registry, rpk prints records it cannot decode through the registry to stderr instead of stdout.
    private suspend fun runRpkCapturingBoth(
        container: String,
        arguments: List<String>,
    ): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val result = ProcessRunner.run(KafkaBrokerCommands.rpk(container, arguments)).getOrThrow()
                check(result.exitCode == 0) { result.stderr.ifBlank { "rpk ${arguments.take(2).joinToString(" ")} failed" } }
                result.stdout + "\n" + result.stderr
            }
        }

    private suspend fun runRpk(
        container: String,
        arguments: List<String>,
        stdin: String? = null,
    ): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val result =
                    ProcessRunner.run(
                        command = KafkaBrokerCommands.rpk(container, arguments),
                        config = ProcessConfig(stdin = stdin),
                    ).getOrThrow()
                if (result.exitCode != 0) {
                    error(result.stderr.ifBlank { result.stdout }.ifBlank { "rpk ${arguments.take(2).joinToString(" ")} failed" })
                }
                result.stdout
            }
        }

    @Serializable
    private data class TopicDto(
        val name: String,
        val partitions: Int = 0,
        val replicas: Int = 0,
    )

    @Serializable
    private data class TopicDescriptionDto(
        val partitions: List<PartitionDto> = emptyList(),
    )

    @Serializable
    private data class PartitionDto(
        val partition: Int,
        @SerialName("log_start_offset") val logStartOffset: Long = 0,
        @SerialName("high_watermark") val highWatermark: Long = 0,
    )

    @Serializable
    private data class SchemaVersionDto(
        val subject: String,
        val version: Int,
        val id: Int = 0,
        val type: String = "",
    )

    @Serializable
    private data class GroupListDto(
        val group: String,
    )

    @Serializable
    private data class GroupDescriptionDto(
        @SerialName("group_name") val name: String,
        val state: String = "",
        val members: Int = 0,
        @SerialName("total_lag") val totalLag: Long = 0,
    ) {
        fun toGroup() = KafkaConsumerGroup(name = name, state = state, members = members, totalLag = totalLag)
    }
}
