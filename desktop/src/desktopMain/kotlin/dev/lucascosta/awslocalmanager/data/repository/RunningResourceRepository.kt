package dev.lucascosta.awslocalmanager.data.repository

import dev.lucascosta.awslocalmanager.data.model.aws.RunningResource
import dev.lucascosta.awslocalmanager.data.model.process.ProcessConfig
import dev.lucascosta.awslocalmanager.data.model.resources.DynamoDbResource
import dev.lucascosta.awslocalmanager.data.model.resources.S3Resource
import dev.lucascosta.awslocalmanager.data.model.resources.SnsResource
import dev.lucascosta.awslocalmanager.data.model.resources.SqsResource
import dev.lucascosta.awslocalmanager.data.model.resources.StepFunctionsResource
import dev.lucascosta.awslocalmanager.data.remote.AwsDynamoDbClient
import dev.lucascosta.awslocalmanager.data.remote.AwsS3Client
import dev.lucascosta.awslocalmanager.data.remote.AwsSnsClient
import dev.lucascosta.awslocalmanager.data.remote.AwsSqsClient
import dev.lucascosta.awslocalmanager.data.remote.AwsStepFunctionsClient
import dev.lucascosta.awslocalmanager.data.remote.ProcessRunner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext

class RunningResourceRepository(
    private val sqsClientFactory: (String) -> AwsSqsClient,
    private val snsClientFactory: (String) -> AwsSnsClient,
    private val s3ClientFactory: (String) -> AwsS3Client,
    private val dynamoDbClientFactory: (String) -> AwsDynamoDbClient,
    private val stepFunctionsClientFactory: (String) -> AwsStepFunctionsClient,
) {
    suspend fun fetchAllRunningResources(
        endpoint: String,
        activeServices: Set<String>,
    ): List<RunningResource> =
        withContext(Dispatchers.IO) {
            val sqsJob = async { fetchSqsResources(endpoint, activeServices) }
            val snsJob = async { fetchSnsResources(endpoint, activeServices) }
            val s3Job = async { fetchS3Resources(endpoint, activeServices) }
            val dynamoJob = async { fetchDynamoDbResources(endpoint, activeServices) }
            val sfnJob = async { fetchStepFunctionsResources(endpoint, activeServices) }

            sqsJob.await() + snsJob.await() + s3Job.await() + dynamoJob.await() + sfnJob.await()
        }

    suspend fun deleteResources(
        resources: List<RunningResource>,
        endpoint: String,
    ): Map<RunningResource, Boolean> {
        val env = ProcessRunner.awsEnvVars(endpoint)
        val results = mutableMapOf<RunningResource, Boolean>()

        resources.forEach { resource ->
            val cmd = resource.type.deleteCommand(resource)

            if (cmd == null) {
                results[resource] = false
            } else {
                val success =
                    runCatching {
                        ProcessRunner.run(cmd, ProcessConfig(envVars = env)).getOrThrow().exitCode == 0
                    }.getOrElse { false }
                results[resource] = success
            }
        }

        return results
    }

    private suspend fun fetchSqsResources(
        endpoint: String,
        activeServices: Set<String>,
    ): List<RunningResource> =
        if ("sqs" !in activeServices) {
            emptyList()
        } else {
            sqsClientFactory(endpoint).listQueues().getOrElse { emptyList() }
                .map { url ->
                    RunningResource(
                        name = url.substringAfterLast("/"),
                        type = SqsResource,
                        arn = null,
                        url = url,
                        projectName = null,
                    )
                }
        }

    private suspend fun fetchSnsResources(
        endpoint: String,
        activeServices: Set<String>,
    ): List<RunningResource> =
        if ("sns" !in activeServices) {
            emptyList()
        } else {
            snsClientFactory(endpoint).listTopics().getOrElse { emptyList() }
                .map { arn ->
                    RunningResource(
                        name = arn.substringAfterLast(":"),
                        type = SnsResource,
                        arn = arn,
                        url = null,
                        projectName = null,
                    )
                }
        }

    private suspend fun fetchS3Resources(
        endpoint: String,
        activeServices: Set<String>,
    ): List<RunningResource> =
        if ("s3" !in activeServices) {
            emptyList()
        } else {
            s3ClientFactory(endpoint).listBuckets().getOrElse { emptyList() }
                .map { bucket ->
                    RunningResource(name = bucket, type = S3Resource, arn = null, url = null, projectName = null)
                }
        }

    private suspend fun fetchDynamoDbResources(
        endpoint: String,
        activeServices: Set<String>,
    ): List<RunningResource> =
        if ("dynamodb" !in activeServices) {
            emptyList()
        } else {
            dynamoDbClientFactory(endpoint).listTables().getOrElse { emptyList() }
                .map { table ->
                    RunningResource(name = table, type = DynamoDbResource, arn = null, url = null, projectName = null)
                }
        }

    private suspend fun fetchStepFunctionsResources(
        endpoint: String,
        activeServices: Set<String>,
    ): List<RunningResource> =
        if ("states" !in activeServices) {
            emptyList()
        } else {
            stepFunctionsClientFactory(endpoint).listStateMachines().getOrElse { emptyList() }
                .map { arn ->
                    RunningResource(
                        name = arn.substringAfterLast(":"),
                        type = StepFunctionsResource,
                        arn = arn,
                        url = null,
                        projectName = null,
                    )
                }
        }

}
