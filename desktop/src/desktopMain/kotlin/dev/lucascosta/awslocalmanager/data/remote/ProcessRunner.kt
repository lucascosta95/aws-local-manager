package dev.lucascosta.awslocalmanager.data.remote

import dev.lucascosta.awslocalmanager.constants.AppConstants.AWS_ACCESS_KEY_ID
import dev.lucascosta.awslocalmanager.constants.AppConstants.AWS_DEFAULT_REGION
import dev.lucascosta.awslocalmanager.constants.AppConstants.AWS_ENDPOINT_URL
import dev.lucascosta.awslocalmanager.constants.AppConstants.AWS_SECRET_ACCESS_KEY
import dev.lucascosta.awslocalmanager.data.model.process.ProcessConfig
import dev.lucascosta.awslocalmanager.domain.AppLogger
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
    private const val LOG_SOURCE = "ProcessRunner"
    private const val PATH_VARIABLE = "PATH"

    private fun ProcessBuilder.withResolvedEnvironment(): ProcessBuilder =
        apply { environment()[PATH_VARIABLE] = CommandLocator.searchPath() }

    private fun locate(command: List<String>): List<String> =
        if (command.isEmpty()) command else listOf(CommandLocator.resolve(command.first())) + command.drop(1)

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
                    ProcessBuilder(locate(command))
                        .withResolvedEnvironment()
                        .also { builder ->
                            if (config.workingDir != null) {
                                builder.directory(config.workingDir)
                            }
                            if (config.envVars.isNotEmpty()) {
                                builder.environment().putAll(config.envVars)
                            }
                        }
                        .start()

                process.outputStream.bufferedWriter().use { writer -> config.stdin?.let(writer::write) }

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
            }.onSuccess { output -> logOutcome(command, output) }
                .onFailure { failure -> AppLogger.error(LOG_SOURCE, "${command.joinToString(" ")} could not run", failure) }
        }

    private fun logOutcome(
        command: List<String>,
        output: ProcessOutput,
    ) {
        val line = command.joinToString(" ")
        if (output.exitCode == 0) {
            AppLogger.debug(LOG_SOURCE, line)
        } else {
            val reason = output.stderr.ifBlank { output.stdout }.lineSequence().firstOrNull().orEmpty()
            AppLogger.warn(LOG_SOURCE, "$line exited ${output.exitCode}${if (reason.isBlank()) "" else ": $reason"}")
        }
    }

    fun runStreaming(
        command: List<String>,
        config: ProcessConfig = ProcessConfig(),
    ): Flow<ProcessLine> =
        flow {
            val process =
                try {
                    ProcessBuilder(locate(command))
                        .withResolvedEnvironment()
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
                    AppLogger.error(LOG_SOURCE, "${command.joinToString(" ")} could not start", e)
                    emit(ProcessLine("Error starting process: ${e.message}", isError = true))
                    return@flow
                }

            process.inputStream.bufferedReader().useLines { lines ->
                lines.forEach { line -> emit(ProcessLine(line, isError = false)) }
            }

            val completed = process.waitFor(config.timeoutSeconds, TimeUnit.SECONDS)
            if (!completed) {
                AppLogger.error(LOG_SOURCE, "${command.joinToString(" ")} timed out after ${config.timeoutSeconds}s")
                process.destroyForcibly()
            } else {
                AppLogger.debug(LOG_SOURCE, "${command.joinToString(" ")} exited ${process.exitValue()}")
            }
        }.flowOn(Dispatchers.IO)
}
