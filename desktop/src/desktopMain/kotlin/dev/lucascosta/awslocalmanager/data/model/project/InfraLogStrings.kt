package dev.lucascosta.awslocalmanager.data.model.project

import dev.lucascosta.awslocalmanager.i18n.Strings

data class InfraLogStrings(
    val creatingFmt: String,
    val unsupportedFmt: String,
    val createdFmt: String,
    val createErrorFmt: String,
    val subscribingFmt: String,
    val subscribedFmt: String,
    val subscribeErrorFmt: String,
    val filterWarningFmt: String,
    val filterScopeWarningFmt: String,
) {
    companion object {
        fun from(strings: Strings) =
            InfraLogStrings(
                creatingFmt = strings.infraLogCreatingFmt,
                unsupportedFmt = strings.infraLogUnsupportedFmt,
                createdFmt = strings.infraLogCreatedFmt,
                createErrorFmt = strings.infraLogCreateErrorFmt,
                subscribingFmt = strings.infraLogSubscribingFmt,
                subscribedFmt = strings.infraLogSubscribedFmt,
                subscribeErrorFmt = strings.infraLogSubscribeErrorFmt,
                filterWarningFmt = strings.infraLogFilterWarningFmt,
                filterScopeWarningFmt = strings.infraLogFilterScopeWarningFmt,
            )
    }
}
