package dev.lucascosta.awslocalmanager.features.inspector.handler

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.ui.graphics.vector.ImageVector
import dev.lucascosta.awslocalmanager.data.model.inspector.InspectorDetail
import dev.lucascosta.awslocalmanager.data.model.inspector.InspectorResource
import dev.lucascosta.awslocalmanager.data.model.inspector.SfnInspectorExecution
import dev.lucascosta.awslocalmanager.data.model.process.ProcessConfig
import dev.lucascosta.awslocalmanager.data.remote.ProcessRunner
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class StepFunctionsInspectorHandler : InspectorServiceHandler {
    override val serviceKey: String = "states"
    override val displayName: String = "Step Functions"
    override val icon: ImageVector = Icons.Outlined.AccountTree

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun loadResources(endpoint: String): List<InspectorResource> {
        val result =
            ProcessRunner.run(
                command =
                    listOf(
                        "aws",
                        "stepfunctions",
                        "list-state-machines",
                        "--query",
                        "stateMachines[].stateMachineArn",
                        "--output",
                        "text",
                    ),
                config = ProcessConfig(envVars = ProcessRunner.awsEnvVars(endpoint)),
            ).getOrElse { return emptyList() }

        if (result.exitCode != 0 || result.stdout.isBlank()) return emptyList()

        return result.stdout.trim().split("\t", "\n").filter { it.isNotBlank() }.map { arn ->
            InspectorResource(
                id = arn,
                name = arn.substringAfterLast(":"),
                summaryType = "sfn",
            )
        }
    }

    override suspend fun loadDetail(
        endpoint: String,
        resource: InspectorResource,
    ): InspectorDetail {
        val result =
            ProcessRunner.run(
                command =
                    listOf(
                        "aws",
                        "stepfunctions",
                        "list-executions",
                        "--state-machine-arn",
                        resource.id,
                        "--output",
                        "json",
                    ),
                config = ProcessConfig(envVars = ProcessRunner.awsEnvVars(endpoint)),
            ).getOrElse { return emptyStepFunctionsDetail() }

        if (result.exitCode != 0) return emptyStepFunctionsDetail()

        val executions =
            runCatching {
                json.decodeFromString<SfnListExecutionsResponse>(result.stdout).executions
            }.getOrElse { emptyList() }

        val sfnExecutions =
            executions
                .sortedByDescending { it.parsedStartDate() }
                .map { it.toModel() }

        val statusCounts = sfnExecutions.groupingBy { it.status }.eachCount()

        return InspectorDetail.StepFunctionsDetail(
            executions = sfnExecutions,
            statusCounts = statusCounts,
        )
    }

    override suspend fun loadSubDetail(
        endpoint: String,
        resource: InspectorResource,
        currentDetail: InspectorDetail,
        subItemId: String,
    ): InspectorDetail? {
        val sfnDetail = currentDetail as? InspectorDetail.StepFunctionsDetail ?: return null
        val execution = sfnDetail.executions.find { it.executionArn == subItemId } ?: return null

        val result =
            ProcessRunner.run(
                command =
                    listOf(
                        "aws",
                        "stepfunctions",
                        "describe-execution",
                        "--execution-arn",
                        subItemId,
                        "--output",
                        "json",
                    ),
                config = ProcessConfig(envVars = ProcessRunner.awsEnvVars(endpoint)),
            ).getOrElse {
                return sfnDetail.copy(selectedExecution = execution)
            }

        if (result.exitCode != 0) return sfnDetail.copy(selectedExecution = execution)

        val description =
            runCatching {
                json.decodeFromString<SfnDescribeExecutionResponse>(result.stdout)
            }.getOrElse {
                return sfnDetail.copy(selectedExecution = execution)
            }

        return sfnDetail.copy(
            selectedExecution = execution,
            executionInput = description.input,
            executionOutput = description.output,
        )
    }

    private fun emptyStepFunctionsDetail() =
        InspectorDetail.StepFunctionsDetail(
            executions = emptyList(),
            statusCounts = emptyMap(),
        )

    @Serializable
    private data class SfnListExecutionsResponse(
        val executions: List<SfnExecutionDto> = emptyList(),
    )

    @Serializable
    private data class SfnExecutionDto(
        val executionArn: String,
        val name: String,
        val status: String,
        val startDate: String = "",
        val stopDate: String? = null,
        val stateMachineArn: String = "",
    ) {
        fun parsedStartDate(): java.time.Instant =
            runCatching {
                java.time.OffsetDateTime.parse(startDate).toInstant()
            }.getOrElse { java.time.Instant.EPOCH }

        fun toModel() =
            SfnInspectorExecution(
                executionArn = executionArn,
                name = name,
                status = status,
                startDate = startDate,
                stopDate = stopDate,
            )
    }

    @Serializable
    private data class SfnDescribeExecutionResponse(
        val executionArn: String = "",
        val name: String = "",
        val status: String = "",
        val input: String? = null,
        val output: String? = null,
        val startDate: String = "",
        val stopDate: String? = null,
    )
}
