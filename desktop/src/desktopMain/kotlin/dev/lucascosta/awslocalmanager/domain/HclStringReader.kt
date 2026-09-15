package dev.lucascosta.awslocalmanager.domain

import java.io.File

// Reads a string attribute written in any of the forms Terraform files commonly use for long text:
// a quoted string, a heredoc, or file() pointing next to the .tf files. jsonencode() is not evaluated.
object HclStringReader {
    private val modulePathPrefix = Regex("""^\$\{path\.(module|root)}/""")
    private val escapedCharacter = Regex("""\\(["\\nt])""")

    fun read(
        blockContent: String,
        key: String,
        baseDir: File,
    ): String? =
        readHeredoc(blockContent, key)
            ?: readFile(blockContent, key, baseDir)
            ?: readQuoted(blockContent, key)

    private fun readHeredoc(
        blockContent: String,
        key: String,
    ): String? {
        val match = Regex("""^\s*${Regex.escape(key)}\s*=\s*<<(-?)(\w+)\s*$""", RegexOption.MULTILINE).find(blockContent) ?: return null
        val (indented, marker) = match.destructured
        val lines = blockContent.substring(match.range.last + 1).lines().drop(1)
        val body = lines.takeWhile { it.trim() != marker }
        if (body.size == lines.size) return null
        return (if (indented.isEmpty()) body.joinToString("\n") else body.joinToString("\n").trimIndent()).trim()
    }

    private fun readFile(
        blockContent: String,
        key: String,
        baseDir: File,
    ): String? {
        val path =
            Regex("""^\s*${Regex.escape(key)}\s*=\s*file\(\s*"([^"]+)"\s*\)""", RegexOption.MULTILINE)
                .find(blockContent)?.groupValues?.get(1) ?: return null
        val file = File(baseDir, path.replace(modulePathPrefix, ""))
        return file.takeIf { it.isFile }?.readText()?.trim()
    }

    private fun readQuoted(
        blockContent: String,
        key: String,
    ): String? {
        val raw =
            Regex("""^\s*${Regex.escape(key)}\s*=\s*"((?:[^"\\]|\\.)*)"""", RegexOption.MULTILINE)
                .find(blockContent)?.groupValues?.get(1) ?: return null
        return raw.replace(escapedCharacter) { escape ->
            when (val character = escape.groupValues[1]) {
                "n" -> "\n"
                "t" -> "\t"
                else -> character
            }
        }
    }
}
