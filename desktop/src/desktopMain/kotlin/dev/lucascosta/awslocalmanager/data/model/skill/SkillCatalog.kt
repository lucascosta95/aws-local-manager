package dev.lucascosta.awslocalmanager.data.model.skill

import kotlinx.serialization.Serializable

@Serializable
data class SkillCatalog(
    val version: Int = 1,
    val skills: List<SkillCatalogEntry> = emptyList(),
)

@Serializable
data class SkillTranslation(
    val name: String,
    val description: String,
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
    val translations: Map<String, SkillTranslation> = emptyMap(),
) {
    fun localizedName(language: String): String = translations[language]?.name ?: name

    fun localizedDescription(language: String): String = translations[language]?.description ?: description
}

enum class CatalogSource { REMOTE, BUNDLED }
