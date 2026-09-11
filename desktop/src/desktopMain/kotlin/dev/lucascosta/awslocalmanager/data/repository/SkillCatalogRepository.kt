package dev.lucascosta.awslocalmanager.data.repository

import dev.lucascosta.awslocalmanager.BuildConfig
import dev.lucascosta.awslocalmanager.constants.AppConstants.HTTP_CONNECT_TIMEOUT_MS
import dev.lucascosta.awslocalmanager.constants.AppConstants.HTTP_REQUEST_TIMEOUT_MS
import dev.lucascosta.awslocalmanager.constants.AppConstants.SKILL_CATALOG_DIR
import dev.lucascosta.awslocalmanager.constants.AppConstants.SKILL_CATALOG_FILENAME
import dev.lucascosta.awslocalmanager.constants.AppConstants.SKILL_CATALOG_REF
import dev.lucascosta.awslocalmanager.data.model.skill.CatalogSource
import dev.lucascosta.awslocalmanager.data.model.skill.SkillCatalog
import dev.lucascosta.awslocalmanager.data.model.skill.SkillCatalogEntry
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import java.security.MessageDigest

data class CatalogResult(
    val catalog: SkillCatalog,
    val source: CatalogSource,
)

class SkillCatalogRepository {
    private val httpClient =
        HttpClient(CIO) {
            install(HttpTimeout) {
                requestTimeoutMillis = HTTP_REQUEST_TIMEOUT_MS
                connectTimeoutMillis = HTTP_CONNECT_TIMEOUT_MS
            }
        }

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun loadCatalog(): CatalogResult {
        val remote =
            fetchRemote(SKILL_CATALOG_FILENAME)
                ?.let { text -> runCatching { json.decodeFromString<SkillCatalog>(text) }.getOrNull() }
        if (remote != null && remote.skills.isNotEmpty()) {
            return CatalogResult(remote, CatalogSource.REMOTE)
        }
        return CatalogResult(bundledCatalog(), CatalogSource.BUNDLED)
    }

    suspend fun loadContent(entry: SkillCatalogEntry): Result<String> {
        val bundled = bundledResource(entry.path)
        val remote = fetchRemote(entry.path)?.takeIf { matchesChecksum(it, entry.sha256) }
        val content = remote ?: bundled
        return if (content.isNullOrBlank()) {
            Result.failure(IllegalStateException("Skill content not available for ${entry.id}"))
        } else {
            Result.success(content)
        }
    }

    private fun bundledCatalog(): SkillCatalog =
        bundledResource(SKILL_CATALOG_FILENAME)
            ?.let { text -> runCatching { json.decodeFromString<SkillCatalog>(text) }.getOrNull() }
            ?: SkillCatalog()

    private fun bundledResource(path: String): String? =
        runCatching { javaClass.getResourceAsStream("/$path")?.bufferedReader()?.use { it.readText() } }.getOrNull()

    private suspend fun fetchRemote(path: String): String? =
        runCatching {
            val response = httpClient.get(remoteUrl(path))
            if (response.status.isSuccess()) response.bodyAsText() else null
        }.getOrNull()

    private fun remoteUrl(path: String): String =
        "https://raw.githubusercontent.com/${BuildConfig.GITHUB_OWNER}/${BuildConfig.GITHUB_REPO}/" +
            "$SKILL_CATALOG_REF/$SKILL_CATALOG_DIR/$path"

    private fun matchesChecksum(
        content: String,
        expected: String?,
    ): Boolean {
        if (expected.isNullOrBlank()) {
            return true
        }
        val digest = MessageDigest.getInstance("SHA-256").digest(content.toByteArray())
        return digest.joinToString("") { byte -> "%02x".format(byte) } == expected
    }
}
