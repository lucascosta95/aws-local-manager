package dev.lucascosta.awslocalmanager.features.infrastructure

import dev.lucascosta.awslocalmanager.BaseViewModel
import dev.lucascosta.awslocalmanager.constants.AppConstants.SNS_SUBSCRIPTION_ARN_KEY
import dev.lucascosta.awslocalmanager.data.model.aws.AwsResourceDefinition
import dev.lucascosta.awslocalmanager.data.model.aws.ResourceRegistry
import dev.lucascosta.awslocalmanager.data.model.aws.ResourceRunningStatus
import dev.lucascosta.awslocalmanager.data.model.aws.SnsSubscription
import dev.lucascosta.awslocalmanager.data.model.health.AppServiceStatus
import dev.lucascosta.awslocalmanager.data.model.process.ProcessConfig
import dev.lucascosta.awslocalmanager.data.model.project.ApplyContext
import dev.lucascosta.awslocalmanager.data.model.project.InfraLogStrings
import dev.lucascosta.awslocalmanager.data.model.project.InfraProject
import dev.lucascosta.awslocalmanager.data.model.project.TerraformResource
import dev.lucascosta.awslocalmanager.data.model.resources.SnsSubscriptionResource
import dev.lucascosta.awslocalmanager.data.model.resources.SqsResource
import dev.lucascosta.awslocalmanager.data.remote.AwsCommands
import dev.lucascosta.awslocalmanager.data.remote.ProcessLine
import dev.lucascosta.awslocalmanager.data.remote.ProcessRunner
import dev.lucascosta.awslocalmanager.data.repository.PreferencesRepository
import dev.lucascosta.awslocalmanager.domain.AwsResourceChecker
import dev.lucascosta.awslocalmanager.domain.ServiceStatusChecker
import dev.lucascosta.awslocalmanager.domain.TerraformReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

