package dev.lucascosta.awslocalmanager.features.running

import dev.lucascosta.awslocalmanager.constants.AppConstants.EMPTY_STRING
import dev.lucascosta.awslocalmanager.data.model.aws.MessageHistoryItem
import dev.lucascosta.awslocalmanager.data.model.aws.PublishResult
import dev.lucascosta.awslocalmanager.data.model.aws.RunningProjectGroup
import dev.lucascosta.awslocalmanager.data.model.aws.RunningResource
import dev.lucascosta.awslocalmanager.data.model.aws.SavedPayload

data class RunningUiState(
    val projectGroups: List<RunningProjectGroup> = emptyList(),
    val unassociated: List<RunningResource> = emptyList(),
    val selectedResources: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val isAutoRefresh: Boolean = false,
    val showDownConfirmation: Boolean = false,
    val pendingDownResources: List<RunningResource> = emptyList(),
    val deleteFailureCount: Int = 0,
    val publishTarget: RunningResource? = null,
    val publishJson: String = EMPTY_STRING,
    val isJsonValid: Boolean = true,
    val isSending: Boolean = false,
    val publishResult: PublishResult? = null,
    val publishFeedback: PublishFeedback? = null,
    val publishHistory: List<MessageHistoryItem> = emptyList(),
    val showHistoryDialog: Boolean = false,
    val s3ObjectKey: String = EMPTY_STRING,
    val s3FilePath: String? = null,
    val hasPayloadsFile: Boolean = false,
    val showPayloadsDialog: Boolean = false,
    val filteredPayloads: List<SavedPayload> = emptyList(),
)
