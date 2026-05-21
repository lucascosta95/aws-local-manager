package dev.lucascosta.awslocalmanager.features.inspector

import dev.lucascosta.awslocalmanager.BaseViewModel
import dev.lucascosta.awslocalmanager.data.model.inspector.InspectorResource
import dev.lucascosta.awslocalmanager.data.repository.PreferencesRepository
import dev.lucascosta.awslocalmanager.features.inspector.handler.InspectorHandlerRegistry
import dev.lucascosta.awslocalmanager.features.inspector.handler.InspectorServiceHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.time.Duration.Companion.seconds

class InspectorViewModel(
    private val preferencesRepository: PreferencesRepository,
) : BaseViewModel() {
    private val _state = MutableStateFlow(InspectorUiState())
    val state: StateFlow<InspectorUiState> = _state.asStateFlow()

    private var pollingJob: Job? = null
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

    init {
        val handlers = InspectorHandlerRegistry.all()
        _state.update { it.copy(handlers = handlers) }
        if (handlers.isNotEmpty()) {
            selectHandler(handlers.first())
        } else {
            restartPollingJob()
        }
    }

    fun selectHandler(handler: InspectorServiceHandler) {
        _state.update {
            it.copy(
                selectedHandler = handler,
                resources = emptyList(),
                resourcesError = null,
                selectedResource = null,
                detail = null,
                detailError = null,
            )
        }
        loadResourcesFor(handler)
        restartPollingJob()
    }

    fun selectResource(resource: InspectorResource) {
        _state.update {
            it.copy(
                selectedResource = resource,
                detail = null,
                detailError = null,
            )
        }
        scope.launch {
            val endpoint = preferencesRepository.preferences.first().endpoint
            loadDetailFor(endpoint, resource)
        }
    }

    fun refresh() {
        val handler = _state.value.selectedHandler ?: return
        loadResourcesFor(handler)
    }

    fun navigateToPath(path: String) {
        val resource = _state.value.selectedResource ?: return
        val updated = resource.copy(currentPath = path)
        _state.update { it.copy(selectedResource = updated, detailError = null) }
        scope.launch {
            val endpoint = preferencesRepository.preferences.first().endpoint
            loadDetailFor(endpoint, updated)
        }
    }

    fun selectDetailItem(subItemId: String) {
        scope.launch {
            val handler = _state.value.selectedHandler ?: return@launch
            val resource = _state.value.selectedResource ?: return@launch
            val currentDetail = _state.value.detail ?: return@launch
            val endpoint = preferencesRepository.preferences.first().endpoint

            _state.update { it.copy(isLoadingSubDetail = true) }

            runCatching {
                handler.loadSubDetail(endpoint, resource, currentDetail, subItemId)
            }.onSuccess { newDetail ->
                _state.update {
                    it.copy(
                        isLoadingSubDetail = false,
                        detail = newDetail ?: currentDetail,
                    )
                }
            }.onFailure { e ->
                _state.update { it.copy(isLoadingSubDetail = false, detailError = e.message) }
            }
        }
    }

    fun loadMoreItems() {
        scope.launch {
            val handler = _state.value.selectedHandler ?: return@launch
            val resource = _state.value.selectedResource ?: return@launch
            val currentDetail = _state.value.detail ?: return@launch
            val endpoint = preferencesRepository.preferences.first().endpoint

            _state.update { it.copy(isLoadingSubDetail = true) }

            runCatching {
                handler.loadMore(endpoint, resource, currentDetail)
            }.onSuccess { moreDetail ->
                _state.update {
                    it.copy(
                        isLoadingSubDetail = false,
                        detail = moreDetail ?: currentDetail,
                    )
                }
            }.onFailure { e ->
                _state.update { it.copy(isLoadingSubDetail = false, detailError = e.message) }
            }
        }
    }

    fun performAction(actionId: String) {
        scope.launch {
            val handler = _state.value.selectedHandler ?: return@launch
            val resource = _state.value.selectedResource ?: return@launch
            val endpoint = preferencesRepository.preferences.first().endpoint

            handler.performAction(endpoint, resource, actionId)
                .onSuccess {
                    _state.update { it.copy(actionSuccess = true, actionError = null) }
                    loadDetailFor(endpoint, resource)
                }
                .onFailure { e ->
                    _state.update { it.copy(actionError = e.message) }
                }
        }
    }

    fun clearActionState() {
        _state.update { it.copy(actionSuccess = false, actionError = null) }
    }

    fun retryDetail() {
        val resource = _state.value.selectedResource ?: return
        selectResource(resource)
    }

    private fun loadResourcesFor(handler: InspectorServiceHandler) {
        scope.launch {
            val endpoint = preferencesRepository.preferences.first().endpoint
            _state.update { it.copy(isLoadingResources = true, resourcesError = null) }
            runCatching { handler.loadResources(endpoint) }
                .onSuccess { resources ->
                    _state.update {
                        it.copy(
                            isLoadingResources = false,
                            resources = resources,
                            lastUpdated = currentTime(),
                        )
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoadingResources = false, resourcesError = e.message) }
                }
        }
    }

    private suspend fun loadDetailFor(
        endpoint: String,
        resource: InspectorResource,
    ) {
        val handler = _state.value.selectedHandler ?: return
        _state.update { it.copy(isLoadingDetail = true, detailError = null) }
        runCatching { handler.loadDetail(endpoint, resource) }
            .onSuccess { detail ->
                _state.update { it.copy(isLoadingDetail = false, detail = detail) }
            }
            .onFailure { e ->
                _state.update { it.copy(isLoadingDetail = false, detailError = e.message) }
            }
    }

    private fun restartPollingJob() {
        pollingJob?.cancel()
        scope.launch {
            val intervalSeconds = preferencesRepository.preferences.first().pollingIntervalSeconds
            if (intervalSeconds <= 0) return@launch
            pollingJob =
                scope.launch {
                    while (isActive) {
                        delay(intervalSeconds.toLong().seconds)
                        val handler = _state.value.selectedHandler ?: continue
                        val endpoint = preferencesRepository.preferences.first().endpoint
                        runCatching { handler.loadResources(endpoint) }
                            .onSuccess { resources ->
                                _state.update { it.copy(resources = resources, lastUpdated = currentTime()) }
                            }
                    }
                }
        }
    }

    private fun currentTime(): String = LocalTime.now().format(timeFormatter)
}
