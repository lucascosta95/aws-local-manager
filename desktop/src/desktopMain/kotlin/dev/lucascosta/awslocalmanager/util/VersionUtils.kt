package dev.lucascosta.awslocalmanager.util

fun isNewerVersion(current: String, latest: String): Boolean {
    val currentParts = current.split(".").map { it.toIntOrNull() ?: 0 }
    val latestParts = latest.split(".").map { it.toIntOrNull() ?: 0 }
    for (i in 0..2) {
        val c = currentParts.getOrElse(i) { 0 }
        val l = latestParts.getOrElse(i) { 0 }
        if (l > c) return true
        if (l < c) return false
    }
    return false
}
