package dev.lucascosta.awslocalmanager.features.project

import dev.lucascosta.awslocalmanager.constants.AppConstants.EMPTY_STRING
import dev.lucascosta.awslocalmanager.data.model.project.InfraProject
import dev.lucascosta.awslocalmanager.data.model.project.ProjectRunningInfo

data class ProjectSelectorUiState(
    val currentDir: String = EMPTY_STRING,
    val projects: List<InfraProject> = emptyList(),
    val isScanning: Boolean = false,
    val error: String? = null,
    val projectRunningInfo: Map<String, ProjectRunningInfo> = emptyMap(),
)
