package dev.lucascosta.awslocalmanager.data.model.skill

/**
 * How an older release of the app used to write a skill, so installing again can clean it up.
 *
 * [RULE_FILE] one file per skill inside a shared rules folder.
 * [INSTRUCTION_FILE] a single instruction file shared by every skill, where each one lived in a delimited block.
 */
enum class LegacyInstallMode { RULE_FILE, INSTRUCTION_FILE }

data class LegacyInstall(
    val mode: LegacyInstallMode,
    val relativePath: String,
    val fileExtension: String = "md",
)

/**
 * An agent tool that reads the Agent Skills format.
 *
 * Every supported tool loads a skill from `<home>/<skillsPath>/<skill id>/SKILL.md`, keeps its
 * frontmatter and only pulls the body into the conversation when the skill is called. That is the
 * whole point of shipping a skill instead of a rule or a global instruction file: the project the
 * user opens decides when the instructions apply, and nothing is injected into unrelated chats.
 *
 * [invocation] is how the user calls it, with `{skill}` standing for the skill id.
 * [legacy] is where releases up to 1.2.0 wrote this skill, and is deleted on the next install.
 */
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
