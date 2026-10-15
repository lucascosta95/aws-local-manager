package dev.lucascosta.awslocalmanager.data.remote

import dev.lucascosta.awslocalmanager.constants.AppConstants.EMPTY_STRING
import dev.lucascosta.awslocalmanager.constants.AppConstants.USER_HOME
import dev.lucascosta.awslocalmanager.domain.AppLogger
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

// A macOS app launched from Finder gets launchd's bare PATH, so a Homebrew install is invisible to it.
object CommandLocator {
    private const val LOG_SOURCE = "CommandLocator"
    private const val PATH_VARIABLE = "PATH"
    private const val SHELL_VARIABLE = "SHELL"
    private const val SHELL_PATH_MARKER = "__AWS_LOCAL_MANAGER_PATH__"
    private const val SHELL_TIMEOUT_SECONDS = 5L

    private val homeDir: String get() = System.getProperty(USER_HOME) ?: EMPTY_STRING

    private val wellKnownDirs: List<String>
        get() =
            listOf(
                "/opt/homebrew/bin",
                "/opt/homebrew/sbin",
                "/usr/local/bin",
                "/usr/local/sbin",
                "$homeDir/.docker/bin",
                "$homeDir/.rd/bin",
                "$homeDir/.colima/bin",
                "$homeDir/.local/bin",
                "/snap/bin",
            )

    private val inheritedDirs: List<String>
        get() = System.getenv(PATH_VARIABLE).orEmpty().split(File.pathSeparator).filter { it.isNotBlank() }

    // The user's terminal PATH, so a CLI installed outside the well-known dirs is still found.
    private val loginShellDirs: List<String> by lazy { readLoginShellPath() }

    private val resolved = ConcurrentHashMap<String, String>()

    fun resolve(command: String): String {
        if (command.contains(File.separatorChar)) {
            return command
        }
        return resolved.getOrPut(command) { search(command) ?: command }
    }

    fun searchPath(): String = searchDirs().joinToString(File.pathSeparator)

    private fun searchDirs(): List<String> = (inheritedDirs + wellKnownDirs + loginShellDirs).distinct()

    private fun search(command: String): String? {
        val candidates =
            searchDirs()
                .map { File(it, command) }
                .filter { it.isFile && it.canExecute() }

        // A binary built only for another CPU fails with "Bad CPU type" (e.g. x86_64 on Apple Silicon without
        // Rosetta), so a native one wins; the first match is still kept for setups where emulation works.
        val candidate = candidates.firstOrNull { ExecutableArchitecture.runsNatively(it) } ?: candidates.firstOrNull()

        if (candidate == null) {
            AppLogger.warn(LOG_SOURCE, "$command not found in PATH or in any known install location")
        } else {
            val skipped = candidates.takeWhile { it != candidate }
            if (skipped.isNotEmpty()) {
                AppLogger.info(
                    LOG_SOURCE,
                    "$command skipped ${skipped.joinToString { it.absolutePath }}: not built for this CPU",
                )
            }
            AppLogger.debug(LOG_SOURCE, "$command resolved to ${candidate.absolutePath}")
        }
        return candidate?.absolutePath
    }

    private fun readLoginShellPath(): List<String> {
        val shell = System.getenv(SHELL_VARIABLE)?.takeIf { File(it).canExecute() } ?: return emptyList()
        return runCatching {
            val process =
                ProcessBuilder(shell, "-ilc", "printf '$SHELL_PATH_MARKER%s$SHELL_PATH_MARKER' \"\$PATH\"")
                    .redirectInput(ProcessBuilder.Redirect.from(File("/dev/null")))
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start()
            if (!process.waitFor(SHELL_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly()
                error("$shell timed out after ${SHELL_TIMEOUT_SECONDS}s")
            }
            val output = process.inputStream.bufferedReader().readText()
            output
                .substringAfter(SHELL_PATH_MARKER, EMPTY_STRING)
                .substringBefore(SHELL_PATH_MARKER)
                .split(File.pathSeparator)
                .filter { it.isNotBlank() }
        }.onFailure { failure -> AppLogger.debug(LOG_SOURCE, "Could not read the login shell PATH: ${failure.message}") }
            .getOrDefault(emptyList())
    }
}
