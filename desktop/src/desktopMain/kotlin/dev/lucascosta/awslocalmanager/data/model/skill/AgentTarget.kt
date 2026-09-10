package dev.lucascosta.awslocalmanager.data.model.skill

/**
 * How a skill is written into an agent tool.
 *
 * [SKILL_DIRECTORY] one folder per skill, holding a SKILL.md with its original frontmatter.
 * [RULE_FILE] one rule file per skill inside a shared rules folder.
 * [INSTRUCTION_FILE] a single instruction file shared by every skill, where each one lives in a delimited block.
 */
enum class SkillInstallMode { SKILL_DIRECTORY, RULE_FILE, INSTRUCTION_FILE }

data class AgentTarget(
    val id: String,
    val displayName: String,
    val mode: SkillInstallMode,
    val relativePath: String,
    val detectionPath: String,
    val fileExtension: String = "md",
)

object AgentTargetRegistry {
    val all: List<AgentTarget> =
        listOf(
            AgentTarget(
                id = "claude-code",
                displayName = "Claude Code",
                mode = SkillInstallMode.SKILL_DIRECTORY,
                relativePath = ".claude/skills",
                detectionPath = ".claude",
            ),
            AgentTarget(
                id = "cursor",
                displayName = "Cursor",
                mode = SkillInstallMode.RULE_FILE,
                relativePath = ".cursor/rules",
                detectionPath = ".cursor",
                fileExtension = "mdc",
            ),
            AgentTarget(
                id = "codex",
                displayName = "Codex CLI",
                mode = SkillInstallMode.INSTRUCTION_FILE,
                relativePath = ".codex/AGENTS.md",
                detectionPath = ".codex",
            ),
            AgentTarget(
                id = "gemini-cli",
                displayName = "Gemini CLI",
                mode = SkillInstallMode.INSTRUCTION_FILE,
                relativePath = ".gemini/GEMINI.md",
                detectionPath = ".gemini",
            ),
        )

    fun byId(id: String): AgentTarget? = all.find { it.id == id }
}
