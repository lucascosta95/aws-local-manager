package dev.lucascosta.awslocalmanager.data.remote

import dev.lucascosta.awslocalmanager.constants.AppConstants.SNS_RAW_MESSAGE_DELIVERY_ATTR

object AwsCommands {
    fun createSqs(
        queueName: String,
        attributes: String? = null,
    ): List<String> =
        buildList {
            addAll(listOf("aws", "sqs", "create-queue", "--queue-name", queueName, "--output", "text"))
            if (attributes != null) addAll(listOf("--attributes", attributes))
        }

    fun deleteSqs(queueUrl: String): List<String> = listOf("aws", "sqs", "delete-queue", "--queue-url", queueUrl)

    fun createSns(topicName: String): List<String> = listOf("aws", "sns", "create-topic", "--name", topicName, "--output", "text")

    fun deleteSns(topicArn: String): List<String> = listOf("aws", "sns", "delete-topic", "--topic-arn", topicArn)

    fun createS3(bucketName: String): List<String> = listOf("aws", "s3", "mb", "s3://$bucketName")

    fun deleteS3(bucketName: String): List<String> = listOf("aws", "s3", "rb", "s3://$bucketName", "--force")

    fun createDynamoDb(
        tableName: String,
        partitionKey: String = "id",
        keyType: String = "S",
    ): List<String> =
        listOf(
            "aws",
            "dynamodb",
            "create-table",
            "--table-name",
            tableName,
            "--attribute-definitions",
            "AttributeName=$partitionKey,AttributeType=$keyType",
            "--key-schema",
            "AttributeName=$partitionKey,KeyType=HASH",
            "--billing-mode",
            "PAY_PER_REQUEST",
            "--output",
            "text",
        )

    fun deleteDynamoDb(tableName: String): List<String> =
        listOf("aws", "dynamodb", "delete-table", "--table-name", tableName, "--output", "text")

    fun createStateMachine(name: String): List<String> =
        listOf(
            "aws",
            "stepfunctions",
            "create-state-machine",
            "--name",
            name,
            "--definition",
            """{"Comment":"local","StartAt":"Pass","States":{"Pass":{"Type":"Pass","End":true}}}""",
            "--role-arn",
            "arn:aws:iam::000000000000:role/local",
            "--type",
            "STANDARD",
            "--output",
            "text",
        )

    fun startExecution(
        stateMachineArn: String,
        input: String,
    ): List<String> =
        listOf(
            "aws",
            "stepfunctions",
            "start-execution",
            "--state-machine-arn",
            stateMachineArn,
            "--input",
            input,
            "--output",
            "text",
        )

    fun deleteStepFunctions(stateMachineArn: String): List<String> =
        listOf("aws", "stepfunctions", "delete-state-machine", "--state-machine-arn", stateMachineArn)

    fun subscribeSns(
        topicArn: String,
        protocol: String,
        endpointArn: String,
        rawMessageDelivery: Boolean,
    ): List<String> =
        buildList {
            add("aws")
            add("sns")
            add("subscribe")
            add("--topic-arn")
            add(topicArn)
            add("--protocol")
            add(protocol)
            add("--notification-endpoint")
            add(endpointArn)
            if (rawMessageDelivery) {
                add("--attributes")
                add("""{"$SNS_RAW_MESSAGE_DELIVERY_ATTR":"true"}""")
            }
        }

    fun setSubscriptionAttribute(
        subscriptionArn: String,
        attributeName: String,
        value: String,
    ): List<String> =
        listOf(
            "aws",
            "sns",
            "set-subscription-attributes",
            "--subscription-arn",
            subscriptionArn,
            "--attribute-name",
            attributeName,
            "--attribute-value",
            value,
        )
}

object ElastiCacheCommands {
    fun createElastiCacheCluster(
        clusterId: String,
        nodeType: String,
        numNodes: String,
    ): List<String> =
        listOf(
            "aws",
            "elasticache",
            "create-cache-cluster",
            "--cache-cluster-id",
            clusterId,
            "--engine",
            "memcached",
            "--cache-node-type",
            nodeType,
            "--num-cache-nodes",
            numNodes,
            "--output",
            "text",
        )

