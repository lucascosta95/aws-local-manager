package dev.lucascosta.awslocalmanager.data.model.health

/**
 * [OUTDATED] is a working emulator on a version the app no longer supports: the image or the
 * container is there and may well answer, but it is not the one this release was built against.
 */
enum class CheckStatus { OK, MISSING, NOT_RUNNING, OUTDATED, UNKNOWN, CHECKING, ERROR }
