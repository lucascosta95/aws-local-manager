package dev.lucascosta.awslocalmanager.data.remote

import dev.lucascosta.awslocalmanager.data.model.process.ProcessConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AwsStepFunctionsClient(private val endpointUrl: String) {
    suspend fun listStateMachines(): Result<List<String>> =
        withContext(Dispatchers.IO) {
            runCatching {
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
                        config = ProcessConfig(envVars = ProcessRunner.awsEnvVars(endpointUrl)),
                    ).getOrThrow()
                if (result.exitCode != 0 || result.stdout.isBlank()) {
                    emptyList()
                } else {
                    result.stdout.trim().split("\t", "\n").filter { it.isNotBlank() }
                }
            }
        }

    suspend fun startExecution(
        stateMachineArn: String,
        input: String,
    ): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val result =
                    ProcessRunner.run(
                        command = AwsCommands.startExecution(stateMachineArn, input),
                        config = ProcessConfig(envVars = ProcessRunner.awsEnvVars(endpointUrl)),
                    ).getOrThrow()
                if (result.exitCode != 0) {
                    error(result.stderr.ifBlank { "start-execution failed (exit ${result.exitCode})" })
                }
                result.stdout.ifBlank { stateMachineArn }
            }
        }
}
