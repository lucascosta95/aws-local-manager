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
