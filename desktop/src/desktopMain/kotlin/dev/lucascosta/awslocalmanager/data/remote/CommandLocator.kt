package dev.lucascosta.awslocalmanager.data.remote

import dev.lucascosta.awslocalmanager.constants.AppConstants.EMPTY_STRING
import dev.lucascosta.awslocalmanager.constants.AppConstants.USER_HOME
import dev.lucascosta.awslocalmanager.domain.AppLogger
import java.io.File
import java.util.concurrent.ConcurrentHashMap

// A macOS app launched from Finder gets launchd's bare PATH, so a Homebrew install is invisible to it.
object CommandLocator {
    private const val LOG_SOURCE = "CommandLocator"
    private const val PATH_VARIABLE = "PATH"

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

    private val resolved = ConcurrentHashMap<String, String>()

    fun resolve(command: String): String {
        if (command.contains(File.separatorChar)) {
            return command
        }
        return resolved.getOrPut(command) { search(command) ?: command }
    }

    fun searchPath(): String {
        val inherited = System.getenv(PATH_VARIABLE).orEmpty().split(File.pathSeparator).filter { it.isNotBlank() }
        val extras = wellKnownDirs.filterNot { it in inherited }
        return (inherited + extras).joinToString(File.pathSeparator)
    }

    private fun search(command: String): String? {
        val inherited = System.getenv(PATH_VARIABLE).orEmpty().split(File.pathSeparator)
        val candidate =
            (inherited + wellKnownDirs)
                .asSequence()
                .filter { it.isNotBlank() }
                .map { File(it, command) }
                .firstOrNull { it.isFile && it.canExecute() }

        if (candidate == null) {
            AppLogger.warn(LOG_SOURCE, "$command not found in PATH or in any known install location")
        } else {
            AppLogger.debug(LOG_SOURCE, "$command resolved to ${candidate.absolutePath}")
        }
        return candidate?.absolutePath
    }
}
