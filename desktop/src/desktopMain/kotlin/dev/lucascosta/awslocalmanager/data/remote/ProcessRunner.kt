package dev.lucascosta.awslocalmanager.data.remote

import dev.lucascosta.awslocalmanager.constants.AppConstants.AWS_ACCESS_KEY_ID
import dev.lucascosta.awslocalmanager.constants.AppConstants.AWS_DEFAULT_REGION
import dev.lucascosta.awslocalmanager.constants.AppConstants.AWS_ENDPOINT_URL
import dev.lucascosta.awslocalmanager.constants.AppConstants.AWS_SECRET_ACCESS_KEY
import dev.lucascosta.awslocalmanager.data.model.process.ProcessConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.concurrent.TimeUnit

object ProcessRunner {
    fun awsEnvVars(endpoint: String): Map<String, String> =
        mapOf(
            AWS_ACCESS_KEY_ID to EmulatorConfig.ACCESS_KEY,
            AWS_SECRET_ACCESS_KEY to EmulatorConfig.SECRET_KEY,
            AWS_DEFAULT_REGION to EmulatorDefaults.AWS_REGION,
            AWS_ENDPOINT_URL to endpoint,
        )

    suspend fun run(
        command: List<String>,
        config: ProcessConfig = ProcessConfig(),
    ): Result<ProcessOutput> =
        withContext(Dispatchers.IO) {
            runCatching {
                val process =
                    ProcessBuilder(command)
                        .also { builder ->
                            if (config.workingDir != null) {
                                builder.directory(config.workingDir)
                            }
                            if (config.envVars.isNotEmpty()) {
                                builder.environment().putAll(config.envVars)
                            }
                        }
                        .start()

                val stdout: String
                val stderr: String

                coroutineScope {
                    val stdoutJob = async { process.inputStream.bufferedReader().readText() }
                    val stderrJob = async { process.errorStream.bufferedReader().readText() }
                    stdout = stdoutJob.await()
                    stderr = stderrJob.await()
                }

                val completed = process.waitFor(config.timeoutSeconds, TimeUnit.SECONDS)
                if (!completed) {
                    process.destroyForcibly()
                    error("Process timed out after ${config.timeoutSeconds}s: ${command.firstOrNull()}")
                }
                ProcessOutput(stdout = stdout.trim(), stderr = stderr.trim(), exitCode = process.exitValue())
            }
        }

    fun runStreaming(
        command: List<String>,
        config: ProcessConfig = ProcessConfig(),
    ): Flow<ProcessLine> =
        flow {
            val process =
                try {
                    ProcessBuilder(command)
                        .redirectErrorStream(true)
                        .also { builder ->
                            if (config.workingDir != null) {
                                builder.directory(config.workingDir)
                            }

                            if (config.envVars.isNotEmpty()) {
                                builder.environment().putAll(config.envVars)
                            }
                        }
                        .start()
                } catch (e: IOException) {
                    emit(ProcessLine("Error starting process: ${e.message}", isError = true))
                    return@flow
                }

            process.inputStream.bufferedReader().useLines { lines ->
                lines.forEach { line -> emit(ProcessLine(line, isError = false)) }
            }

            val completed = process.waitFor(config.timeoutSeconds, TimeUnit.SECONDS)
            if (!completed) {
                process.destroyForcibly()
            }
        }.flowOn(Dispatchers.IO)
}
