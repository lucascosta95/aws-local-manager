package dev.lucascosta.awslocalmanager.data.model.project

import java.io.File

data class InfraProject(
    val name: String,
    val directory: File,
    val resources: List<TerraformResource>,
)
