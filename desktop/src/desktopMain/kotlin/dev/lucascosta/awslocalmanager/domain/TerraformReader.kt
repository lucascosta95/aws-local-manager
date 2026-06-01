package dev.lucascosta.awslocalmanager.domain

import dev.lucascosta.awslocalmanager.constants.AppConstants.AWS_LOCAL_CONFIG_FILENAME
import dev.lucascosta.awslocalmanager.constants.AppConstants.PROJECT_INFRA_SUBDIR
import dev.lucascosta.awslocalmanager.constants.AppConstants.TERRAFORM_FILE_EXTENSION
import dev.lucascosta.awslocalmanager.data.model.aws.ResourceRegistry
import dev.lucascosta.awslocalmanager.data.model.aws.SnsSubscription
import dev.lucascosta.awslocalmanager.data.model.project.InfraProject
import dev.lucascosta.awslocalmanager.data.model.project.ProjectConfig
import dev.lucascosta.awslocalmanager.data.model.project.TerraformResource
import dev.lucascosta.awslocalmanager.data.model.resources.ElastiCacheEngine
import kotlinx.serialization.json.Json
import java.io.File

class TerraformReader {
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private val resourcePattern = Regex("""resource\s+"(aws_\w+)"\s+"(\w+)"\s*\{""")
        private val namePattern = Regex("""^\s*name\s*=\s*"([^"]+)"""", RegexOption.MULTILINE)
        private val snsSubscriptionPattern = Regex("""resource\s+"aws_sns_topic_subscription"\s+"(\w+)"\s*\{""")
        private val rawDeliveryPattern = Regex("""raw_message_delivery\s*=\s*true""")
        private val filterPolicyPattern = Regex("""filter_policy\s*=\s*jsonencode\s*\(\s*\{""")
    }

    fun findProjects(rootDir: File): List<InfraProject> {
        if (!rootDir.exists() || !rootDir.isDirectory) {
            return emptyList()
        }

        return rootDir.listFiles()
            ?.filter { it.isDirectory && !it.name.startsWith(".") }
            ?.mapNotNull { dir -> parseProjectDirectory(dir) }
            ?.sortedBy { it.name }
            ?: emptyList()
    }

    fun readResources(projectDir: File): List<TerraformResource> {
        if (!projectDir.exists() || !projectDir.isDirectory) {
            return emptyList()
        }

        val tfFiles = projectDir.listFiles { file -> file.extension == TERRAFORM_FILE_EXTENSION } ?: return emptyList()
        if (tfFiles.isEmpty()) {
            return emptyList()
        }

        return tfFiles.flatMap { parseResourcesFromFile(it) }.sortedBy { it.tfLabel }
    }

    private fun parseProjectDirectory(dir: File): InfraProject? {
        val infraDir = File(dir, PROJECT_INFRA_SUBDIR)
        val configFile = File(infraDir, AWS_LOCAL_CONFIG_FILENAME)
        if (!configFile.exists()) {
            return null
        }

        val config =
            runCatching { json.decodeFromString<ProjectConfig>(configFile.readText()) }
                .onFailure { System.err.println("[TerraformReader] Failed to parse config ${configFile.path}: ${it.message}") }
                .getOrNull()

        val resources = if (config != null) readResources(infraDir) else emptyList()
        return if (resources.isEmpty() || config == null) {
            null
        } else {
            InfraProject(name = config.name, directory = infraDir, resources = resources)
        }
    }

    private fun parseResourcesFromFile(file: File): List<TerraformResource> {
        val content = file.readText()
        return resourcePattern.findAll(content).map { match ->
            val awsPrefix = match.groupValues[1]
            val tfLabel = match.groupValues[2]
            val resourceType = ResourceRegistry.fromTerraformPrefix(awsPrefix)
            val blockContent = extractBlock(content, match.range.last + 1)
            val (awsName, extraProperties) =
                if (awsPrefix == "aws_elasticache_cluster") {
                    val clusterId = extractQuotedAttribute(blockContent, "cluster_id") ?: tfLabel.replace("_", "-")
                    val engine = extractQuotedAttribute(blockContent, "engine") ?: ElastiCacheEngine.REDIS.cliValue
                    val nodeType = extractQuotedAttribute(blockContent, "node_type") ?: "cache.t3.micro"
                    val numNodes = extractQuotedAttribute(blockContent, "num_cache_nodes") ?: "1"
                    val defaultPort =
                        if (engine == ElastiCacheEngine.REDIS.cliValue) {
                            ElastiCacheEngine.REDIS.defaultPort
                        } else {
                            ElastiCacheEngine.MEMCACHED.defaultPort
                        }
                    val port = extractQuotedAttribute(blockContent, "port") ?: defaultPort.toString()
                    clusterId to mapOf("engine" to engine, "node_type" to nodeType, "num_cache_nodes" to numNodes, "port" to port)
                } else {
                    (namePattern.find(blockContent)?.groupValues?.get(1) ?: tfLabel.replace("_", "-")) to emptyMap()
                }
            TerraformResource(
                tfLabel = tfLabel,
                awsName = awsName,
                resourceType = resourceType,
                rawAwsType = awsPrefix,
                filePath = file.absolutePath,
                extraProperties = extraProperties,
            )
        }.toList()
    }

