package dev.lucascosta.awslocalmanager.util

import dev.lucascosta.awslocalmanager.constants.AppConstants.EMPTY_STRING
import dev.lucascosta.awslocalmanager.constants.AppConstants.USER_HOME

object PathUtils {
    fun expandTilde(path: String): String =
        if (path.startsWith("~")) {
            path.replaceFirst("~", System.getProperty(USER_HOME) ?: EMPTY_STRING)
        } else {
            path
        }
}
