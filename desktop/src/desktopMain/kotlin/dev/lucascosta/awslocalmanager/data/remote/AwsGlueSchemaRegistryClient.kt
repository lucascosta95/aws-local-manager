package dev.lucascosta.awslocalmanager.data.remote

import dev.lucascosta.awslocalmanager.data.model.process.ProcessConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

data class GlueSchemaInfo(
    val registry: String,
    val name: String,
    val dataFormat: String,
    val compatibility: String,
    val latestVersion: Long,
)

data class GlueSchemaVersionInfo(
    val versionNumber: Long,
    val versionId: String,
    val status: String,
    val createdTime: String,
)

class AwsGlueSchemaRegistryClient(private val endpointUrl: String) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun listRegistries(): Result<List<String>> =
        runGlue(listOf("list-registries")).mapCatching { stdout ->
            json.decodeFromString<ListRegistriesResponse>(stdout).registries.map { it.name }
        }

    suspend fun listSchemaNames(registry: String): Result<List<String>> =
        runGlue(listOf("list-schemas", "--registry-id", "RegistryName=$registry")).mapCatching { stdout ->
            json.decodeFromString<ListSchemasResponse>(stdout).schemas.map { it.name }
        }

    suspend fun getSchema(
        registry: String,
        schema: String,
    ): Result<GlueSchemaInfo> =
        runGlue(listOf("get-schema", "--schema-id", GlueSchemaRegistryCommands.schemaId(registry, schema))).mapCatching { stdout ->
            val dto = json.decodeFromString<SchemaDto>(stdout)
            GlueSchemaInfo(registry, schema, dto.dataFormat, dto.compatibility, dto.latestVersion)
        }

    suspend fun listSchemaVersions(
        registry: String,
        schema: String,
    ): Result<List<GlueSchemaVersionInfo>> =
        runGlue(listOf("list-schema-versions", "--schema-id", GlueSchemaRegistryCommands.schemaId(registry, schema))).mapCatching {
                stdout ->
            json.decodeFromString<ListSchemaVersionsResponse>(stdout).versions
                .map { GlueSchemaVersionInfo(it.versionNumber, it.versionId, it.status, it.createdTime) }
                .sortedByDescending { it.versionNumber }
        }

    suspend fun getSchemaDefinition(
        registry: String,
        schema: String,
        versionNumber: Long,
    ): Result<String> {
        val arguments =
            listOf(
                "get-schema-version",
                "--schema-id",
                GlueSchemaRegistryCommands.schemaId(registry, schema),
                "--schema-version-number",
                "VersionNumber=$versionNumber",
            )
        return runGlue(arguments).mapCatching { stdout -> json.decodeFromString<SchemaVersionDto>(stdout).definition }
    }

    private suspend fun runGlue(arguments: List<String>): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val result =
                    ProcessRunner.run(
                        command = listOf("aws", "glue") + arguments + listOf("--output", "json"),
                        config = ProcessConfig(envVars = ProcessRunner.awsEnvVars(endpointUrl)),
                    ).getOrThrow()
                if (result.exitCode != 0) {
                    error(result.stderr.ifBlank { "glue ${arguments.first()} failed (exit ${result.exitCode})" })
                }
                result.stdout
            }
        }

    @Serializable
    private data class ListRegistriesResponse(
        @SerialName("Registries") val registries: List<RegistryDto> = emptyList(),
    )

    @Serializable
    private data class RegistryDto(
        @SerialName("RegistryName") val name: String,
    )

    @Serializable
    private data class ListSchemasResponse(
        @SerialName("Schemas") val schemas: List<SchemaNameDto> = emptyList(),
    )

    @Serializable
    private data class SchemaNameDto(
        @SerialName("SchemaName") val name: String,
    )

    @Serializable
    private data class SchemaDto(
        @SerialName("DataFormat") val dataFormat: String = "",
        @SerialName("Compatibility") val compatibility: String = "",
        @SerialName("LatestSchemaVersion") val latestVersion: Long = 0,
    )

    @Serializable
    private data class ListSchemaVersionsResponse(
        @SerialName("Schemas") val versions: List<VersionDto> = emptyList(),
    )

    @Serializable
    private data class VersionDto(
        @SerialName("VersionNumber") val versionNumber: Long,
        @SerialName("SchemaVersionId") val versionId: String = "",
        @SerialName("Status") val status: String = "",
        @SerialName("CreatedTime") val createdTime: String = "",
    )

    @Serializable
    private data class SchemaVersionDto(
        @SerialName("SchemaDefinition") val definition: String = "",
    )
}