    private fun extractBlock(
        content: String,
        startIndex: Int,
    ): String {
        var depth = 1
        var index = startIndex
        val stringBuilder = StringBuilder()
        while (index < content.length && depth > 0) {
            when (content[index]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) {
                        break
                    }
                }
            }

            stringBuilder.append(content[index])
            index++
        }

        return stringBuilder.toString()
    }

    fun readSnsSubscriptions(projectDir: File): List<SnsSubscription> {
        if (!projectDir.exists() || !projectDir.isDirectory) {
            return emptyList()
        }

        val tfFiles = projectDir.listFiles { file -> file.extension == TERRAFORM_FILE_EXTENSION } ?: return emptyList()
        return tfFiles.flatMap { parseSnsSubscriptions(it.readText()) }
    }

    private fun parseSnsSubscriptions(content: String): List<SnsSubscription> {
        val result = mutableListOf<SnsSubscription>()
        var searchStart = 0
        while (searchStart < content.length) {
            val match = snsSubscriptionPattern.find(content, searchStart) ?: break
            val tfLabel = match.groupValues[1]
            val blockStart = match.range.last + 1
            val blockContent = extractBlock(content, blockStart)

            val topicRef = extractAttributeValue(blockContent, "topic_arn")
            val endpointRef = extractAttributeValue(blockContent, "endpoint")

            if (topicRef != null && endpointRef != null) {
                val protocol = extractQuotedAttribute(blockContent, "protocol") ?: "sqs"
                val rawMessageDelivery = rawDeliveryPattern.containsMatchIn(blockContent)
                val filterPolicy = extractFilterPolicy(blockContent)
                val filterPolicyScope = extractQuotedAttribute(blockContent, "filter_policy_scope")
                result.add(SnsSubscription(tfLabel, topicRef, protocol, endpointRef, rawMessageDelivery, filterPolicy, filterPolicyScope))
            }

            searchStart = blockStart + blockContent.length + 1
        }

        return result
    }

    private fun extractAttributeValue(
        blockContent: String,
        key: String,
    ): String? {
        val quoted =
            Regex("""^\s*${Regex.escape(key)}\s*=\s*"([^"]+)"""", RegexOption.MULTILINE)
                .find(blockContent)?.groupValues?.get(1)
        if (quoted != null) return quoted
        return Regex("""^\s*${Regex.escape(key)}\s*=\s*(aws_\S+)""", RegexOption.MULTILINE)
            .find(blockContent)?.groupValues?.get(1)
    }

    private fun extractQuotedAttribute(
        blockContent: String,
        key: String,
    ): String? =
        Regex("""^\s*${Regex.escape(key)}\s*=\s*"([^"]+)"""", RegexOption.MULTILINE)
            .find(blockContent)?.groupValues?.get(1)

    private fun extractFilterPolicy(blockContent: String): String? {
        val match = filterPolicyPattern.find(blockContent) ?: return null
        val openBrace = blockContent.lastIndexOf('{', match.range.last)
        if (openBrace == -1) {
            return null
        }

        val innerContent = extractBlock(blockContent, openBrace + 1)
        return hclToJson(innerContent)
    }

    private fun hclToJson(content: String): String {
        val entries =
            content.lines()
                .map { it.trim().trimEnd(',') }
                .filter { it.isNotEmpty() && it.contains('=') }
                .map { line ->
                    val key = line.substringBefore('=').trim()
                    val value = line.substringAfter('=').trim()
                    "\"$key\":$value"
                }

        return "{${entries.joinToString(",")}}"
    }
}
