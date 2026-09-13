package dev.lucascosta.awslocalmanager.data.model.skill

enum class LegacyInstallMode { RULE_FILE, INSTRUCTION_FILE }

data class LegacyInstall(
    val mode: LegacyInstallMode,
    val relativePath: String,
    val fileExtension: String = "md",
)

data class AgentTarget(
    val id: String,
    val displayName: String,
    val skillsPath: String,
    val detectionPath: String,
    val invocation: String,
    val legacy: LegacyInstall? = null,
)

object AgentTargetRegistry {
    val all: List<AgentTarget> =
        listOf(
            AgentTarget(
                id = "claude-code",
                displayName = "Claude Code",
                skillsPath = ".claude/skills",
                detectionPath = ".claude",
                invocation = "/{skill}",
            ),
            AgentTarget(
                id = "cursor",
                displayName = "Cursor",
                skillsPath = ".cursor/skills",
                detectionPath = ".cursor",
                invocation = "/{skill}",
                legacy = LegacyInstall(LegacyInstallMode.RULE_FILE, ".cursor/rules", "mdc"),
            ),
            AgentTarget(
                id = "codex",
                displayName = "Codex CLI",
                skillsPath = ".codex/skills",
                detectionPath = ".codex",
                invocation = "\${skill}",
                legacy = LegacyInstall(LegacyInstallMode.INSTRUCTION_FILE, ".codex/AGENTS.md"),
            ),
            AgentTarget(
                id = "gemini-cli",
                displayName = "Gemini CLI",
                skillsPath = ".gemini/skills",
                detectionPath = ".gemini",
                invocation = "/skills",
                legacy = LegacyInstall(LegacyInstallMode.INSTRUCTION_FILE, ".gemini/GEMINI.md"),
            ),
        )

    fun byId(id: String): AgentTarget? = all.find { it.id == id }
}
