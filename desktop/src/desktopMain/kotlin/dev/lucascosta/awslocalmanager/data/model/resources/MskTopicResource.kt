package dev.lucascosta.awslocalmanager.data.model.resources

import dev.lucascosta.awslocalmanager.data.model.aws.AwsResourceDefinition
import dev.lucascosta.awslocalmanager.data.model.aws.RunningResource
import dev.lucascosta.awslocalmanager.data.model.aws.SuccessSnackbarKey
import dev.lucascosta.awslocalmanager.data.remote.KafkaBrokerCommands

object MskTopicResource : AwsResourceDefinition {
    const val CLUSTER_PROPERTY = "cluster"
    const val CLUSTER_REFERENCE_PROPERTY = "cluster_arn"
    const val PARTITIONS_PROPERTY = "partition_count"
    const val DEFAULT_PARTITIONS = 1
    private const val NAME_SEPARATOR = "/"

    override val id = "MSK_TOPIC"
    override val displayName = "MSK Topic"
    override val terraformPrefix = "aws_msk_topic"
    override val healthKey = "kafka"
    override val creationPriority = 4
    override val isQuickCreatable = true
    override val hasFilePublish = false
    override val publishableViaJson = true
    override val supportsPayloads = true
    override val successSnackbarKey = SuccessSnackbarKey.GENERIC

    fun qualifiedName(
        cluster: String,
        topic: String,
    ) = "$cluster$NAME_SEPARATOR$topic"

    fun clusterOf(qualifiedName: String) = qualifiedName.substringBefore(NAME_SEPARATOR)

    fun topicOf(qualifiedName: String) = qualifiedName.substringAfter(NAME_SEPARATOR)

    override fun createCommand(
        name: String,
        extraProperties: Map<String, String>,
    ): List<String>? = null

    override fun deleteCommand(resource: RunningResource): List<String>? =
        resource.url?.let { container -> KafkaBrokerCommands.deleteTopic(container, topicOf(resource.name)) }

    override fun buildArn(name: String): String? = null

    override fun terraformTemplate(label: String): String {
        val clusterLabel = "${label}_cluster"
        val topicTemplate =
            """
            resource "aws_msk_topic" "$label" {
              name               = "${label.replace("_", "-")}"
              cluster_arn        = aws_msk_cluster.$clusterLabel.arn
              partition_count    = 3
              replication_factor = 1
            }
            """.trimIndent()
        return MskClusterResource.terraformTemplate(clusterLabel) + "\n\n" + topicTemplate
    }

    override fun publishIdentifier(resource: RunningResource) = resource.name
}
