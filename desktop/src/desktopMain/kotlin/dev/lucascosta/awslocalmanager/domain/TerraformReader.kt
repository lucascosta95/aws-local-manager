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
import dev.lucascosta.awslocalmanager.data.model.resources.GlueRegistryResource
import dev.lucascosta.awslocalmanager.data.model.resources.GlueSchemaDataFormat
import dev.lucascosta.awslocalmanager.data.model.resources.GlueSchemaResource
import dev.lucascosta.awslocalmanager.data.model.resources.MskClusterResource
import dev.lucascosta.awslocalmanager.data.model.resources.MskTopicResource
import dev.lucascosta.awslocalmanager.data.model.resources.SsmParameterResource
import dev.lucascosta.awslocalmanager.data.model.resources.SsmParameterType
import kotlinx.serialization.json.Json
import java.io.File

class TerraformReader {
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private const val LOG_SOURCE = "TerraformReader"
        private val resourcePattern = Regex("""resource\s+"(aws_\w+)"\s+"(\w+)"\s*\{""")
        private val namePattern = Regex("""^\s*name\s*=\s*"([^"]+)"""", RegexOption.MULTILINE)
        private val snsSubscriptionPattern = Regex("""resource\s+"aws_sns_topic_subscription"\s+"(\w+)"\s*\{""")
        private val rawDeliveryPattern = Regex("""raw_message_delivery\s*=\s*true""")
        private val filterPolicyPattern = Regex("""filter_policy\s*=\s*jsonencode\s*\(\s*\{""")
        private val parentLinks =
            listOf(
                ParentLink(
                    childPrefix = MskTopicResource.terraformPrefix,
                    parentPrefix = MskClusterResource.terraformPrefix,
                    referenceProperty = MskTopicResource.CLUSTER_REFERENCE_PROPERTY,
                    parentProperty = MskTopicResource.CLUSTER_PROPERTY,
                    arnPattern = Regex("""^arn:aws:kafka:[^:]*:[^:]*:cluster/([^/]+)/"""),
                    defaultParent = null,
                    qualify = MskTopicResource::qualifiedName,
                ),
                ParentLink(
                    childPrefix = GlueSchemaResource.terraformPrefix,
                    parentPrefix = GlueRegistryResource.terraformPrefix,
                    referenceProperty = GlueSchemaResource.REGISTRY_REFERENCE_PROPERTY,
                    parentProperty = GlueSchemaResource.REGISTRY_PROPERTY,
                    arnPattern = Regex("""^arn:aws:glue:[^:]*:[^:]*:registry/([^/]+)$"""),
                    defaultParent = GlueRegistryResource.DEFAULT_REGISTRY,
                    qualify = GlueSchemaResource::qualifiedName,
                ),
            )
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

        return resolveParents(tfFiles.flatMap { parseResourcesFromFile(it) }).sortedBy { it.tfLabel }
    }

    private fun parseProjectDirectory(dir: File): InfraProject? {
        val infraDir = File(dir, PROJECT_INFRA_SUBDIR)
        val configFile = File(infraDir, AWS_LOCAL_CONFIG_FILENAME)
        if (!configFile.exists()) {
            return null
        }

        val config =
            runCatching { json.decodeFromString<ProjectConfig>(configFile.readText()) }
                .onFailure { AppLogger.warn(LOG_SOURCE, "Failed to parse config ${configFile.path}", it) }
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
        return resourcePattern.findAll(content).mapNotNull { match ->
            val awsPrefix = match.groupValues[1]
            val tfLabel = match.groupValues[2]
            val blockContent = extractBlock(content, match.range.last + 1)
            val (awsName, extraProperties) = parseAttributes(awsPrefix, tfLabel, blockContent, file.parentFile) ?: return@mapNotNull null
            TerraformResource(
                tfLabel = tfLabel,
                awsName = awsName,
                resourceType = ResourceRegistry.fromTerraformPrefix(awsPrefix),
                rawAwsType = awsPrefix,
                filePath = file.absolutePath,
                extraProperties = extraProperties,
            )
        }.toList()
    }

    private fun parseAttributes(
        awsPrefix: String,
        tfLabel: String,
        blockContent: String,
        baseDir: File,
    ): Pair<String, Map<String, String>>? =
        when (awsPrefix) {
            "aws_elasticache_cluster" -> parseElastiCacheAttributes(tfLabel, blockContent)
            "aws_ssm_parameter" -> parseSsmParameterAttributes(blockContent)
            "aws_msk_cluster" -> parseMskClusterAttributes(tfLabel, blockContent)
            "aws_msk_topic" -> parseMskTopicAttributes(blockContent)
            "aws_glue_registry" -> parseGlueRegistryAttributes(tfLabel, blockContent)
            "aws_glue_schema" -> parseGlueSchemaAttributes(blockContent, baseDir)
            else -> (namePattern.find(blockContent)?.groupValues?.get(1) ?: tfLabel.replace("_", "-")) to emptyMap()
        }

    private fun parseElastiCacheAttributes(
        tfLabel: String,
        blockContent: String,
    ): Pair<String, Map<String, String>> {
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
        return clusterId to mapOf("engine" to engine, "node_type" to nodeType, "num_cache_nodes" to numNodes, "port" to port)
    }

    private fun parseSsmParameterAttributes(blockContent: String): Pair<String, Map<String, String>>? {
        val name = namePattern.find(blockContent)?.groupValues?.get(1) ?: return null
        val value = extractQuotedAttribute(blockContent, SsmParameterResource.VALUE_PROPERTY).orEmpty()
        val type =
            extractQuotedAttribute(blockContent, SsmParameterResource.TYPE_PROPERTY)
                ?.let { SsmParameterType.fromCliValue(it).cliValue }
                ?: SsmParameterType.STRING.cliValue
        return name to
            mapOf(
                SsmParameterResource.VALUE_PROPERTY to value,
                SsmParameterResource.TYPE_PROPERTY to type,
            )
    }

