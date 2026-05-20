package dev.lucascosta.awslocalmanager.features.inspector

import dev.lucascosta.awslocalmanager.data.model.inspector.InspectorDetail
import dev.lucascosta.awslocalmanager.data.model.inspector.InspectorResource
import dev.lucascosta.awslocalmanager.features.inspector.handler.InspectorServiceHandler

data class InspectorUiState(
    val handlers: List<InspectorServiceHandler> = emptyList(),
    val selectedHandler: InspectorServiceHandler? = null,

    val isLoadingResources: Boolean = false,
    val resources: List<InspectorResource> = emptyList(),
    val resourcesError: String? = null,

    val selectedResource: InspectorResource? = null,
    val isLoadingDetail: Boolean = false,
    val isLoadingSubDetail: Boolean = false,
    val detail: InspectorDetail? = null,
    val detailError: String? = null,

    val actionError: String? = null,
    val actionSuccess: Boolean = false,
    val lastUpdated: String = "",
)