    fun createElastiCacheReplicationGroup(
        groupId: String,
        nodeType: String,
    ): List<String> =
        listOf(
            "aws",
            "elasticache",
            "create-replication-group",
            "--replication-group-id",
            groupId,
            "--replication-group-description",
            groupId,
            "--cache-node-type",
            nodeType,
            "--num-cache-clusters",
            "1",
            "--output",
            "text",
        )

    fun deleteElastiCacheCluster(clusterId: String): List<String> =
        listOf("aws", "elasticache", "delete-cache-cluster", "--cache-cluster-id", clusterId)

    fun deleteElastiCacheReplicationGroup(groupId: String): List<String> =
        listOf("aws", "elasticache", "delete-replication-group", "--replication-group-id", groupId)
}

object SsmCommands {
    fun putParameter(
        name: String,
        value: String,
        type: String,
    ): List<String> =
        listOf(
            "aws",
            "ssm",
            "put-parameter",
            "--name",
            name,
            "--value",
            value,
            "--type",
            type,
            "--overwrite",
            "--output",
            "text",
        )

    fun deleteParameter(name: String): List<String> = listOf("aws", "ssm", "delete-parameter", "--name", name)
}

object MskCommands {
    private const val PLACEHOLDER_SUBNET = "subnet-local"

    fun createCluster(
        name: String,
        kafkaVersion: String,
        brokerNodes: String,
        instanceType: String,
    ): List<String> =
        listOf(
            "aws",
            "kafka",
            "create-cluster",
            "--cluster-name",
            name,
            "--kafka-version",
            kafkaVersion,
            "--number-of-broker-nodes",
            brokerNodes,
            "--broker-node-group-info",
            """{"InstanceType":"$instanceType","ClientSubnets":["$PLACEHOLDER_SUBNET"]}""",
            "--output",
            "text",
        )

    fun deleteCluster(clusterArn: String): List<String> = listOf("aws", "kafka", "delete-cluster", "--cluster-arn", clusterArn)
}

object KafkaBrokerCommands {
    fun deleteTopic(
        container: String,
        topic: String,
    ): List<String> = rpk(container, listOf("topic", "delete", topic))

    fun rpk(
        container: String,
        arguments: List<String>,
    ): List<String> = listOf("docker", "exec", "-i", container, "rpk") + arguments
}

object GlueSchemaRegistryCommands {
    fun createRegistry(name: String): List<String> = listOf("aws", "glue", "create-registry", "--registry-name", name, "--output", "text")

    fun deleteRegistry(name: String): List<String> = listOf("aws", "glue", "delete-registry", "--registry-id", "RegistryName=$name")

    fun createSchema(definition: GlueSchemaDefinition): List<String> =
        listOf(
            "aws",
            "glue",
            "create-schema",
            "--registry-id",
            "RegistryName=${definition.registry}",
            "--schema-name",
            definition.schema,
            "--data-format",
            definition.dataFormat,
            "--compatibility",
            definition.compatibility,
            "--schema-definition",
            definition.definition,
            "--output",
            "json",
        )

    fun registerSchemaVersion(definition: GlueSchemaDefinition): List<String> =
        listOf(
            "aws",
            "glue",
            "register-schema-version",
            "--schema-id",
            schemaId(definition.registry, definition.schema),
            "--schema-definition",
            definition.definition,
            "--output",
            "json",
        )

    fun deleteSchema(
        registry: String,
        schema: String,
    ): List<String> = listOf("aws", "glue", "delete-schema", "--schema-id", schemaId(registry, schema))

    fun schemaId(
        registry: String,
        schema: String,
    ) = "RegistryName=$registry,SchemaName=$schema"
}

data class GlueSchemaDefinition(
    val registry: String,
    val schema: String,
    val dataFormat: String,
    val compatibility: String,
    val definition: String,
)
