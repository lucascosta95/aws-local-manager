package dev.lucascosta.awslocalmanager.data.remote

object EmulatorDefaults {
    const val AWS_ACCOUNT_ID = "000000000000"
    const val AWS_REGION = "us-east-1"

    fun sqsArn(queueName: String) = "arn:aws:sqs:$AWS_REGION:$AWS_ACCOUNT_ID:$queueName"

    fun snsArn(topicName: String) = "arn:aws:sns:$AWS_REGION:$AWS_ACCOUNT_ID:$topicName"

    fun stepFunctionsArn(name: String) = "arn:aws:states:$AWS_REGION:$AWS_ACCOUNT_ID:stateMachine:$name"

    fun iamRoleArn(roleName: String) = "arn:aws:iam::$AWS_ACCOUNT_ID:role/$roleName"
}
