package dev.lucascosta.awslocalmanager.domain

import dev.lucascosta.awslocalmanager.data.model.process.ProcessConfig
import dev.lucascosta.awslocalmanager.data.remote.GlueSchemaDefinition
import dev.lucascosta.awslocalmanager.data.remote.GlueSchemaRegistryCommands
import dev.lucascosta.awslocalmanager.data.remote.ProcessRunner

class GlueSchemaProvisioner {
    private companion object {
        const val ALREADY_EXISTS = "AlreadyExistsException"
    }

    // Registering the same definition again returns the existing version, so an unchanged schema is a no-op and an edited
    // one becomes a new version, which is what re-applying a Terraform file should mean.
    suspend fun apply(
        endpoint: String,
        definition: GlueSchemaDefinition,
    ): Result<Unit> =
        runCatching {
            val config = ProcessConfig(envVars = ProcessRunner.awsEnvVars(endpoint))
            val created = ProcessRunner.run(GlueSchemaRegistryCommands.createSchema(definition), config).getOrThrow()
            if (created.exitCode == 0) return@runCatching
            check(ALREADY_EXISTS in created.stderr) { created.stderr.ifBlank { "create-schema failed (exit ${created.exitCode})" } }

            val registered = ProcessRunner.run(GlueSchemaRegistryCommands.registerSchemaVersion(definition), config).getOrThrow()
            check(registered.exitCode == 0) { registered.stderr.ifBlank { "register-schema-version failed (exit ${registered.exitCode})" } }
        }
}
