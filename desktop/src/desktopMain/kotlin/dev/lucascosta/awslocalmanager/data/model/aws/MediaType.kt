package dev.lucascosta.awslocalmanager.data.model.aws

import dev.lucascosta.awslocalmanager.constants.AppConstants.APPLICATION_OCTET_STREAM

enum class MediaType(val mimeType: String, vararg val extensions: String) {
    JSON("application/json", "json"),
    TEXT("text/plain", "txt"),
    HTML("text/html", "html", "htm"),
    XML("application/xml", "xml"),
    CSV("text/csv", "csv"),
    PNG("image/png", "png"),
    JPEG("image/jpeg", "jpg", "jpeg"),
    GIF("image/gif", "gif"),
    PDF("application/pdf", "pdf"),
    ZIP("application/zip", "zip"),
    ;

    companion object {
        fun from(filename: String): String {
            val ext = filename.substringAfterLast('.').lowercase()
            return entries.find { ext in it.extensions }?.mimeType ?: APPLICATION_OCTET_STREAM
        }
    }
}
