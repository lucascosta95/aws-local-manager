package dev.lucascosta.awslocalmanager.domain

import dev.lucascosta.awslocalmanager.constants.AppConstants.EMPTY_STRING
import dev.lucascosta.awslocalmanager.constants.AppConstants.SKILL_BACKUP_SUFFIX
import dev.lucascosta.awslocalmanager.constants.AppConstants.SKILL_BLOCK_NAMESPACE
import dev.lucascosta.awslocalmanager.constants.AppConstants.SKILL_MANIFEST_FILENAME
import dev.lucascosta.awslocalmanager.constants.AppConstants.USER_HOME
import dev.lucascosta.awslocalmanager.data.model.skill.AgentTarget
import dev.lucascosta.awslocalmanager.data.model.skill.SkillCatalogEntry
import dev.lucascosta.awslocalmanager.data.model.skill.SkillInstallMode
import java.io.File

/**
 * Writes skills into the agent tools installed for the current user.
 *
 * Everything it touches lives under the user home. Shared instruction files are edited through a
 * delimited block, so reinstalling replaces only that block and the rest of the file is preserved.
 */
class SkillInstaller(
    private val homeDir: File = File(System.getProperty(USER_HOME) ?: EMPTY_STRING),
) {
    private companion object {
        const val FRONTMATTER_FENCE = "---"

        fun beginMarker(skillId: String) = "<!-- $SKILL_BLOCK_NAMESPACE:$skillId:begin"

        fun endMarker(skillId: String) = "<!-- $SKILL_BLOCK_NAMESPACE:$skillId:end -->"

        fun blockRegex(skillId: String) =
            Regex(
                Regex.escape(beginMarker(skillId)) + ".*?" + Regex.escape(endMarker(skillId)) + "\\n?",
                RegexOption.DOT_MATCHES_ALL,
            )
    }

    fun isDetected(target: AgentTarget): Boolean = File(homeDir, target.detectionPath).exists()

    fun installPath(
        entry: SkillCatalogEntry,
        target: AgentTarget,
    ): File =
        when (target.mode) {
            SkillInstallMode.SKILL_DIRECTORY -> File(homeDir, "${target.relativePath}/${entry.id}/$SKILL_MANIFEST_FILENAME")
            SkillInstallMode.RULE_FILE -> File(homeDir, "${target.relativePath}/${entry.id}.${target.fileExtension}")
            SkillInstallMode.INSTRUCTION_FILE -> File(homeDir, target.relativePath)
        }

    fun isInstalled(
        entry: SkillCatalogEntry,
        target: AgentTarget,
    ): Boolean {
        val file = installPath(entry, target)
        if (!file.isFile) {
            return false
        }
        return when (target.mode) {
            SkillInstallMode.INSTRUCTION_FILE -> file.readText().contains(beginMarker(entry.id))
            else -> true
        }
    }

    fun install(
        entry: SkillCatalogEntry,
        target: AgentTarget,
        content: String,
    ): Result<File> =
        runCatching {
            val file = installPath(entry, target)
            file.parentFile?.mkdirs()
            when (target.mode) {
                SkillInstallMode.SKILL_DIRECTORY -> file.writeText(content)
                SkillInstallMode.RULE_FILE -> file.writeText(ruleFileContent(entry, content))
                SkillInstallMode.INSTRUCTION_FILE -> writeBlock(file, entry, content)
            }
            file
        }

    fun uninstall(
        entry: SkillCatalogEntry,
        target: AgentTarget,
    ): Result<File> =
        runCatching {
            val file = installPath(entry, target)
            when (target.mode) {
                SkillInstallMode.SKILL_DIRECTORY -> file.parentFile?.deleteRecursively()
                SkillInstallMode.RULE_FILE -> file.delete()
                SkillInstallMode.INSTRUCTION_FILE -> removeBlock(file, entry)
            }
            file
        }

    private fun writeBlock(
        file: File,
        entry: SkillCatalogEntry,
        content: String,
    ) {
        val existing = if (file.isFile) file.readText() else EMPTY_STRING
        backup(file, existing)
        val stripped = blockRegex(entry.id).replace(existing, EMPTY_STRING).trimEnd()
        val block =
            buildString {
                append(beginMarker(entry.id))
                append(" (v${entry.version}) -->\n")
                append("## ${entry.name}\n\n")
                append(stripFrontmatter(content).trim())
                append("\n")
                append(endMarker(entry.id))
                append("\n")
            }
        val separator = if (stripped.isEmpty()) EMPTY_STRING else "\n\n"
        file.writeText(stripped + separator + block)
    }

    private fun removeBlock(
        file: File,
        entry: SkillCatalogEntry,
    ) {
        if (!file.isFile) {
            return
        }
        val existing = file.readText()
        backup(file, existing)
        val cleaned = blockRegex(entry.id).replace(existing, EMPTY_STRING).trimEnd()
        file.writeText(if (cleaned.isEmpty()) EMPTY_STRING else cleaned + "\n")
    }

    private fun backup(
        file: File,
        existing: String,
    ) {
        if (existing.isNotEmpty()) {
            File(file.parentFile, file.name + SKILL_BACKUP_SUFFIX).writeText(existing)
        }
    }

    private fun ruleFileContent(
        entry: SkillCatalogEntry,
        content: String,
    ): String =
        buildString {
            append("$FRONTMATTER_FENCE\n")
            append("description: ${entry.description}\n")
            append("alwaysApply: false\n")
            append("$FRONTMATTER_FENCE\n\n")
            append("# ${entry.name}\n\n")
            append(stripFrontmatter(content).trim())
            append("\n")
        }

    private fun stripFrontmatter(content: String): String {
        val trimmed = content.trimStart()
        if (!trimmed.startsWith(FRONTMATTER_FENCE)) {
            return content
        }
        val afterOpening = trimmed.removePrefix(FRONTMATTER_FENCE)
        val closingIndex = afterOpening.indexOf("\n$FRONTMATTER_FENCE")
        if (closingIndex == -1) {
            return content
        }
        return afterOpening
            .substring(closingIndex)
            .removePrefix("\n$FRONTMATTER_FENCE")
            .trimStart()
    }
}
