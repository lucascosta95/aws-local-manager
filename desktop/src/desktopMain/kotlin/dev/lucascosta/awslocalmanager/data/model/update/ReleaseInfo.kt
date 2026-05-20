package dev.lucascosta.awslocalmanager.data.model.update

data class ReleaseInfo(
    val version: String,
    val tagName: String,
    val releaseUrl: String,
    val releaseNotes: String,
)
