package dev.lucascosta.awslocalmanager.data.model.aws

data class RunningProjectGroup(
    val projectName: String,
    val resources: List<RunningResource>,
)
