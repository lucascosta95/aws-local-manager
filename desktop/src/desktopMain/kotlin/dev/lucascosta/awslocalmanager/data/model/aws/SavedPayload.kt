package dev.lucascosta.awslocalmanager.data.model.aws

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class SavedPayload(
    val name: String,
    val queue: String,
    val payload: JsonElement,
)
