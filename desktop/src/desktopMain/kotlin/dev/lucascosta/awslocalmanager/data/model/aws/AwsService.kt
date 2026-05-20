package dev.lucascosta.awslocalmanager.data.model.aws

import dev.lucascosta.awslocalmanager.data.model.health.ServiceStatus

data class AwsService(
    val name: String,
    val status: ServiceStatus,
    val endpoint: String,
) {
    val displayName: String get() = ResourceRegistry.fromHealthKey(name)?.displayName ?: name.uppercase()
}
