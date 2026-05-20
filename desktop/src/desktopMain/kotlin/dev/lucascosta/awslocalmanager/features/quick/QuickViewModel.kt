package dev.lucascosta.awslocalmanager.features.quick

import dev.lucascosta.awslocalmanager.BaseViewModel
import dev.lucascosta.awslocalmanager.constants.AppConstants.DLQ_SUFFIX
import dev.lucascosta.awslocalmanager.constants.AppConstants.SQS_DLQ_CREATION_DELAY_MS
import dev.lucascosta.awslocalmanager.constants.AppConstants.SQS_DLQ_TARGET_ARN_KEY
import dev.lucascosta.awslocalmanager.constants.AppConstants.SQS_MAX_RECEIVE_COUNT_KEY
import dev.lucascosta.awslocalmanager.constants.AppConstants.SQS_REDRIVE_POLICY_ATTR
import dev.lucascosta.awslocalmanager.constants.AppConstants.TIME_FORMAT_PATTERN
import dev.lucascosta.awslocalmanager.data.model.aws.AwsResourceDefinition
import dev.lucascosta.awslocalmanager.data.model.aws.ResourceCreationResult
import dev.lucascosta.awslocalmanager.data.model.process.ProcessConfig
import dev.lucascosta.awslocalmanager.data.model.resources.DynamoDbResource
import dev.lucascosta.awslocalmanager.data.model.resources.S3Resource
import dev.lucascosta.awslocalmanager.data.model.resources.SnsResource
import dev.lucascosta.awslocalmanager.data.model.resources.SqsResource
import dev.lucascosta.awslocalmanager.data.remote.AwsCommands
import dev.lucascosta.awslocalmanager.data.remote.EmulatorDefaults
import dev.lucascosta.awslocalmanager.data.remote.ProcessRunner
import dev.lucascosta.awslocalmanager.data.repository.PreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.time.Duration.Companion.milliseconds

class QuickViewModel(
    private val preferencesRepository: PreferencesRepository,
) : BaseViewModel() {
    private val _state = MutableStateFlow(QuickUiState())
    val state: StateFlow<QuickUiState> = _state.asStateFlow()

    private val timeFormatter = DateTimeFormatter.ofPattern(TIME_FORMAT_PATTERN)

    fun setType(type: AwsResourceDefinition) {
        _state.update { it.copy(selectedType = type) }
    }

    fun setName(name: String) {
        _state.update { it.copy(resourceName = name) }
    }

    fun setCreateDlq(enabled: Boolean) {
        _state.update { it.copy(createDlq = enabled) }
    }

    fun setDlqMaxReceiveCount(count: Int) {
        _state.update { it.copy(dlqMaxReceiveCount = count) }
    }

    fun setPartitionKey(key: String) {
        _state.update { it.copy(partitionKey = key) }
    }

    fun setPartitionKeyType(type: DynamoKeyType) {
        _state.update { it.copy(partitionKeyType = type) }
    }

    fun create() {
        val currentState = _state.value
        if (currentState.resourceName.isBlank() || currentState.isCreating) return
        scope.launch {
            val endpoint = preferencesRepository.preferences.first().endpoint
            _state.update { it.copy(isCreating = true) }
            val env = ProcessRunner.awsEnvVars(endpoint)
            val timestamp = LocalTime.now().format(timeFormatter)
            val results: List<ResourceCreationResult> =
                withContext(Dispatchers.IO) {
                    runCatching { dispatchCreate(currentState, env) }
                        .getOrElse { listOf(ResourceCreationResult(currentState.resourceName, false)) }
                }
            val newItems =
                results.map { (name, success) ->
                    QuickHistoryItem(
                        timestamp = timestamp,
                        type = currentState.selectedType,
                        name = name,
                        success = success,
                    )
                }
            _state.update { it.copy(isCreating = false, history = newItems + it.history) }
        }
    }

    private suspend fun dispatchCreate(
        state: QuickUiState,
        env: Map<String, String>,
    ): List<ResourceCreationResult> =
        when (state.selectedType) {
            SqsResource -> if (state.createDlq) createSqsWithDlq(state, env) else createSqsSimple(state.resourceName, env)
            SnsResource -> listOf(ResourceCreationResult(state.resourceName, runCommand(AwsCommands.createSns(state.resourceName), env)))
            S3Resource -> listOf(ResourceCreationResult(state.resourceName, runCommand(AwsCommands.createS3(state.resourceName), env)))
            DynamoDbResource -> listOf(ResourceCreationResult(state.resourceName, createDynamoDB(state, env)))
            else -> listOf(ResourceCreationResult(state.resourceName, false))
        }

    private suspend fun createSqsWithDlq(
        state: QuickUiState,
        env: Map<String, String>,
    ): List<ResourceCreationResult> {
        val dlqName = "${state.resourceName}$DLQ_SUFFIX"
        val dlqSuccess = runCommand(AwsCommands.createSqs(dlqName), env)
        if (!dlqSuccess) return listOf(ResourceCreationResult(dlqName, false))

        delay(SQS_DLQ_CREATION_DELAY_MS.milliseconds)

        val attributes = buildRedriveAttributes(dlqName, state.dlqMaxReceiveCount)
        val mainSuccess = runCommand(AwsCommands.createSqs(state.resourceName, attributes), env)
        return listOf(ResourceCreationResult(dlqName, dlqSuccess), ResourceCreationResult(state.resourceName, mainSuccess))
    }

    private suspend fun createSqsSimple(
        name: String,
        env: Map<String, String>,
    ): List<ResourceCreationResult> = listOf(ResourceCreationResult(name, runCommand(AwsCommands.createSqs(name), env)))

    private suspend fun createDynamoDB(
        state: QuickUiState,
        env: Map<String, String>,
    ): Boolean =
        runCommand(
            AwsCommands.createDynamoDb(state.resourceName, state.partitionKey, state.partitionKeyType.awsValue),
            env,
        )

    private suspend fun runCommand(
        command: List<String>,
        env: Map<String, String>,
    ): Boolean = ProcessRunner.run(command, ProcessConfig(envVars = env)).getOrElse { return false }.exitCode == 0

    private fun buildRedriveAttributes(
        dlqName: String,
        maxReceiveCount: Int,
    ): String {
        val dlqArn = EmulatorDefaults.sqsArn(dlqName)
        val redrivePolicy =
            buildJsonObject {
                put(SQS_DLQ_TARGET_ARN_KEY, dlqArn)
                put(SQS_MAX_RECEIVE_COUNT_KEY, maxReceiveCount)
            }.toString()
        return buildJsonObject {
            put(SQS_REDRIVE_POLICY_ATTR, redrivePolicy)
        }.toString()
    }
}
