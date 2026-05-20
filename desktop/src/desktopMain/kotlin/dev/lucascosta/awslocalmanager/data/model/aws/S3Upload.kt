package dev.lucascosta.awslocalmanager.data.model.aws

import dev.lucascosta.awslocalmanager.constants.AppConstants.APPLICATION_OCTET_STREAM

data class S3Upload(
    val bucketName: String,
    val key: String,
    val bytes: ByteArray,
    val contentType: String = APPLICATION_OCTET_STREAM,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is S3Upload) return false
        return bucketName == other.bucketName && key == other.key && bytes.contentEquals(other.bytes) && contentType == other.contentType
    }

    override fun hashCode(): Int {
        var result = bucketName.hashCode()
        result = 31 * result + key.hashCode()
        result = 31 * result + bytes.contentHashCode()
        result = 31 * result + contentType.hashCode()
        return result
    }
}
