package dev.lucascosta.awslocalmanager.data.model.project

data class ProjectRunningInfo(
    val totalRunning: Int,
    val totalNotRunning: Int,
    val isLoading: Boolean = true,
)
