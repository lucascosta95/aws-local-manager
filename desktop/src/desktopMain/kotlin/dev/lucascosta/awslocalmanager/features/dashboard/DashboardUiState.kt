package dev.lucascosta.awslocalmanager.features.dashboard

import dev.lucascosta.awslocalmanager.constants.AppConstants.EMPTY_STRING
import dev.lucascosta.awslocalmanager.data.model.aws.AwsResourceDefinition
import dev.lucascosta.awslocalmanager.data.model.aws.AwsService
import dev.lucascosta.awslocalmanager.data.model.health.AppServiceStatus

data class DashboardUiState(
    val services: List<AwsService> = emptyList(),
    val unsupportedServices: List<AwsService> = emptyList(),
    val serviceStatuses: Map<AwsResourceDefinition, AppServiceStatus> = emptyMap(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isConnected: Boolean = false,
    val selectedFilter: AppServiceStatus? = null,
    val lastUpdated: String = EMPTY_STRING,
    val endpoint: String = EMPTY_STRING,
    val showUnsupportedServices: Boolean = true,
)
