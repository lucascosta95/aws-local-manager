package dev.lucascosta.awslocalmanager.domain

import dev.lucascosta.awslocalmanager.data.model.aws.ResourceRunningStatus
import dev.lucascosta.awslocalmanager.data.model.project.TerraformResource
import dev.lucascosta.awslocalmanager.data.repository.RunningResourceRepository

class AwsResourceChecker(
    private val runningResourceRepository: RunningResourceRepository,
) {
    suspend fun exists(
        resource: TerraformResource,
        endpoint: String,
    ): ResourceRunningStatus {
        val type = resource.resourceType ?: return ResourceRunningStatus.NOT_RUNNING
        if (!type.isCheckable) {
            return ResourceRunningStatus.UNKNOWN
        }

        return runCatching {
            val resources =
                runningResourceRepository.fetchAllRunningResources(
                    endpoint = endpoint,
                    activeServices = setOf(type.healthKey),
                )
            if (resources.any { it.name.equals(resource.awsName, ignoreCase = true) }) {
                ResourceRunningStatus.RUNNING
            } else {
                ResourceRunningStatus.NOT_RUNNING
            }
        }.getOrElse { ResourceRunningStatus.UNKNOWN }
    }
}
