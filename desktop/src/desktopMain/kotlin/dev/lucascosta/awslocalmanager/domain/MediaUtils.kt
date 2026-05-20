package dev.lucascosta.awslocalmanager.domain

import dev.lucascosta.awslocalmanager.data.model.aws.MediaType
import kotlinx.serialization.json.Json

fun isValidJson(text: String): Boolean {
    if (text.isBlank()) {
        return false
    }

    return runCatching { Json.parseToJsonElement(text) }.isSuccess
}

fun guessContentType(filename: String): String = MediaType.from(filename)
