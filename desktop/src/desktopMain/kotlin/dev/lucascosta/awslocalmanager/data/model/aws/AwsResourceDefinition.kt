package dev.lucascosta.awslocalmanager.data.model.aws

import dev.lucascosta.awslocalmanager.constants.AppConstants.PROCESS_DEFAULT_TIMEOUT_SECONDS

enum class SuccessSnackbarKey { GENERIC, S3, DYNAMODB }

interface AwsResourceDefinition {
    val id: String
    val displayName: String
    val terraformPrefix: String
    val healthKey: String
    val creationPriority: Int
    val isSupported: Boolean get() = true
    val isCheckable: Boolean get() = true
    val isQuickCreatable: Boolean
    val hasFilePublish: Boolean
    val publishableViaJson: Boolean
    val supportsPayloads: Boolean
    val successSnackbarKey: SuccessSnackbarKey
    val createTimeoutSeconds: Long get() = PROCESS_DEFAULT_TIMEOUT_SECONDS

    fun createCommand(
        name: String,
        extraProperties: Map<String, String> = emptyMap(),
    ): List<String>?

    fun deleteCommand(resource: RunningResource): List<String>?

    fun buildArn(name: String): String?

    fun terraformTemplate(label: String): String

    fun publishIdentifier(resource: RunningResource): String
}
