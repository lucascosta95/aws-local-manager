package dev.lucascosta.awslocalmanager.data.model.skill

import kotlinx.serialization.Serializable

@Serializable
data class InstalledSkill(
    val skillId: String,
    val targetId: String,
    val version: String,
    val path: String,
)

@Serializable
data class SkillsState(
    val installed: List<InstalledSkill> = emptyList(),
)
