package dev.lucascosta.awslocalmanager.util

fun String.stripMarkdown(): String =
    this
        .replace(Regex("#{1,6}\\s+"), "")
        .replace(Regex("\\*\\*(.+?)\\*\\*"), "$1")
        .replace(Regex("\\*(.+?)\\*"), "$1")
        .replace(Regex("^[-*]\\s+", RegexOption.MULTILINE), "• ")
        .replace(Regex("\\[(.+?)\\]\\(.+?\\)"), "$1")
        .replace(Regex("`(.+?)`"), "$1")
        .trim()
