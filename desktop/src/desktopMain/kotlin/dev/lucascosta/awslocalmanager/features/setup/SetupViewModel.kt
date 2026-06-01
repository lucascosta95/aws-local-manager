package dev.lucascosta.awslocalmanager.features.setup

import dev.lucascosta.awslocalmanager.BaseViewModel
import dev.lucascosta.awslocalmanager.constants.AppConstants.COLIMA_COMMAND
import dev.lucascosta.awslocalmanager.constants.AppConstants.DOCKER_READY_POLL_INTERVAL_MS
import dev.lucascosta.awslocalmanager.constants.AppConstants.DOCKER_READY_POLL_MAX_ATTEMPTS
import dev.lucascosta.awslocalmanager.constants.AppConstants.DOCKER_RUN_FAILED_MSG
import dev.lucascosta.awslocalmanager.constants.AppConstants.DOCKER_SOCKET_BINDING
import dev.lucascosta.awslocalmanager.constants.AppConstants.EMPTY_STRING
import dev.lucascosta.awslocalmanager.constants.AppConstants.EMULATOR_CONTAINER_NAME
import dev.lucascosta.awslocalmanager.constants.AppConstants.EMULATOR_PORT_MAPPING
import dev.lucascosta.awslocalmanager.constants.AppConstants.EMULATOR_READY_POLL_MAX_ATTEMPTS
import dev.lucascosta.awslocalmanager.constants.AppConstants.FIX_SETTLE_DELAY_MS
import dev.lucascosta.awslocalmanager.constants.AppConstants.FLOCI_IMAGE
import dev.lucascosta.awslocalmanager.constants.AppConstants.OS_MAC_IDENTIFIER
import dev.lucascosta.awslocalmanager.constants.AppConstants.OS_NAME_PROPERTY
import dev.lucascosta.awslocalmanager.data.model.health.CheckStatus
import dev.lucascosta.awslocalmanager.data.model.health.PrerequisiteCheck
import dev.lucascosta.awslocalmanager.data.remote.EmulatorClient
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
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class SetupViewModel(
    private val emulatorClient: EmulatorClient,
    private val preferencesRepository: PreferencesRepository,
) : BaseViewModel() {
    internal companion object {
        const val ID_DOCKER_INSTALLED = "docker_installed"
        const val ID_DOCKER_RUNNING = "docker_running"
        const val ID_EMULATOR_IMAGE = "emulator_image"
        const val ID_EMULATOR_RUNNING = "emulator_running"
        const val ID_AWS_CLI = "aws_cli"
    }

    private val _state = MutableStateFlow(SetupUiState(checks = initialChecks()))
    val state: StateFlow<SetupUiState> = _state.asStateFlow()

    init {
        runChecks()
    }

    fun runChecks(resetAll: Boolean = true) {
        scope.launch(Dispatchers.IO) {
            resetCheckStates(resetAll)
            val dockerInstalled = checkAndUpdateDockerInstalled()
            val dockerRunning = checkAndUpdateDockerRunning(dockerInstalled)
            val imagePresent = checkAndUpdateEmulatorImage(dockerRunning)
            checkAndUpdateEmulatorRunning(imagePresent)
            checkAndUpdateAwsCli()
            _state.update { it.copy(isChecking = false, allOk = it.checks.all { check -> check.status == CheckStatus.OK }) }
        }
    }

    fun fixItem(checkId: String) {
        scope.launch(Dispatchers.IO) {
            clearFixLog()
            updateCheck(checkId) { it.copy(isFixing = true, status = CheckStatus.CHECKING, detail = null) }
            val needsRecheck =
                when (checkId) {
                    ID_EMULATOR_IMAGE -> {
                        fixEmulatorImage()
                        true
                    }

                    ID_EMULATOR_RUNNING -> fixEmulatorRunning()
                    else -> {
                        fixWithCommand(checkId)
                        true
                    }
                }
            if (needsRecheck) {
                runChecks(resetAll = false)
            }
        }
    }

    private fun initialChecks(): List<PrerequisiteCheck> =
        listOf(
            PrerequisiteCheck(ID_DOCKER_INSTALLED, EMPTY_STRING, CheckStatus.CHECKING, null, canAutoFix = false),
            PrerequisiteCheck(ID_DOCKER_RUNNING, EMPTY_STRING, CheckStatus.CHECKING, null, canAutoFix = true),
            PrerequisiteCheck(ID_EMULATOR_IMAGE, EMPTY_STRING, CheckStatus.CHECKING, null, canAutoFix = true),
            PrerequisiteCheck(ID_EMULATOR_RUNNING, EMPTY_STRING, CheckStatus.CHECKING, null, canAutoFix = true),
            PrerequisiteCheck(ID_AWS_CLI, EMPTY_STRING, CheckStatus.CHECKING, null, canAutoFix = false),
        )

    private fun resetCheckStates(resetAll: Boolean) {
        _state.update { state ->
            state.copy(
                isChecking = true,
                allOk = false,
                fixLogLines = emptyList(),
                checks =
                    state.checks.map { check ->
                        if (resetAll || check.status != CheckStatus.OK) {
                            check.copy(status = CheckStatus.CHECKING, detail = null, isFixing = false)
                        } else {
                            check
                        }
                    },
            )
        }
    }

    private suspend fun checkAndUpdateDockerInstalled(): Boolean {
        val installed = checkCommand(listOf("docker", "--version"))
        updateCheck(ID_DOCKER_INSTALLED) {
            it.copy(status = if (installed) CheckStatus.OK else CheckStatus.MISSING)
        }
        return installed
    }

    private suspend fun checkAndUpdateDockerRunning(dockerInstalled: Boolean): Boolean {
        val running = dockerInstalled && checkCommand(listOf("docker", "info"))
        updateCheck(ID_DOCKER_RUNNING) {
            it.copy(
                status =
                    when {
                        !dockerInstalled -> CheckStatus.UNKNOWN
                        running -> CheckStatus.OK
                        else -> CheckStatus.NOT_RUNNING
                    },
                canAutoFix = dockerInstalled,
            )
        }
        return running
    }

    private suspend fun checkAndUpdateEmulatorImage(dockerRunning: Boolean): Boolean {
        val present =
            dockerRunning &&
                checkCommandOutputNotEmpty(
                    listOf("docker", "images", "-q", FLOCI_IMAGE),
                )

        updateCheck(ID_EMULATOR_IMAGE) {
            it.copy(
                status =
                    when {
                        !dockerRunning -> CheckStatus.UNKNOWN
                        present -> CheckStatus.OK
                        else -> CheckStatus.NOT_RUNNING
                    },
                canAutoFix = dockerRunning,
            )
        }
        return present
    }

    private suspend fun checkAndUpdateEmulatorRunning(imagePresent: Boolean) {
        val endpoint = currentEndpoint()
        val running = imagePresent && emulatorClient.isReachable(endpoint)

        updateCheck(ID_EMULATOR_RUNNING) {
            it.copy(
                status =
                    when {
                        !imagePresent -> CheckStatus.UNKNOWN
                        running -> CheckStatus.OK
                        else -> CheckStatus.NOT_RUNNING
                    },
                canAutoFix = imagePresent,
            )
        }
    }

    private suspend fun checkAndUpdateAwsCli() {
        val installed = checkCommand(listOf("aws", "--version"))
        updateCheck(ID_AWS_CLI) { it.copy(status = if (installed) CheckStatus.OK else CheckStatus.MISSING) }
    }

    private suspend fun fixEmulatorImage() {
        updateCheck(ID_EMULATOR_IMAGE) { it.copy(isFixing = true) }
        val pullCmd = listOf("docker", "pull", FLOCI_IMAGE)
        var lastLine = EMPTY_STRING

        ProcessRunner.runStreaming(pullCmd).collect { processLine ->
            appendFixLog(processLine.text)
            lastLine = processLine.text
        }

        val imagePresent = checkCommandOutputNotEmpty(listOf("docker", "images", "-q", FLOCI_IMAGE))
        if (!imagePresent) {
            updateCheck(ID_EMULATOR_IMAGE) { it.copy(isFixing = false, status = CheckStatus.NOT_RUNNING, detail = lastLine) }
        } else {
            updateCheck(ID_EMULATOR_IMAGE) { it.copy(isFixing = false, status = CheckStatus.OK) }
        }
    }

    private suspend fun fixEmulatorRunning(): Boolean {
        val imagePresent = checkCommandOutputNotEmpty(listOf("docker", "images", "-q", FLOCI_IMAGE))
        if (!imagePresent) {
            updateCheck(ID_EMULATOR_RUNNING) { it.copy(isFixing = false, status = CheckStatus.UNKNOWN) }
            return false
        }

        removeExistingContainerIfPresent()

        val runError = startEmulatorContainer()
        return if (runError != null) {
            updateCheck(ID_EMULATOR_RUNNING) { it.copy(isFixing = false, status = CheckStatus.ERROR, detail = runError) }
            false
        } else if (!pollUntilEmulatorReady()) {
            updateCheck(ID_EMULATOR_RUNNING) { it.copy(isFixing = false, status = CheckStatus.ERROR) }
            false
        } else {
            true
        }
    }

    private suspend fun removeExistingContainerIfPresent() {
        val exists = checkCommandOutputNotEmpty(listOf("docker", "ps", "-aq", "-f", "name=$EMULATOR_CONTAINER_NAME"))
        if (exists) {
            appendFixLog("Removing existing container $EMULATOR_CONTAINER_NAME...")
            ProcessRunner.run(listOf("docker", "rm", "-f", EMULATOR_CONTAINER_NAME))
        }
    }

    private suspend fun startEmulatorContainer(): String? {
        val command =
            listOf(
                "docker",
                "run",
                "-d",
                "--name",
                EMULATOR_CONTAINER_NAME,
                "-p",
                EMULATOR_PORT_MAPPING,
                "-v",
                DOCKER_SOCKET_BINDING,
                FLOCI_IMAGE,
            )

        var lastLine = EMPTY_STRING
        ProcessRunner.runStreaming(command).collect { line ->
            appendFixLog(line.text)
            lastLine = line.text
        }

        val running = checkCommandOutputNotEmpty(listOf("docker", "ps", "-q", "-f", "name=$EMULATOR_CONTAINER_NAME"))
        return if (running) null else lastLine.ifBlank { DOCKER_RUN_FAILED_MSG }
    }

    private suspend fun pollUntilEmulatorReady(): Boolean {
        val endpoint = currentEndpoint()
        repeat(EMULATOR_READY_POLL_MAX_ATTEMPTS) {
            delay(1.seconds)
            if (emulatorClient.isReachable(endpoint)) return true
        }
        return false
    }

    private suspend fun fixWithCommand(checkId: String) {
        buildFixCommand(checkId)?.let { ProcessRunner.run(it) }
        waitForFixCompletion(checkId)
    }

    private fun buildFixCommand(checkId: String): List<String>? {
        val isMac = System.getProperty(OS_NAME_PROPERTY)?.lowercase()?.contains(OS_MAC_IDENTIFIER) ?: false
        return when (checkId) {
            ID_DOCKER_RUNNING -> buildDockerStartCommand(isMac)
            else -> null
        }
    }

    private fun buildDockerStartCommand(isMac: Boolean): List<String> =
        if (isMac) {
            listOf(COLIMA_COMMAND, "start")
        } else {
            listOf("sh", "-c", "systemctl start docker || sudo systemctl start docker")
        }

    private suspend fun waitForFixCompletion(checkId: String) {
        when (checkId) {
            ID_DOCKER_RUNNING -> waitForDockerReady()
            else -> delay(FIX_SETTLE_DELAY_MS.milliseconds)
        }
    }

    private suspend fun waitForDockerReady() {
        repeat(DOCKER_READY_POLL_MAX_ATTEMPTS) {
            delay(DOCKER_READY_POLL_INTERVAL_MS.milliseconds)
            if (ProcessRunner.run(listOf("docker", "info")).getOrNull()?.exitCode == 0) return
        }
    }

    private fun updateCheck(
        id: String,
        transform: (PrerequisiteCheck) -> PrerequisiteCheck,
    ) {
        _state.update { state ->
            state.copy(checks = state.checks.map { check -> if (check.id == id) transform(check) else check })
        }
    }

    private fun clearFixLog() {
        _state.update { it.copy(fixLogLines = emptyList()) }
    }

    private fun appendFixLog(line: String) {
        _state.update { it.copy(fixLogLines = it.fixLogLines + line) }
    }

    private suspend fun currentEndpoint(): String =
        withContext(Dispatchers.IO) {
            preferencesRepository.preferences.first().endpoint
        }

    private suspend fun checkCommand(command: List<String>): Boolean =
        withContext(Dispatchers.IO) {
            runCatching { ProcessRunner.run(command).getOrThrow().exitCode == 0 }.getOrElse { false }
        }

    private suspend fun checkCommandOutputNotEmpty(command: List<String>): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                val result = ProcessRunner.run(command).getOrThrow()
                result.exitCode == 0 && result.stdout.isNotBlank()
            }.getOrElse { false }
        }
}
