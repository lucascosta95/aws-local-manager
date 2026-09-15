package dev.lucascosta.awslocalmanager.data.model.aws

import dev.lucascosta.awslocalmanager.data.model.health.ServiceStatus
import dev.lucascosta.awslocalmanager.domain.AwsServiceType

data class AwsService(
    val name: String,
    val status: ServiceStatus,
    val endpoint: String,
) {
    val displayName: String get() = AwsServiceType.from(name)?.displayName ?: name.uppercase().replace('-', ' ')
}
