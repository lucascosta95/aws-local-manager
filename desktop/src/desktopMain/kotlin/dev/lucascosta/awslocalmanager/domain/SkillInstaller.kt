package dev.lucascosta.awslocalmanager.domain

import dev.lucascosta.awslocalmanager.constants.AppConstants.EMPTY_STRING
import dev.lucascosta.awslocalmanager.constants.AppConstants.SKILL_BACKUP_SUFFIX
import dev.lucascosta.awslocalmanager.constants.AppConstants.SKILL_BLOCK_NAMESPACE
import dev.lucascosta.awslocalmanager.constants.AppConstants.SKILL_MANIFEST_FILENAME
import dev.lucascosta.awslocalmanager.constants.AppConstants.USER_HOME
import dev.lucascosta.awslocalmanager.data.model.skill.AgentTarget
import dev.lucascosta.awslocalmanager.data.model.skill.LegacyInstallMode
import dev.lucascosta.awslocalmanager.data.model.skill.SkillCatalogEntry
import java.io.File

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
    ): File = File(homeDir, "${target.skillsPath}/${entry.id}/$SKILL_MANIFEST_FILENAME")

    fun invocation(
        entry: SkillCatalogEntry,
        target: AgentTarget,
    ): String = target.invocation.replace("{skill}", entry.id)

    fun isInstalled(
        entry: SkillCatalogEntry,
        target: AgentTarget,
    ): Boolean = installPath(entry, target).isFile

    fun legacyInstall(
        entry: SkillCatalogEntry,
        target: AgentTarget,
    ): File? {
        val legacy = target.legacy ?: return null
        return when (legacy.mode) {
            LegacyInstallMode.RULE_FILE ->
                File(homeDir, "${legacy.relativePath}/${entry.id}.${legacy.fileExtension}").takeIf { it.isFile }

            LegacyInstallMode.INSTRUCTION_FILE ->
                File(homeDir, legacy.relativePath).takeIf { file ->
                    runCatching { file.isFile && file.readText().contains(beginMarker(entry.id)) }.getOrDefault(false)
                }
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
            file.writeText(manifest(entry, content))
            removeLegacy(entry, target)
            file
        }

    fun uninstall(
        entry: SkillCatalogEntry,
        target: AgentTarget,
    ): Result<File> =
        runCatching {
            val file = installPath(entry, target)
            file.parentFile?.deleteRecursively()
            removeLegacy(entry, target)
            file
        }

    private fun manifest(
        entry: SkillCatalogEntry,
        content: String,
    ): String {
        val body = content.trim()
        if (body.startsWith(FRONTMATTER_FENCE)) {
            return body + "\n"
        }
        return buildString {
            append("$FRONTMATTER_FENCE\n")
            append("name: ${entry.id}\n")
            append("description: ${entry.description.replace('\n', ' ')}\n")
            append("$FRONTMATTER_FENCE\n\n")
            append(body)
            append("\n")
        }
    }

    private fun removeLegacy(
        entry: SkillCatalogEntry,
        target: AgentTarget,
    ) {
        val legacy = target.legacy ?: return
        when (legacy.mode) {
            LegacyInstallMode.RULE_FILE ->
                File(homeDir, "${legacy.relativePath}/${entry.id}.${legacy.fileExtension}").delete()

            LegacyInstallMode.INSTRUCTION_FILE ->
                removeBlock(File(homeDir, legacy.relativePath), entry)
        }
    }

    private fun removeBlock(
        file: File,
        entry: SkillCatalogEntry,
    ) {
        if (!file.isFile) {
            return
        }
        val existing = file.readText()
        if (!existing.contains(beginMarker(entry.id))) {
            return
        }
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
}
