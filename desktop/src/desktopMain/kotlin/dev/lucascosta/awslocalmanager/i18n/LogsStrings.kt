package dev.lucascosta.awslocalmanager.i18n

import androidx.compose.runtime.compositionLocalOf
import dev.lucascosta.awslocalmanager.constants.AppConstants.ENGLISH

val LocalLogsStrings = compositionLocalOf<LogsStrings> { LogsStringsPtBr }

fun logsStringsForLanguage(tag: String): LogsStrings =
    when (tag) {
        ENGLISH -> LogsStringsEnUs
        else -> LogsStringsPtBr
    }

data class LogsStrings(
    val logsTitle: String,
    val logsSubtitle: String,
    val logsEmpty: String,
    val logsNoMatches: String,
    val logsSearchPlaceholder: String,
    val logsLevel: String,
    val logsSource: String,
    val logsClearFilters: String,
    val logsAutoScroll: String,
    val logsCopyAll: String,
    val logsClear: String,
    val logsCountFmt: String,
    val logsStackTraceHint: String,
    val logsCopyEntry: String,
    val logsRepeatFmt: String,
)

val LogsStringsEnUs =
    LogsStrings(
        logsTitle = "Logs",
        logsSubtitle =
            "Everything the app did in this session, exceptions included. Nothing is written to disk, " +
                "so closing the app discards it.",
        logsEmpty = "Nothing logged yet in this session",
        logsNoMatches = "No entry matches the current filters",
        logsSearchPlaceholder = "Search message, source or stack trace",
        logsLevel = "Level",
        logsSource = "Source",
        logsClearFilters = "Clear filters",
        logsAutoScroll = "Tail",
        logsCopyAll = "Copy visible",
        logsClear = "Clear",
        logsCountFmt = "{visible} of {total}",
        logsStackTraceHint = "click the line to see the stack trace",
        logsCopyEntry = "Copy entry",
        logsRepeatFmt = "repeated {count}x",
    )

val LogsStringsPtBr =
    LogsStrings(
        logsTitle = "Logs",
        logsSubtitle =
            "Tudo que o app fez nesta sessão, exceções incluídas. Nada é gravado em disco, " +
                "então fechar o app descarta.",
        logsEmpty = "Nada registrado ainda nesta sessão",
        logsNoMatches = "Nenhuma entrada corresponde aos filtros atuais",
        logsSearchPlaceholder = "Buscar mensagem, origem ou stack trace",
        logsLevel = "Nível",
        logsSource = "Origem",
        logsClearFilters = "Limpar filtros",
        logsAutoScroll = "Acompanhar",
        logsCopyAll = "Copiar visíveis",
        logsClear = "Limpar",
        logsCountFmt = "{visible} de {total}",
        logsStackTraceHint = "clique na linha para ver o stack trace",
        logsCopyEntry = "Copiar entrada",
        logsRepeatFmt = "repetiu {count}x",
    )
