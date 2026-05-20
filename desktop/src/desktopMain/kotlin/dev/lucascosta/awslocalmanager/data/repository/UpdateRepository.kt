package dev.lucascosta.awslocalmanager.data.repository

import dev.lucascosta.awslocalmanager.BuildConfig
import dev.lucascosta.awslocalmanager.constants.AppConstants.HTTP_CONNECT_TIMEOUT_MS
import dev.lucascosta.awslocalmanager.constants.AppConstants.HTTP_REQUEST_TIMEOUT_MS
import dev.lucascosta.awslocalmanager.data.model.update.GitHubReleaseResponse
import dev.lucascosta.awslocalmanager.data.model.update.ReleaseInfo
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class UpdateRepository {
    private val httpClient =
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
            install(HttpTimeout) {
                requestTimeoutMillis = HTTP_REQUEST_TIMEOUT_MS
                connectTimeoutMillis = HTTP_CONNECT_TIMEOUT_MS
            }
        }

    suspend fun checkLatestRelease(): ReleaseInfo? =
        runCatching {
            val response =
                httpClient.get(
                    "https://api.github.com/repos/${BuildConfig.GITHUB_OWNER}/${BuildConfig.GITHUB_REPO}/releases/latest",
                ) {
                    header("Accept", "application/vnd.github.v3+json")
                }
            val release = response.body<GitHubReleaseResponse>()
            ReleaseInfo(
                version = release.tagName.removePrefix("v"),
                tagName = release.tagName,
                releaseUrl = release.htmlUrl,
                releaseNotes = release.body,
            )
        }.getOrNull()
}
