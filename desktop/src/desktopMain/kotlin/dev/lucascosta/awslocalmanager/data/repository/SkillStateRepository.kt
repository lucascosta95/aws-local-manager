package dev.lucascosta.awslocalmanager.data.repository

import dev.lucascosta.awslocalmanager.constants.AppConstants.APP_DATA_DIR_NAME
import dev.lucascosta.awslocalmanager.constants.AppConstants.SKILLS_STATE_FILENAME
import dev.lucascosta.awslocalmanager.constants.AppConstants.USER_HOME
import dev.lucascosta.awslocalmanager.data.model.skill.InstalledSkill
import dev.lucascosta.awslocalmanager.data.model.skill.SkillsState
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/** Tracks which skills are installed, in which tool and at which version. */
class SkillStateRepository {
    private val json =
        Json {
            prettyPrint = true
            ignoreUnknownKeys = true
        }
    private val mutex = Mutex()
    private val stateFile = File(System.getProperty(USER_HOME), "$APP_DATA_DIR_NAME/$SKILLS_STATE_FILENAME")

    fun load(): SkillsState =
        runCatching {
            if (stateFile.exists()) json.decodeFromString<SkillsState>(stateFile.readText()) else SkillsState()
        }.getOrElse { SkillsState() }

    suspend fun record(entry: InstalledSkill): SkillsState =
        save { current ->
            current.filterNot { it.skillId == entry.skillId && it.targetId == entry.targetId } + entry
        }

    suspend fun forget(
        skillId: String,
        targetId: String,
    ): SkillsState = save { current -> current.filterNot { it.skillId == skillId && it.targetId == targetId } }

    private suspend fun save(transform: (List<InstalledSkill>) -> List<InstalledSkill>): SkillsState =
        mutex.withLock {
            val updated = SkillsState(transform(load().installed))
            runCatching {
                stateFile.parentFile?.mkdirs()
                stateFile.writeText(json.encodeToString(updated))
            }
            updated
        }
}
