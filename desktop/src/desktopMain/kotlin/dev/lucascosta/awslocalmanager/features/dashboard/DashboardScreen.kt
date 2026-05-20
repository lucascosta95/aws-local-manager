package dev.lucascosta.awslocalmanager.features.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.lucascosta.awslocalmanager.data.model.aws.AwsResourceDefinition
import dev.lucascosta.awslocalmanager.data.model.aws.AwsService
import dev.lucascosta.awslocalmanager.data.model.aws.ResourceRegistry
import dev.lucascosta.awslocalmanager.data.model.health.AppServiceStatus
import dev.lucascosta.awslocalmanager.features.dashboard.components.ServiceCard
import dev.lucascosta.awslocalmanager.features.dashboard.components.UnsupportedServicesSection
import dev.lucascosta.awslocalmanager.i18n.LocalStrings
import org.koin.compose.koinInject

@Composable
fun DashboardScreen(viewModel: DashboardViewModel = koinInject()) {
    val state by viewModel.state.collectAsState()
    val strings = LocalStrings.current
    val filteredServices by viewModel.filteredServices.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = !state.isConnected,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        Icons.Outlined.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            strings.dashboardErrorBannerTitle,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                        Text(
                            state.error ?: strings.dashboardErrorBannerMessage.replace("{endpoint}", state.endpoint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                        )
                    }
                    OutlinedButton(
                        onClick = { viewModel.refresh() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onErrorContainer),
                    ) {
                        Text(strings.dashboardRetry)
                    }
                }
            }
        }

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                strings.dashboardTitle,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )

            if (state.lastUpdated.isNotEmpty()) {
                Text(
                    "${strings.dashboardLastUpdated} ${state.lastUpdated}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val filterOptions =
                listOf(
                    null to strings.dashboardFilterAll,
                    AppServiceStatus.ACTIVE to strings.statusActive,
                    AppServiceStatus.AVAILABLE to strings.statusAvailable,
                    AppServiceStatus.ERROR to strings.statusError,
                )
            filterOptions.forEach { (status, label) ->
                FilterChip(
                    selected = state.selectedFilter == status,
                    onClick = { viewModel.setFilter(status) },
                    label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                    shape = RoundedCornerShape(16.dp),
                )
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { viewModel.refresh() }) {
                Icon(Icons.Outlined.Refresh, contentDescription = strings.dashboardRetry)
            }
        }

        Spacer(Modifier.height(8.dp))

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                state.isLoading && state.services.isEmpty() && state.unsupportedServices.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }

                filteredServices.isEmpty() && state.unsupportedServices.isEmpty() && !state.isLoading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            Icons.Outlined.Refresh,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp),
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            strings.dashboardNoServices,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { viewModel.refresh() }) {
                            Text(strings.dashboardRetry)
                        }
                    }
                }

                else -> {
                    val listState = rememberLazyListState()

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize().padding(end = 12.dp),
                    ) {
                        item {
                            if (filteredServices.isNotEmpty()) {
                                Text(
                                    text = strings.dashboardSupportedServicesTitle,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                )
                            }
                        }

                        item {
                            SupportedServicesGrid(filteredServices, state.serviceStatuses)
                        }

                        item {
                            UnsupportedServicesSection(
                                services = state.unsupportedServices,
                                expanded = state.showUnsupportedServices,
                                onToggle = { viewModel.toggleUnsupportedServices() },
                            )
                        }
                    }

                    VerticalScrollbar(
                        adapter = rememberScrollbarAdapter(listState),
                        modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(end = 2.dp),
                    )

                    if (state.isLoading) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

private const val SERVICES_GRID_COLUMNS = 4

@Composable
private fun SupportedServicesGrid(
    services: List<AwsService>,
    serviceStatuses: Map<AwsResourceDefinition, AppServiceStatus>,
) {
    if (services.isEmpty()) return
    val rows = services.chunked(SERVICES_GRID_COLUMNS)
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        rows.forEach { rowItems ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                rowItems.forEach { service ->
                    Box(modifier = Modifier.weight(1f)) {
                        ServiceCard(
                            service = service,
                            appStatus = serviceStatuses[ResourceRegistry.fromHealthKey(service.name)],
                        )
                    }
                }
                repeat(SERVICES_GRID_COLUMNS - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}
