package dev.lucascosta.awslocalmanager.data.repository

import dev.lucascosta.awslocalmanager.constants.AppConstants.PAYLOADS_FILENAME
import dev.lucascosta.awslocalmanager.data.model.aws.SavedPayload
import kotlinx.serialization.json.Json
import java.io.File

class SavedPayloadRepository {
    private val json = Json { ignoreUnknownKeys = true }

    fun loadPayloads(infraDir: File): List<SavedPayload> {
        val file = File(infraDir, PAYLOADS_FILENAME)
        if (!file.exists()) {
            return emptyList()
        }
        return runCatching {
            json.decodeFromString<List<SavedPayload>>(file.readText())
        }.getOrElse { emptyList() }
    }
}
