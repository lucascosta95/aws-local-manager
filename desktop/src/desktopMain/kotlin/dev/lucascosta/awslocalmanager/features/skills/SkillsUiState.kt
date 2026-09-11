package dev.lucascosta.awslocalmanager.features.skills

import dev.lucascosta.awslocalmanager.data.model.skill.AgentTarget
import dev.lucascosta.awslocalmanager.data.model.skill.CatalogSource
import dev.lucascosta.awslocalmanager.data.model.skill.SkillCatalogEntry

data class TargetStatus(
    val target: AgentTarget,
    val isDetected: Boolean,
    val installedVersion: String?,
    val path: String,
) {
    val isInstalled: Boolean get() = installedVersion != null

    fun hasUpdate(availableVersion: String): Boolean = installedVersion != null && installedVersion != availableVersion
}

data class InstallFeedback(
    val skillName: String,
    val installedPaths: List<String> = emptyList(),
    val removedPaths: List<String> = emptyList(),
    val failedTargets: List<String> = emptyList(),
)

data class SkillsUiState(
    val isLoading: Boolean = true,
    val skills: List<SkillCatalogEntry> = emptyList(),
    val selectedSkill: SkillCatalogEntry? = null,
    val targets: List<TargetStatus> = emptyList(),
    val selectedTargets: Set<String> = emptySet(),
    val source: CatalogSource = CatalogSource.BUNDLED,
    val isInstalling: Boolean = false,
    val showConfirmDialog: Boolean = false,
    val previewContent: String? = null,
    val feedback: InstallFeedback? = null,
    val error: String? = null,
)
