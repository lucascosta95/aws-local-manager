package dev.lucascosta.awslocalmanager.features.infrastructure

import dev.lucascosta.awslocalmanager.data.model.aws.AwsResourceDefinition
import dev.lucascosta.awslocalmanager.data.model.aws.ResourceRunningStatus
import dev.lucascosta.awslocalmanager.data.model.project.InfraProject
import dev.lucascosta.awslocalmanager.data.model.resources.SqsResource
import dev.lucascosta.awslocalmanager.data.remote.ProcessLine

data class InfrastructureUiState(
    val project: InfraProject? = null,
    val selectedResources: Set<String> = emptySet(),
    val resourceStatuses: Map<String, ResourceOpStatus> = emptyMap(),
    val typeFilter: AwsResourceDefinition? = null,
    val isRunning: Boolean = false,
    val logLines: List<ProcessLine> = emptyList(),
    val showCreateTemplateDialog: Boolean = false,
    val templateType: AwsResourceDefinition = SqsResource,
    val templateFileName: String = "",
    val templateCreatedFile: String? = null,
    val error: String? = null,
    val allSelected: Boolean = false,
    val runningStatus: Map<String, ResourceRunningStatus> = emptyMap(),
    val isCheckingRunning: Boolean = false,
    val hasRunOperation: Boolean = false,
    val availableTypes: List<AwsResourceDefinition> = emptyList(),
)
