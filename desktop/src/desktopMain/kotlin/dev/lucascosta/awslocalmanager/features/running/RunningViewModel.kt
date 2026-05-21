package dev.lucascosta.awslocalmanager.features.running

import dev.lucascosta.awslocalmanager.BaseViewModel
import dev.lucascosta.awslocalmanager.constants.AppConstants.DELETION_SETTLE_DELAY_MS
import dev.lucascosta.awslocalmanager.constants.AppConstants.EMPTY_STRING
import dev.lucascosta.awslocalmanager.constants.AppConstants.PAYLOADS_FILENAME
import dev.lucascosta.awslocalmanager.constants.AppConstants.S3_URI_PREFIX
import dev.lucascosta.awslocalmanager.data.model.aws.MessageHistoryItem
import dev.lucascosta.awslocalmanager.data.model.aws.PublishResult
import dev.lucascosta.awslocalmanager.data.model.aws.RunningResource
import dev.lucascosta.awslocalmanager.data.model.aws.SavedPayload
import dev.lucascosta.awslocalmanager.data.model.aws.resourceId
import dev.lucascosta.awslocalmanager.data.model.project.InfraProject
import dev.lucascosta.awslocalmanager.data.model.resources.S3Resource
import dev.lucascosta.awslocalmanager.data.model.resources.SnsResource
import dev.lucascosta.awslocalmanager.data.model.resources.SqsResource
import dev.lucascosta.awslocalmanager.data.repository.MessageRepository
import dev.lucascosta.awslocalmanager.data.repository.PreferencesRepository
import dev.lucascosta.awslocalmanager.data.repository.RunningResourceRepository
import dev.lucascosta.awslocalmanager.data.repository.SavedPayloadRepository
import dev.lucascosta.awslocalmanager.data.repository.ServiceHealthRepository
import dev.lucascosta.awslocalmanager.domain.AssociateResourcesUseCase
import dev.lucascosta.awslocalmanager.domain.TerraformReader
import dev.lucascosta.awslocalmanager.domain.guessContentType
import dev.lucascosta.awslocalmanager.domain.isValidJson
import dev.lucascosta.awslocalmanager.util.PathUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import java.io.File
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

