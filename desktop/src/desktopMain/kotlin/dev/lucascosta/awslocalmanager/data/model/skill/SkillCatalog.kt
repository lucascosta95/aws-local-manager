package dev.lucascosta.awslocalmanager.data.model.skill

import kotlinx.serialization.Serializable

@Serializable
data class SkillCatalog(
    val version: Int = 1,
    val skills: List<SkillCatalogEntry> = emptyList(),
)

@Serializable
data class SkillCatalogEntry(
    val id: String,
    val name: String,
    val description: String,
    val version: String,
    val path: String,
    val sha256: String? = null,
    val tags: List<String> = emptyList(),
)

enum class CatalogSource { REMOTE, BUNDLED }
