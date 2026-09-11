package dev.lucascosta.awslocalmanager.features.skills

import dev.lucascosta.awslocalmanager.BaseViewModel
import dev.lucascosta.awslocalmanager.data.model.skill.AgentTarget
import dev.lucascosta.awslocalmanager.data.model.skill.AgentTargetRegistry
import dev.lucascosta.awslocalmanager.data.model.skill.InstalledSkill
import dev.lucascosta.awslocalmanager.data.model.skill.SkillCatalogEntry
import dev.lucascosta.awslocalmanager.data.model.skill.SkillsState
import dev.lucascosta.awslocalmanager.data.repository.SkillCatalogRepository
import dev.lucascosta.awslocalmanager.data.repository.SkillStateRepository
import dev.lucascosta.awslocalmanager.domain.SkillInstaller
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SkillsViewModel(
    private val catalogRepository: SkillCatalogRepository,
    private val stateRepository: SkillStateRepository,
    private val installer: SkillInstaller,
) : BaseViewModel() {
    private val _state = MutableStateFlow(SkillsUiState())
    val state: StateFlow<SkillsUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        scope.launch(Dispatchers.IO) {
            _state.update { it.copy(isLoading = true, error = null) }
            val result = catalogRepository.loadCatalog()
            val selected = result.catalog.skills.firstOrNull()
            val targets = selected?.let { entry -> buildTargets(entry, stateRepository.load()) } ?: emptyList()
            _state.update {
                it.copy(
                    isLoading = false,
                    skills = result.catalog.skills,
                    source = result.source,
                    selectedSkill = selected,
                    targets = targets,
                    selectedTargets = defaultSelection(targets),
                )
            }
        }
    }

    fun selectSkill(entry: SkillCatalogEntry) {
        scope.launch(Dispatchers.IO) {
            val targets = buildTargets(entry, stateRepository.load())
            _state.update {
                it.copy(
                    selectedSkill = entry,
                    targets = targets,
                    selectedTargets = defaultSelection(targets),
                    feedback = null,
                )
            }
        }
    }

    fun toggleTarget(targetId: String) {
        _state.update { current ->
            val updated =
                current.selectedTargets.toMutableSet().apply {
                    if (targetId in this) remove(targetId) else add(targetId)
                }
            current.copy(selectedTargets = updated)
        }
    }

    fun requestInstall() {
        if (_state.value.selectedTargets.isNotEmpty()) {
            _state.update { it.copy(showConfirmDialog = true) }
        }
    }

    fun dismissConfirm() {
        _state.update { it.copy(showConfirmDialog = false) }
    }

    fun confirmInstall() {
        val entry = _state.value.selectedSkill ?: return
        val targets = _state.value.selectedTargets.mapNotNull { AgentTargetRegistry.byId(it) }
        _state.update { it.copy(showConfirmDialog = false, isInstalling = true, feedback = null, error = null) }
        scope.launch(Dispatchers.IO) {
            val content = catalogRepository.loadContent(entry).getOrNull()
            if (content == null) {
                _state.update { it.copy(isInstalling = false, error = "content-unavailable") }
                return@launch
            }
            val installed = mutableListOf<String>()
            val failed = mutableListOf<String>()
            for (target in targets) {
                installer.install(entry, target, content).fold(
                    onSuccess = { file ->
                        stateRepository.record(InstalledSkill(entry.id, target.id, entry.version, file.absolutePath))
                        installed.add(file.absolutePath)
                    },
                    onFailure = { failed.add(target.displayName) },
                )
            }
            finish(entry, InstallFeedback(installedPaths = installed, failedTargets = failed))
        }
    }

    fun uninstall(target: AgentTarget) {
        val entry = _state.value.selectedSkill ?: return
        _state.update { it.copy(isInstalling = true, feedback = null, error = null) }
        scope.launch(Dispatchers.IO) {
            val feedback =
                installer.uninstall(entry, target).fold(
                    onSuccess = { file ->
                        stateRepository.forget(entry.id, target.id)
                        InstallFeedback(removedPaths = listOf(file.absolutePath))
                    },
                    onFailure = { InstallFeedback(failedTargets = listOf(target.displayName)) },
                )
            finish(entry, feedback)
        }
    }

    fun showPreview() {
        val entry = _state.value.selectedSkill ?: return
        scope.launch(Dispatchers.IO) {
            catalogRepository.loadContent(entry).fold(
                onSuccess = { content -> _state.update { it.copy(previewContent = content) } },
                onFailure = { _state.update { it.copy(error = "content-unavailable") } },
            )
        }
    }

    fun dismissPreview() {
        _state.update { it.copy(previewContent = null) }
    }

    fun clearFeedback() {
        _state.update { it.copy(feedback = null, error = null) }
    }

    private fun finish(
        entry: SkillCatalogEntry,
        feedback: InstallFeedback,
    ) {
        val targets = buildTargets(entry, stateRepository.load())
        _state.update {
            it.copy(
                isInstalling = false,
                targets = targets,
                selectedTargets = defaultSelection(targets),
                feedback = feedback,
            )
        }
    }

    private fun buildTargets(
        entry: SkillCatalogEntry,
        persisted: SkillsState,
    ): List<TargetStatus> =
        AgentTargetRegistry.all.map { target ->
            val recorded = persisted.installed.find { it.skillId == entry.id && it.targetId == target.id }
            val stillOnDisk = installer.isInstalled(entry, target)
            TargetStatus(
                target = target,
                isDetected = installer.isDetected(target),
                installedVersion = recorded?.version?.takeIf { stillOnDisk },
                path = installer.installPath(entry, target).absolutePath,
                invocation = installer.invocation(entry, target),
                legacyPath = installer.legacyInstall(entry, target)?.absolutePath,
            )
        }

    private fun defaultSelection(targets: List<TargetStatus>): Set<String> =
        targets.filter { (it.isDetected || it.hasLegacyInstall) && !it.isInstalled }.map { it.target.id }.toSet()
}
