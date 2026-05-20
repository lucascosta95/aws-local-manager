package dev.lucascosta.awslocalmanager.features.project

import dev.lucascosta.awslocalmanager.BaseViewModel
import dev.lucascosta.awslocalmanager.data.model.aws.ResourceRunningStatus
import dev.lucascosta.awslocalmanager.data.model.health.AppServiceStatus
import dev.lucascosta.awslocalmanager.data.model.project.InfraProject
import dev.lucascosta.awslocalmanager.data.model.project.ProjectRunningInfo
import dev.lucascosta.awslocalmanager.data.repository.PreferencesRepository
import dev.lucascosta.awslocalmanager.domain.AwsResourceChecker
import dev.lucascosta.awslocalmanager.domain.ServiceStatusChecker
import dev.lucascosta.awslocalmanager.domain.TerraformReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

class ProjectSelectorViewModel(
    private val preferencesRepository: PreferencesRepository,
    private val terraformReader: TerraformReader,
    private val serviceStatusChecker: ServiceStatusChecker,
    private val resourceChecker: AwsResourceChecker,
) : BaseViewModel() {
    private val _state = MutableStateFlow(ProjectSelectorUiState())
    val state: StateFlow<ProjectSelectorUiState> = _state.asStateFlow()

    init {
        scope.launch {
            preferencesRepository.preferences.collect { prefs ->
                _state.update { it.copy(currentDir = prefs.projectsDir) }
                if (prefs.projectsDir.isNotBlank()) {
                    scan(prefs.projectsDir)
                }
            }
        }
    }

    fun selectDirectory(path: String) {
        scope.launch { preferencesRepository.updateProjectsDir(path) }
    }

    fun refresh() {
        scope.launch {
            val dir = preferencesRepository.preferences.first().projectsDir
            if (dir.isNotBlank()) {
                scan(dir)
            }
        }
    }

    private fun scan(dirPath: String) {
        scope.launch(Dispatchers.IO) {
            _state.update { it.copy(isScanning = true, error = null, projectRunningInfo = emptyMap()) }
            val projects =
                runCatching {
                    terraformReader.findProjects(File(dirPath))
                }.getOrElse { exception ->
                    _state.update { state -> state.copy(isScanning = false, error = exception.message) }
                    return@launch
                }

            _state.update { it.copy(projects = projects, isScanning = false) }
            val endpoint = preferencesRepository.preferences.first().endpoint

            checkAllProjectsRunningStatus(projects, endpoint)
        }
    }

    private fun checkAllProjectsRunningStatus(
        projects: List<InfraProject>,
        endpoint: String,
    ) {
        scope.launch(Dispatchers.IO) {
            val initialInfo =
                projects.associate { project ->
                    project.name to
                        ProjectRunningInfo(
                            totalRunning = 0,
                            totalNotRunning = project.resources.size,
                            isLoading = true,
                        )
                }

            _state.update { it.copy(projectRunningInfo = initialInfo) }

            val jobs =
                projects.map { project ->
                    async { project.name to checkProjectRunning(project, endpoint) }
                }

            jobs.forEach { job ->
                val (projectName, info) = job.await()
                _state.update { state ->
                    state.copy(projectRunningInfo = state.projectRunningInfo + (projectName to info))
                }
            }
        }
    }

    private suspend fun checkProjectRunning(
        project: InfraProject,
        endpoint: String,
    ): ProjectRunningInfo {
        val statuses = serviceStatusChecker.lastStatuses.value
        var running = 0
        var notRunning = 0

        project.resources.forEach { resource ->
            val serviceActive =
                resource.resourceType?.let {
                    statuses[it] != AppServiceStatus.ERROR
                } == true

            if (!serviceActive) {
                notRunning++
            } else {
                when (resourceChecker.exists(resource, endpoint)) {
                    ResourceRunningStatus.RUNNING -> running++
                    ResourceRunningStatus.UNKNOWN -> {}
                    else -> notRunning++
                }
            }
        }

        return ProjectRunningInfo(totalRunning = running, totalNotRunning = notRunning, isLoading = false)
    }
}
