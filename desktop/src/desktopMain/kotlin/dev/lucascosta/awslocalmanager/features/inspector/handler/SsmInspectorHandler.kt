package dev.lucascosta.awslocalmanager.features.inspector.handler

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.ui.graphics.vector.ImageVector
import dev.lucascosta.awslocalmanager.data.model.inspector.InspectorDetail
import dev.lucascosta.awslocalmanager.data.model.inspector.InspectorResource
import dev.lucascosta.awslocalmanager.data.model.inspector.SsmInspectorParameter
import dev.lucascosta.awslocalmanager.data.remote.AwsSsmClient
import dev.lucascosta.awslocalmanager.data.remote.SsmParameterInfo

class SsmInspectorHandler : InspectorServiceHandler {
    override val serviceKey: String = "ssm"
    override val displayName: String = "SSM"
    override val icon: ImageVector = Icons.Outlined.Tune

    private var lastKnownParameters: List<SsmParameterInfo> = emptyList()

    override suspend fun loadResources(endpoint: String): List<InspectorResource> {
        val parameters = AwsSsmClient(endpoint).listParameters().getOrElse { return emptyList() }
        lastKnownParameters = parameters
        return parameters
            .groupBy { pathOf(it.name) }
            .map { (path, group) ->
                InspectorResource(
                    id = path,
                    name = path,
                    summaryType = "ssm",
                    summaryCount = group.size.toLong(),
                )
            }
            .sortedBy { it.name }
    }

    override suspend fun loadDetail(
        endpoint: String,
        resource: InspectorResource,
    ): InspectorDetail {
        val names = lastKnownParameters.filter { pathOf(it.name) == resource.id }.map { it.name }
        if (names.isEmpty()) {
            return InspectorDetail.SsmDetail(path = resource.id, parameters = emptyList())
        }

        val withValues =
            AwsSsmClient(endpoint).getParameterValues(names)
                .getOrElse { return InspectorDetail.SsmDetail(path = resource.id, parameters = emptyList()) }

        return InspectorDetail.SsmDetail(
            path = resource.id,
            parameters =
                withValues
                    .map { info ->
                        SsmInspectorParameter(
                            name = info.name,
                            type = info.type,
                            value = info.value.orEmpty(),
                            version = info.version,
                        )
                    }
                    .sortedBy { it.name },
        )
    }

    private fun pathOf(name: String): String = if (name.startsWith("/")) "/${name.removePrefix("/").substringBefore("/")}" else name
}
