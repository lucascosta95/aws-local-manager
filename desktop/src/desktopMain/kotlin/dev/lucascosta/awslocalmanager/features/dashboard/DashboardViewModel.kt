package dev.lucascosta.awslocalmanager.features.dashboard

import dev.lucascosta.awslocalmanager.BaseViewModel
import dev.lucascosta.awslocalmanager.constants.AppConstants.TIME_FORMAT_PATTERN
import dev.lucascosta.awslocalmanager.data.model.aws.AwsService
import dev.lucascosta.awslocalmanager.data.model.aws.ResourceRegistry
import dev.lucascosta.awslocalmanager.data.model.health.AppServiceStatus
import dev.lucascosta.awslocalmanager.data.repository.PreferencesRepository
import dev.lucascosta.awslocalmanager.data.repository.ServiceHealthRepository
import dev.lucascosta.awslocalmanager.domain.ServiceStatusChecker
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class DashboardViewModel(
    private val serviceHealthRepository: ServiceHealthRepository,
    private val preferencesRepository: PreferencesRepository,
    private val serviceStatusChecker: ServiceStatusChecker,
) : BaseViewModel() {
    private val _state = MutableStateFlow(DashboardUiState())
    val state: StateFlow<DashboardUiState> = _state.asStateFlow()

    val filteredServices: StateFlow<List<AwsService>> =
        _state
            .map { dashboardState ->
                if (dashboardState.selectedFilter == null) {
                    dashboardState.services
                } else {
                    dashboardState.services.filter { svc ->
                        val type = ResourceRegistry.fromHealthKey(svc.name)
                        dashboardState.serviceStatuses[type] == dashboardState.selectedFilter
                    }
                }
            }
            .distinctUntilChanged()
            .stateIn(scope, SharingStarted.Eagerly, emptyList())

    private val supportedHealthKeys: Set<String> =
        ResourceRegistry.all().map { it.healthKey }.toSet()

    private var pollingJob: Job? = null
    private var refreshJob: Job? = null
    private val timeFormatter = DateTimeFormatter.ofPattern(TIME_FORMAT_PATTERN)

    init {
        scope.launch {
            preferencesRepository.preferences.collect { prefs ->
                restartPolling(prefs.endpoint, prefs.pollingIntervalSeconds)
            }
        }
    }

    fun refresh(endpoint: String? = null) {
        refreshJob?.cancel()
        refreshJob =
            scope.launch {
                val prefs = preferencesRepository.preferences.first()
                val ep = endpoint ?: prefs.endpoint
                _state.update { it.copy(isLoading = true, error = null, endpoint = ep) }
                serviceHealthRepository.refresh(ep).fold(
                    onSuccess = { services ->
                        val (supported, unsupported) =
                            services.partition { svc ->
                                svc.name.lowercase() in supportedHealthKeys
                            }

                        val statuses = serviceStatusChecker.checkAll(ep)
                        _state.update {
                            it.copy(
                                services = supported,
                                unsupportedServices = unsupported,
                                serviceStatuses = statuses,
                                isLoading = false,
                                isConnected = true,
                                lastUpdated = LocalTime.now().format(timeFormatter),
                            )
                        }
                    },
                    onFailure = { exception ->
                        _state.update {
                            it.copy(isLoading = false, isConnected = false, error = exception.message ?: "Unknown error")
                        }
                    },
                )
            }
    }

    fun setFilter(status: AppServiceStatus?) {
        _state.update { it.copy(selectedFilter = status) }
    }

    fun toggleUnsupportedServices() {
        _state.update { it.copy(showUnsupportedServices = !it.showUnsupportedServices) }
    }

    private fun restartPolling(
        endpoint: String,
        intervalSeconds: Int,
    ) {
        pollingJob?.cancel()
        if (intervalSeconds <= 0) {
            refresh(endpoint)
            return
        }
        pollingJob =
            startPolling(
                intervalProvider = { intervalSeconds.toLong() },
                action = { refresh(endpoint) },
            )
    }
}
