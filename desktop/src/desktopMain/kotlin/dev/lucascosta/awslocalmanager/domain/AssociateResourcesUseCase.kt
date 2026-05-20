package dev.lucascosta.awslocalmanager.domain

import dev.lucascosta.awslocalmanager.data.model.aws.RunningProjectGroup
import dev.lucascosta.awslocalmanager.data.model.aws.RunningResource
import dev.lucascosta.awslocalmanager.data.model.project.InfraProject

class AssociateResourcesUseCase {
    operator fun invoke(
        resources: List<RunningResource>,
        projects: List<InfraProject>,
    ): Pair<List<RunningProjectGroup>, List<RunningResource>> {
        val projectResourceNames =
            buildMap {
                projects.forEach { project ->
                    project.resources.forEach { resource -> put(resource.awsName, project.name) }
                }
            }

        val associated =
            resources.map { resource ->
                resource.copy(projectName = projectResourceNames[resource.name])
            }

        val groups =
            associated
                .filter { it.projectName != null }
                .groupBy { it.projectName!! }
                .map { (name, res) -> RunningProjectGroup(name, res) }
                .sortedBy { it.projectName }

        val unassociated = associated.filter { it.projectName == null }
        return groups to unassociated
    }
}
