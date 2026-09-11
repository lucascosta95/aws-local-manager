package dev.lucascosta.awslocalmanager.data.model.project

import java.io.File

data class InfraProject(
    val name: String,
    val directory: File,
    val resources: List<TerraformResource>,
) {
    /**
     * What tells two projects apart. [name] comes from `aws-local.config.json` and nothing stops
     * two folders from carrying the same one, which is exactly what happens in a repository whose
     * services are all named after the same product. The folder is unique by construction.
     */
    val id: String get() = directory.absolutePath
}
