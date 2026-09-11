package dev.lucascosta.awslocalmanager.data.remote

import dev.lucascosta.awslocalmanager.constants.AppConstants.EMPTY_STRING
import dev.lucascosta.awslocalmanager.constants.AppConstants.USER_HOME
import dev.lucascosta.awslocalmanager.domain.AppLogger
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Finds the external tools the app shells out to.
 *
 * A macOS app launched from Finder is started by launchd, not by a shell, so it inherits a bare
 * `PATH` of `/usr/bin:/bin:/usr/sbin:/sbin` and never reads `.zshrc`. Homebrew installs into
 * `/opt/homebrew/bin`, which is not on that list, so `docker`, `colima` and `aws` were reported as
 * missing in the packaged app while the same build launched from a terminal found all three.
 *
 * Every command is therefore resolved to an absolute path against the inherited `PATH` plus the
 * places these tools are actually installed, and the child process gets the widened `PATH` too, so
 * anything it spawns in turn can still be found.
 */
object CommandLocator {
    private const val LOG_SOURCE = "CommandLocator"
    private const val PATH_VARIABLE = "PATH"

    private val homeDir: String get() = System.getProperty(USER_HOME) ?: EMPTY_STRING

    /** Install locations that a shell would have added to `PATH` and launchd does not. */
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

    /**
     * The absolute path of [command], or [command] unchanged when nothing matches, so a genuinely
     * missing tool still fails the way it always did.
     */
    fun resolve(command: String): String {
        if (command.contains(File.separatorChar)) {
            return command
        }
        return resolved.getOrPut(command) { search(command) ?: command }
    }

    /** The inherited `PATH` widened with [wellKnownDirs], for the child process environment. */
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