    private fun parseMskClusterAttributes(
        tfLabel: String,
        blockContent: String,
    ): Pair<String, Map<String, String>> {
        val clusterName = extractQuotedAttribute(blockContent, "cluster_name") ?: tfLabel.replace("_", "-")
        val kafkaVersion = MskClusterResource.KAFKA_VERSION_PROPERTY
        val brokerNodes = MskClusterResource.BROKER_NODES_PROPERTY
        val instanceType = MskClusterResource.INSTANCE_TYPE_PROPERTY
        return clusterName to
            mapOf(
                kafkaVersion to (extractQuotedAttribute(blockContent, kafkaVersion) ?: MskClusterResource.DEFAULT_KAFKA_VERSION),
                brokerNodes to (extractNumericAttribute(blockContent, brokerNodes) ?: MskClusterResource.DEFAULT_BROKER_NODES),
                instanceType to (extractQuotedAttribute(blockContent, instanceType) ?: MskClusterResource.DEFAULT_INSTANCE_TYPE),
            )
    }

    private fun parseMskTopicAttributes(blockContent: String): Pair<String, Map<String, String>>? {
        val topicName = namePattern.find(blockContent)?.groupValues?.get(1) ?: return null
        val clusterReference = extractAttributeValue(blockContent, MskTopicResource.CLUSTER_REFERENCE_PROPERTY) ?: return null
        val partitions =
            extractNumericAttribute(blockContent, MskTopicResource.PARTITIONS_PROPERTY)
                ?: MskTopicResource.DEFAULT_PARTITIONS.toString()
        return topicName to
            mapOf(
                MskTopicResource.CLUSTER_REFERENCE_PROPERTY to clusterReference,
                MskTopicResource.PARTITIONS_PROPERTY to partitions,
            )
    }

    private fun parseGlueRegistryAttributes(
        tfLabel: String,
        blockContent: String,
    ): Pair<String, Map<String, String>> =
        (extractQuotedAttribute(blockContent, "registry_name") ?: tfLabel.replace("_", "-")) to emptyMap()

    private fun parseGlueSchemaAttributes(
        blockContent: String,
        baseDir: File,
    ): Pair<String, Map<String, String>>? {
        val schemaName = extractQuotedAttribute(blockContent, "schema_name") ?: return null
        val definition = HclStringReader.read(blockContent, GlueSchemaResource.DEFINITION_PROPERTY, baseDir) ?: return null
        val dataFormat =
            extractQuotedAttribute(blockContent, GlueSchemaResource.DATA_FORMAT_PROPERTY)
                ?.let { GlueSchemaDataFormat.fromCliValue(it) } ?: return null
        val properties =
            buildMap {
                put(GlueSchemaResource.DEFINITION_PROPERTY, definition)
                put(GlueSchemaResource.DATA_FORMAT_PROPERTY, dataFormat.name)
                extractQuotedAttribute(blockContent, GlueSchemaResource.COMPATIBILITY_PROPERTY)
                    ?.let { put(GlueSchemaResource.COMPATIBILITY_PROPERTY, it) }
                extractAttributeValue(blockContent, GlueSchemaResource.REGISTRY_REFERENCE_PROPERTY)
                    ?.let { put(GlueSchemaResource.REGISTRY_REFERENCE_PROPERTY, it) }
            }
        return schemaName to properties
    }

    // Topics and schemas point at their parent by Terraform reference, possibly across files, so parents are resolved last.
    private fun resolveParents(resources: List<TerraformResource>): List<TerraformResource> =
        parentLinks.fold(resources) { current, link -> resolveParent(current, link) }

    private fun resolveParent(
        resources: List<TerraformResource>,
        link: ParentLink,
    ): List<TerraformResource> {
        val parentNamesByLabel =
            resources
                .filter { it.rawAwsType == link.parentPrefix }
                .associate { it.tfLabel to it.awsName }

        return resources.mapNotNull { resource ->
            if (resource.rawAwsType != link.childPrefix) return@mapNotNull resource
            val reference = resource.extraProperties[link.referenceProperty]
            val parentName =
                when (reference) {
                    null -> link.defaultParent
                    else -> resolveParentName(reference, link, parentNamesByLabel)
                } ?: return@mapNotNull null
            resource.copy(
                awsName = link.qualify(parentName, resource.awsName),
                extraProperties = resource.extraProperties + (link.parentProperty to parentName),
            )
        }
    }

    private fun resolveParentName(
        reference: String,
        link: ParentLink,
        parentNamesByLabel: Map<String, String>,
    ): String? {
        val label = Regex("""^${link.parentPrefix}\.(\w+)\.arn$""").find(reference)?.groupValues?.get(1)
        if (label != null) return parentNamesByLabel[label]
        return link.arnPattern.find(reference)?.groupValues?.get(1)
    }

    private data class ParentLink(
        val childPrefix: String,
        val parentPrefix: String,
        val referenceProperty: String,
        val parentProperty: String,
        val arnPattern: Regex,
        val defaultParent: String?,
        val qualify: (parent: String, child: String) -> String,
    )

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

    private fun extractNumericAttribute(
        blockContent: String,
        key: String,
    ): String? =
        Regex("""^\s*${Regex.escape(key)}\s*=\s*"?(\d+)"?\s*$""", RegexOption.MULTILINE)
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
