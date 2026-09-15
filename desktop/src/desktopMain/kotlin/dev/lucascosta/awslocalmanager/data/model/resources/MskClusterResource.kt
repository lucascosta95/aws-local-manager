package dev.lucascosta.awslocalmanager.data.model.resources

import dev.lucascosta.awslocalmanager.data.model.aws.AwsResourceDefinition
import dev.lucascosta.awslocalmanager.data.model.aws.RunningResource
import dev.lucascosta.awslocalmanager.data.model.aws.SuccessSnackbarKey
import dev.lucascosta.awslocalmanager.data.remote.MskCommands

object MskClusterResource : AwsResourceDefinition {
    const val KAFKA_VERSION_PROPERTY = "kafka_version"
    const val BROKER_NODES_PROPERTY = "number_of_broker_nodes"
    const val INSTANCE_TYPE_PROPERTY = "instance_type"
    const val DEFAULT_KAFKA_VERSION = "3.6.0"
    const val DEFAULT_BROKER_NODES = "1"
    const val DEFAULT_INSTANCE_TYPE = "kafka.t3.small"

    // Floci pulls the broker image inside the create-cluster call, which outlasts the default timeout on a first run.
    private const val CREATE_TIMEOUT_SECONDS = 600L

    override val id = "MSK"
    override val displayName = "MSK"
    override val terraformPrefix = "aws_msk_cluster"
    override val healthKey = "kafka"
    override val creationPriority = 2
    override val isQuickCreatable = true
    override val hasFilePublish = false
    override val publishableViaJson = false
    override val supportsPayloads = false
    override val successSnackbarKey = SuccessSnackbarKey.GENERIC
    override val createTimeoutSeconds = CREATE_TIMEOUT_SECONDS

    override fun createCommand(
        name: String,
        extraProperties: Map<String, String>,
    ): List<String> =
        MskCommands.createCluster(
            name = name,
            kafkaVersion = extraProperties[KAFKA_VERSION_PROPERTY] ?: DEFAULT_KAFKA_VERSION,
            brokerNodes = extraProperties[BROKER_NODES_PROPERTY] ?: DEFAULT_BROKER_NODES,
            instanceType = extraProperties[INSTANCE_TYPE_PROPERTY] ?: DEFAULT_INSTANCE_TYPE,
        )

    override fun deleteCommand(resource: RunningResource): List<String>? = resource.arn?.let(MskCommands::deleteCluster)

    override fun buildArn(name: String): String? = null

    override fun terraformTemplate(label: String) =
        """
        resource "aws_msk_cluster" "$label" {
          cluster_name           = "${label.replace("_", "-")}"
          kafka_version          = "$DEFAULT_KAFKA_VERSION"
          number_of_broker_nodes = $DEFAULT_BROKER_NODES

          broker_node_group_info {
            instance_type  = "$DEFAULT_INSTANCE_TYPE"
            client_subnets = ["subnet-local"]
          }
        }
        """.trimIndent()

    override fun publishIdentifier(resource: RunningResource) = resource.name
}