class RunningViewModel(
    private val preferencesRepository: PreferencesRepository,
    private val terraformReader: TerraformReader,
    private val serviceHealthRepository: ServiceHealthRepository,
    private val savedPayloadRepository: SavedPayloadRepository,
    private val runningResourceRepository: RunningResourceRepository,
    private val messageRepositoryFactory: (String) -> MessageRepository,
    private val associateResources: AssociateResourcesUseCase,
) : BaseViewModel() {
    companion object {
        private val prettyJson = Json { prettyPrint = true }
    }

    private val _state = MutableStateFlow(RunningUiState())
    val state: StateFlow<RunningUiState> = _state.asStateFlow()

    private var autoRefreshJob: Job? = null

    fun refresh() {
        scope.launch {
            val prefs = preferencesRepository.preferences.first()
            loadResources(prefs.endpoint, prefs.projectsDir)
        }
    }

    fun toggleAutoRefresh(enabled: Boolean) {
        autoRefreshJob?.cancel()
        if (enabled) {
            autoRefreshJob =
                startPolling(
                    intervalProvider = { preferencesRepository.preferences.first().pollingIntervalSeconds.toLong() },
                    action = { refresh() },
                )
        }
        _state.update { it.copy(isAutoRefresh = enabled) }
    }

    fun toggleResource(resourceId: String) {
        _state.update {
            val updated =
                it.selectedResources.toMutableSet().apply {
                    if (resourceId in this) remove(resourceId) else add(resourceId)
                }
            it.copy(selectedResources = updated)
        }
    }

    fun selectAll() {
        val all =
            (_state.value.projectGroups.flatMap { it.resources } + _state.value.unassociated)
                .map { it.resourceId() }.toSet()
        _state.update { it.copy(selectedResources = all) }
    }

    fun requestDownSelected() {
        val all = _state.value.projectGroups.flatMap { it.resources } + _state.value.unassociated
        val toDown = all.filter { it.resourceId() in _state.value.selectedResources }
        _state.update { it.copy(showDownConfirmation = true, pendingDownResources = toDown) }
    }

    fun requestDownResource(resource: RunningResource) {
        _state.update { it.copy(showDownConfirmation = true, pendingDownResources = listOf(resource)) }
    }

    fun dismissDown() {
        _state.update { it.copy(showDownConfirmation = false, pendingDownResources = emptyList()) }
    }

    fun confirmDown() {
        val resources = _state.value.pendingDownResources
        _state.update { it.copy(showDownConfirmation = false, pendingDownResources = emptyList()) }

        scope.launch {
            val prefs = preferencesRepository.preferences.first()
            val results = runningResourceRepository.deleteResources(resources, prefs.endpoint)
            val failures = results.count { !it.value }
            if (failures > 0) {
                _state.update { it.copy(deleteFailureCount = failures) }
            }
            delay(DELETION_SETTLE_DELAY_MS.milliseconds)
            refresh()
        }
    }

    fun selectPublishTarget(resource: RunningResource) {
        _state.update {
            it.copy(
                publishTarget = resource,
                publishJson = EMPTY_STRING,
                isJsonValid = true,
                publishResult = null,
                s3FilePath = null,
                s3ObjectKey = EMPTY_STRING,
            )
        }
    }

    fun updatePublishJson(json: String) {
        _state.update { it.copy(publishJson = json, isJsonValid = isValidJson(json)) }
    }

    fun formatPublishJson() {
        val body = _state.value.publishJson
        if (body.isBlank()) return
        runCatching {
            val element = Json.parseToJsonElement(body)
            val formatted = prettyJson.encodeToString(JsonElement.serializer(), element)
            _state.update { it.copy(publishJson = formatted, isJsonValid = true) }
        }.onFailure {
            _state.update { it.copy(isJsonValid = false) }
        }
    }

    fun selectPublishFile(path: String) {
        val name = File(path).name
        _state.update { state ->
            state.copy(s3FilePath = path, s3ObjectKey = if (state.s3ObjectKey.isBlank()) name else state.s3ObjectKey)
        }
    }

    fun clearPublishFile() {
        _state.update { it.copy(s3FilePath = null, s3ObjectKey = "") }
    }

    fun updateS3Key(key: String) {
        _state.update { it.copy(s3ObjectKey = key) }
    }

    fun togglePublishHistoryItem(id: String) {
        _state.update { state ->
            state.copy(
                publishHistory =
                    state.publishHistory.map { item ->
                        if (item.id == id) item.copy(isExpanded = !item.isExpanded) else item
                    },
            )
        }
    }

    fun sendPublish() {
        val currentState = _state.value
        val target = currentState.publishTarget ?: return
        scope.launch {
            val prefs = preferencesRepository.preferences.first()
            val repo = messageRepositoryFactory(prefs.endpoint)

            _state.update { it.copy(isSending = true, publishResult = null) }

            val payload =
                when (target.type) {
                    S3Resource ->
                        publishS3(target, currentState, repo) ?: run {
                            _state.update { it.copy(isSending = false) }
                            return@launch
                        }

                    else ->
                        publishMessage(target, currentState, repo) ?: run {
                            _state.update { it.copy(isSending = false) }
                            return@launch
                        }
                }

            val publishResult = toPublishResult(payload)
            val historyItem = toHistoryItem(payload, publishResult)
            _state.update {
                it.copy(
                    isSending = false,
                    publishResult = publishResult,
                    publishFeedback = toFeedback(publishResult, target),
                    publishHistory = (listOf(historyItem) + it.publishHistory).take(prefs.maxHistory),
                )
            }
        }
    }

    private fun toPublishResult(payload: PublishPayload): PublishResult =
        payload.result.getOrElse { exception ->
            PublishResult(success = false, error = exception.message ?: "Unknown error")
        }

    private fun toFeedback(
        result: PublishResult,
        target: RunningResource,
    ): PublishFeedback =
        if (!result.success) {
            PublishFeedback.Error(result.error ?: "Unknown error")
        } else {
            PublishFeedback.Success(target.type.successSnackbarKey)
        }

    private fun toHistoryItem(
        payload: PublishPayload,
        result: PublishResult,
    ): MessageHistoryItem =
        MessageHistoryItem(
            id = UUID.randomUUID().toString(),
            type = payload.messageType,
            resourceName = payload.resourceName,
            jsonBody = payload.bodySummary,
            result = result,
        )

    fun clearPublishResult() {
        _state.update { it.copy(publishResult = null, publishFeedback = null) }
    }

    fun toggleHistoryDialog() {
        _state.update { it.copy(showHistoryDialog = !it.showHistoryDialog) }
    }

    fun clearHistory() {
        _state.update { it.copy(publishHistory = emptyList(), showHistoryDialog = false) }
    }

    fun openPayloadsDialog() {
        val target = _state.value.publishTarget ?: return
        scope.launch {
            val prefs = preferencesRepository.preferences.first()
            val projects =
                withContext(Dispatchers.IO) {
                    terraformReader.findProjects(File(PathUtils.expandTilde(prefs.projectsDir)))
                }

            val queueNames =
                if (target.type == SnsResource) {
                    withContext(Dispatchers.IO) { resolveSubscribedSqsNames(target.name, projects) }
                } else {
                    setOf(target.name)
                }

            val payloads =
                withContext(Dispatchers.IO) {
                    projects.flatMap { savedPayloadRepository.loadPayloads(it.directory) }
                }.filter { it.queue in queueNames }
                    .distinctBy { it.name + it.queue }

            _state.update { it.copy(showPayloadsDialog = true, filteredPayloads = payloads) }
        }
    }

    private fun resolveSubscribedSqsNames(
        topicName: String,
        projects: List<InfraProject>,
    ): Set<String> {
        val names = mutableSetOf(topicName)
        for (project in projects) {
            val snsTfLabel =
                project.resources
                    .find { it.rawAwsType == SnsResource.terraformPrefix && it.awsName == topicName }
                    ?.tfLabel

            val subscriptions = terraformReader.readSnsSubscriptions(project.directory)
            for (sub in subscriptions) {
                val matchesTopic =
                    (snsTfLabel != null && sub.topicRef.contains(snsTfLabel)) ||
                        sub.topicRef.endsWith(":$topicName") ||
                        sub.topicRef == topicName

                if (matchesTopic) {
                    resolveEndpointToQueueName(sub.endpointRef, project)?.let { names.add(it) }
                }
            }
        }
        return names
    }

    private fun resolveEndpointToQueueName(
        endpointRef: String,
        project: InfraProject,
    ): String? {
        val tfLabel = Regex("""aws_sqs_queue\.(\w+)\.""").find(endpointRef)?.groupValues?.get(1)
        if (tfLabel != null) {
            val resource =
                project.resources.find { it.rawAwsType == SqsResource.terraformPrefix && it.tfLabel == tfLabel }

            if (resource != null) {
                return resource.awsName
            }
        }

        if (endpointRef.startsWith("http")) {
            return endpointRef.substringAfterLast("/").takeIf { it.isNotBlank() }
        }

        if (endpointRef.startsWith("arn:")) {
            return endpointRef.substringAfterLast(":").takeIf { it.isNotBlank() }
        }

        return null
    }

    fun closePayloadsDialog() {
        _state.update { it.copy(showPayloadsDialog = false, filteredPayloads = emptyList()) }
    }

    fun applyPayload(payload: SavedPayload) {
        val formatted =
            runCatching {
                prettyJson.encodeToString(JsonElement.serializer(), payload.payload)
            }.getOrElse { payload.payload.toString() }

        _state.update {
            it.copy(
                publishJson = formatted,
                isJsonValid = true,
                showPayloadsDialog = false,
                filteredPayloads = emptyList(),
            )
        }
    }

    private suspend fun loadResources(
        endpoint: String,
        projectsDir: String,
    ) {
        _state.update { it.copy(isLoading = true) }

        val projects = fetchProjectsFromDisk(PathUtils.expandTilde(projectsDir))
        val hasPayloadsFile =
            withContext(Dispatchers.IO) {
                projects.any { File(it.directory, PAYLOADS_FILENAME).isFile }
            }

        if (serviceHealthRepository.servicesHealth.value.isEmpty()) {
            serviceHealthRepository.refresh(endpoint)
        }

        val activeServices = serviceHealthRepository.runningServices()
        val allRunning = runningResourceRepository.fetchAllRunningResources(endpoint, activeServices)
        val (groups, unassociated) = associateResources(allRunning, projects)

        _state.update {
            it.copy(
                isLoading = false,
                projectGroups = groups,
                unassociated = unassociated,
                hasPayloadsFile = hasPayloadsFile,
            )
        }
    }

    private suspend fun fetchProjectsFromDisk(projectsDir: String) =
        withContext(Dispatchers.IO) {
            runCatching { terraformReader.findProjects(File(projectsDir)) }.getOrElse { emptyList() }
        }

    private suspend fun publishS3(
        target: RunningResource,
        state: RunningUiState,
        repo: MessageRepository,
    ): PublishPayload? {
        val file = state.s3FilePath?.let { File(it) }?.takeIf { it.exists() } ?: return null
        val key = if (state.s3ObjectKey.isBlank()) file.name else state.s3ObjectKey
        val bytes = withContext(Dispatchers.IO) { file.readBytes() }
        val result = repo.uploadS3(target.name, key, bytes, guessContentType(file.name))
        return PublishPayload(result, "${file.name} → $S3_URI_PREFIX${target.name}/$key", "${target.name}/$key", S3Resource)
    }

    private suspend fun publishMessage(
        target: RunningResource,
        state: RunningUiState,
        repo: MessageRepository,
    ): PublishPayload? {
        val identifier = target.type.publishIdentifier(target)
        val result = repo.publish(target.type, identifier, state.publishJson)
        return if (result == null) {
            null
        } else {
            PublishPayload(result, state.publishJson, target.name, target.type)
        }
    }
}
