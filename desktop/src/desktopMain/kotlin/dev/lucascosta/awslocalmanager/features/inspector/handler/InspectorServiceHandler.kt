package dev.lucascosta.awslocalmanager.features.inspector.handler

import androidx.compose.ui.graphics.vector.ImageVector
import dev.lucascosta.awslocalmanager.data.model.inspector.InspectorDetail
import dev.lucascosta.awslocalmanager.data.model.inspector.InspectorResource

interface InspectorServiceHandler {
    val serviceKey: String
    val displayName: String
    val icon: ImageVector

    suspend fun loadResources(endpoint: String): List<InspectorResource>

    suspend fun loadDetail(
        endpoint: String,
        resource: InspectorResource,
    ): InspectorDetail

    suspend fun loadSubDetail(
        endpoint: String,
        resource: InspectorResource,
        currentDetail: InspectorDetail,
        subItemId: String,
    ): InspectorDetail? = null

    suspend fun loadMore(
        endpoint: String,
        resource: InspectorResource,
        currentDetail: InspectorDetail,
    ): InspectorDetail? = null

    suspend fun performAction(
        endpoint: String,
        resource: InspectorResource,
        actionId: String,
    ): Result<Unit> = Result.failure(UnsupportedOperationException("No actions available"))
}