class InfrastructureViewModel(
    private val preferencesRepository: PreferencesRepository,
    private val terraformReader: TerraformReader,
    private val serviceStatusChecker: ServiceStatusChecker,
    private val resourceChecker: AwsResourceChecker,
) : BaseViewModel() {
    private companion object {
        const val EXIT_CODE_ALREADY_EXISTS = 254

        fun computeAllSelected(
            project: InfraProject?,
            selectedResources: Set<String>,
        ): Boolean =
            selectedResources.isNotEmpty() &&
                project?.resources?.map { it.tfLabel }?.toSet() == selectedResources

        fun computeAvailableTypes(project: InfraProject): List<AwsResourceDefinition> =
            project.resources
                .mapNotNull { it.resourceType }
                .distinctBy { it.id }
                .sortedBy { it.id }
    }

    private val _state = MutableStateFlow(InfrastructureUiState())
    val state: StateFlow<InfrastructureUiState> = _state.asStateFlow()

    val visibleResources: StateFlow<List<TerraformResource>> =
        _state
            .map { state ->
                val resources = state.project?.resources ?: emptyList()
                if (state.typeFilter == null) {
                    resources
                } else {
                    resources.filter { it.resourceType == state.typeFilter }
                }
            }
            .distinctUntilChanged()
            .stateIn(scope, SharingStarted.Eagerly, emptyList())

    fun loadProject(project: InfraProject) {
        _state.update {
            val selected = project.resources.map { resource -> resource.tfLabel }.toSet()
            it.copy(
                project = project,
                selectedResources = selected,
                resourceStatuses = project.resources.associate { resource -> resource.tfLabel to ResourceOpStatus.IDLE },
                logLines = emptyList(),
                error = null,
                allSelected = computeAllSelected(project, selected),
                runningStatus = emptyMap(),
                hasRunOperation = false,
                availableTypes = computeAvailableTypes(project),
            )
        }
        checkRunningStatus()
    }

    fun refresh() {
        val currentProject = _state.value.project ?: return
        scope.launch(Dispatchers.IO) { doRefresh(currentProject) }
    }

    fun toggleResource(tfLabel: String) {
        _state.update {
            val updated =
                it.selectedResources.toMutableSet().apply {
                    if (tfLabel in this) remove(tfLabel) else add(tfLabel)
                }
            it.copy(selectedResources = updated, allSelected = computeAllSelected(it.project, updated))
        }
    }

    fun selectAll() {
        val project = _state.value.project ?: return
        val all = project.resources.map { resource -> resource.tfLabel }.toSet()
        _state.update { it.copy(selectedResources = all, allSelected = computeAllSelected(project, all)) }
    }

    fun deselectAll() {
        _state.update { it.copy(selectedResources = emptySet(), allSelected = false) }
    }

    fun setTypeFilter(type: AwsResourceDefinition?) {
        _state.update { it.copy(typeFilter = type) }
    }

    fun showCreateTemplate() {
        _state.update {
            it.copy(
                showCreateTemplateDialog = true,
                templateFileName = "",
                templateType = SqsResource,
            )
        }
    }

    fun dismissCreateTemplate() {
        _state.update { it.copy(showCreateTemplateDialog = false) }
    }

    fun setTemplateType(type: AwsResourceDefinition) {
        _state.update { it.copy(templateType = type) }
    }

    fun setTemplateFileName(name: String) {
        _state.update { it.copy(templateFileName = name) }
    }

    fun createTemplate() {
        val currentState = _state.value
        val project = currentState.project ?: return
        val fileName = currentState.templateFileName.let { if (it.endsWith(".tf")) it else "$it.tf" }
        val content = currentState.templateType.terraformTemplate(currentState.templateFileName.replace("-", "_").replace(".tf", ""))
        _state.update { it.copy(showCreateTemplateDialog = false) }
        scope.launch(Dispatchers.IO) {
            runCatching { File(project.directory, fileName).writeText(content) }
                .onSuccess {
                    doRefresh(project)
                    _state.update { it.copy(templateCreatedFile = fileName) }
                }
                .onFailure { exception -> _state.update { it.copy(error = exception.message) } }
        }
    }

    fun clearTemplateFeedback() {
        _state.update { it.copy(templateCreatedFile = null) }
    }

    fun upAllDirect(logStrings: InfraLogStrings) {
        scope.launch {
            val prefs = preferencesRepository.preferences.first()
            val project = _state.value.project ?: return@launch
            _state.update { it.copy(hasRunOperation = true, runningStatus = emptyMap()) }
            val toApply =
                project.resources
                    .filter { it.isSupported && it.resourceType != SnsSubscriptionResource }
                    .filter { _state.value.selectedResources.isEmpty() || it.tfLabel in _state.value.selectedResources }
                    .sortedBy { it.resourceType?.creationPriority ?: Int.MAX_VALUE }
            val subscriptions = terraformReader.readSnsSubscriptions(project.directory)
            runDirect(toApply, subscriptions, prefs.endpoint, logStrings)
        }
    }

    fun checkRunningStatus() {
        val project = _state.value.project ?: return
        scope.launch(Dispatchers.IO) {
            val endpoint = preferencesRepository.preferences.first().endpoint
            _state.update {
                it.copy(
                    isCheckingRunning = true,
                    runningStatus = project.resources.associate { resource -> resource.tfLabel to ResourceRunningStatus.CHECKING },
                )
            }

            val statuses = serviceStatusChecker.lastStatuses.value

            val results =
                project.resources
                    .map { resource ->
                        async {
                            val resourceType = resource.resourceType
                            val status =
                                if (resourceType == null) {
                                    ResourceRunningStatus.NOT_RUNNING
                                } else {
                                    val serviceActive = statuses[resourceType] != AppServiceStatus.ERROR
                                    if (!serviceActive) ResourceRunningStatus.NOT_RUNNING else checkResourceExists(resource, endpoint)
                                }
                            resource.tfLabel to status
                        }
                    }.awaitAll()
                    .toMap()
            _state.update { it.copy(runningStatus = results, isCheckingRunning = false) }
        }
    }

    private fun doRefresh(project: InfraProject) {
        val updatedResources = terraformReader.readResources(project.directory)
        val updatedProject = project.copy(resources = updatedResources)
        val selected = updatedResources.map { resource -> resource.tfLabel }.toSet()
        _state.update {
            it.copy(
                project = updatedProject,
                selectedResources = selected,
                resourceStatuses = updatedProject.resources.associate { resource -> resource.tfLabel to ResourceOpStatus.IDLE },
                allSelected = computeAllSelected(updatedProject, selected),
                runningStatus = emptyMap(),
                availableTypes = computeAvailableTypes(updatedProject),
            )
        }
        checkRunningStatus()
    }

    private suspend fun checkResourceExists(
        resource: TerraformResource,
        endpoint: String,
    ): ResourceRunningStatus = resourceChecker.exists(resource, endpoint)

    private suspend fun runDirect(
        resources: List<TerraformResource>,
        subscriptions: List<SnsSubscription>,
        endpoint: String,
        logStrings: InfraLogStrings,
    ) {
        val registry = resources.associateBy { it.tfLabel }
        val appliedLabels = resources.map { it.tfLabel }.toSet()
        val relevantSubscriptions =
            subscriptions.filter { sub ->
                sub.endpointRef.split(".").getOrNull(1) in appliedLabels
            }

        _state.update { state ->
            state.copy(
                isRunning = true,
                logLines = emptyList(),
                resourceStatuses =
                    state.resourceStatuses +
                        resources.associate { resource -> resource.tfLabel to ResourceOpStatus.PENDING } +
                        relevantSubscriptions.associate { sub -> sub.tfLabel to ResourceOpStatus.PENDING },
            )
        }

        val ctx = ApplyContext(ProcessRunner.awsEnvVars(endpoint), logStrings)
        for (resource in resources) {
            applyResourceCommand(resource, ctx)
        }
        for (sub in relevantSubscriptions) {
            applySubscription(sub, registry, ctx)
        }

        _state.update { it.copy(isRunning = false) }
    }

    private suspend fun applyResourceCommand(
        resource: TerraformResource,
        ctx: ApplyContext,
    ) {
        val typeName = resource.resourceType?.id ?: resource.rawAwsType
        appendLog(ProcessLine(ctx.logStrings.creatingFmt.replace("{name}", resource.awsName).replace("{type}", typeName), false))
        val command = resource.resourceType?.createCommand(resource.awsName, resource.extraProperties)
        if (command == null) {
            appendLog(ProcessLine(ctx.logStrings.unsupportedFmt.replace("{type}", typeName), true))
            setResourceStatus(resource.tfLabel, ResourceOpStatus.ERROR)
            return
        }

        setResourceStatus(resource.tfLabel, ResourceOpStatus.PENDING)

        ProcessRunner.run(command, ProcessConfig(envVars = ctx.env)).fold(
            onSuccess = { output ->
                output.stdout.lines().filter { it.isNotBlank() }.forEach { line -> appendLog(ProcessLine(line, false)) }
                val succeeded = output.exitCode == 0 || output.exitCode == EXIT_CODE_ALREADY_EXISTS
                if (succeeded) {
                    setResourceStatus(resource.tfLabel, ResourceOpStatus.SUCCESS)
                    appendLog(ProcessLine(ctx.logStrings.createdFmt.replace("{name}", resource.awsName), false))
                } else {
                    setResourceStatus(resource.tfLabel, ResourceOpStatus.ERROR)
                    appendLog(ProcessLine(ctx.logStrings.createErrorFmt.replace("{name}", resource.awsName), true))
                }
            },
            onFailure = {
                setResourceStatus(resource.tfLabel, ResourceOpStatus.ERROR)
                appendLog(ProcessLine(ctx.logStrings.createErrorFmt.replace("{name}", resource.awsName), true))
            },
        )
    }

    private suspend fun applySubscription(
        sub: SnsSubscription,
        registry: Map<String, TerraformResource>,
        ctx: ApplyContext,
    ) {
        appendLog(ProcessLine(ctx.logStrings.subscribingFmt.replace("{label}", sub.tfLabel), false))
        setResourceStatus(sub.tfLabel, ResourceOpStatus.PENDING)

        val topicArn = resolveArn(sub.topicRef, registry)
        val endpointArn = resolveArn(sub.endpointRef, registry)

        val output =
            ProcessRunner.run(
                AwsCommands.subscribeSns(topicArn, sub.protocol, endpointArn, sub.rawMessageDelivery),
                ProcessConfig(envVars = ctx.env),
            ).getOrElse {
                appendLog(ProcessLine(ctx.logStrings.subscribeErrorFmt.replace("{label}", sub.tfLabel), true))
                setResourceStatus(sub.tfLabel, ResourceOpStatus.ERROR)
                return
            }

        if (output.exitCode != 0) {
            appendLog(ProcessLine(ctx.logStrings.subscribeErrorFmt.replace("{label}", sub.tfLabel), true))
            setResourceStatus(sub.tfLabel, ResourceOpStatus.ERROR)
            return
        }

        output.stdout.lines().filter { it.isNotBlank() }.forEach { appendLog(ProcessLine(it, false)) }

        val filterPolicy = sub.filterPolicy
        if (filterPolicy != null) {
            val subArn =
                Regex(""""$SNS_SUBSCRIPTION_ARN_KEY"\s*:\s*"([^"]+)"""")
                    .find(output.stdout)?.groupValues?.get(1)
            if (subArn != null) {
                applyFilterPolicy(subArn, sub, ctx)
            }
        }

        appendLog(ProcessLine(ctx.logStrings.subscribedFmt.replace("{label}", sub.tfLabel), false))
        setResourceStatus(sub.tfLabel, ResourceOpStatus.SUCCESS)
    }

    private suspend fun applyFilterPolicy(
        subArn: String,
        sub: SnsSubscription,
        ctx: ApplyContext,
    ) {
        val filterPolicy = sub.filterPolicy ?: return
        val filterOk =
            ProcessRunner.run(
                AwsCommands.setSubscriptionAttribute(subArn, "FilterPolicy", filterPolicy),
                ProcessConfig(envVars = ctx.env),
            ).getOrNull()?.exitCode == 0
        if (!filterOk) appendLog(ProcessLine(ctx.logStrings.filterWarningFmt.replace("{label}", sub.tfLabel), true))

        if (sub.filterPolicyScope != null) {
            val scopeOk =
                ProcessRunner.run(
                    AwsCommands.setSubscriptionAttribute(subArn, "FilterPolicyScope", sub.filterPolicyScope),
                    ProcessConfig(envVars = ctx.env),
                ).getOrNull()?.exitCode == 0
            if (!scopeOk) appendLog(ProcessLine(ctx.logStrings.filterScopeWarningFmt.replace("{label}", sub.tfLabel), true))
        }
    }

    private fun resolveArn(
        ref: String,
        registry: Map<String, TerraformResource>,
    ): String {
        if (ref.startsWith("arn:")) return ref
        val parts = ref.split(".")
        return if (parts.size < 2) {
            ref
        } else {
            val name = registry[parts[1]]?.awsName ?: parts[1]
            ResourceRegistry.fromTerraformPrefix(parts[0])?.buildArn(name) ?: ref
        }
    }

    private fun appendLog(line: ProcessLine) {
        _state.update { it.copy(logLines = it.logLines + line) }
    }

    private fun setResourceStatus(
        tfLabel: String,
        status: ResourceOpStatus,
    ) {
        _state.update { it.copy(resourceStatuses = it.resourceStatuses + (tfLabel to status)) }
    }
}
