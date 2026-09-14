package dev.lucascosta.awslocalmanager.data.remote

import dev.lucascosta.awslocalmanager.data.model.process.ProcessConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

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
    val value: String,
    val headers: Map<String, String>,
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
        val producedPattern = Regex("""Produced to partition (\d+) at offset (\d+)""")
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
            json.decodeFromString<List<TopicDto>>(stdout.ifBlank { "[]" }).map { KafkaTopicInfo(it.name, it.partitions, it.replicas) }
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
                "--format",
                "json",
                "--pretty-print=false",
            )
        return runRpk(container, arguments).mapCatching { stdout ->
            stdout.lineSequence()
                .filter { it.isNotBlank() }
                .map { line -> json.decodeFromString<RecordDto>(line).toRecord() }
                .toList()
        }
    }

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
    private data class RecordDto(
        val key: String = "",
        val value: String = "",
        val headers: List<HeaderDto> = emptyList(),
        val timestamp: Long = 0,
        val partition: Int = 0,
        val offset: Long = 0,
    ) {
        fun toRecord() =
            KafkaRecord(
                partition = partition,
                offset = offset,
                timestamp = timestamp,
                key = key,
                value = value,
                headers = headers.associate { it.key to it.value },
            )
    }

    @Serializable
    private data class HeaderDto(
        val key: String,
        val value: String = "",
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
