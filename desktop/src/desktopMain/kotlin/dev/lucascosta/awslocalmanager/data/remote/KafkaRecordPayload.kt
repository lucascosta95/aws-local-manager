package dev.lucascosta.awslocalmanager.data.remote

import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.util.Base64
import java.util.UUID

sealed interface KafkaRecordPayload {
    data class Text(val text: String) : KafkaRecordPayload

    // Glue SerDe framing: header version 3, a compression byte, then the 16-byte schema version id.
    data class GlueEncoded(val schemaVersionId: String, val sizeBytes: Int) : KafkaRecordPayload

    // Confluent framing that the broker could not decode, e.g. because the schema is missing from its registry.
    data class ConfluentEncoded(val schemaId: Int, val sizeBytes: Int) : KafkaRecordPayload

    data class Binary(val base64: String, val sizeBytes: Int) : KafkaRecordPayload

    companion object {
        private const val GLUE_HEADER_VERSION: Byte = 3
        private val GLUE_COMPRESSION_BYTES = setOf<Byte>(0, 5)
        private const val GLUE_HEADER_SIZE = 18
        private const val CONFLUENT_MAGIC_BYTE: Byte = 0
        private const val CONFLUENT_HEADER_SIZE = 5

        fun fromBase64(encoded: String): KafkaRecordPayload = from(Base64.getDecoder().decode(encoded))

        fun from(bytes: ByteArray): KafkaRecordPayload {
            if (isGlueFramed(bytes)) {
                val buffer = ByteBuffer.wrap(bytes, 2, 16)
                return GlueEncoded(UUID(buffer.long, buffer.long).toString(), bytes.size)
            }
            decodeUtf8(bytes)?.let { return Text(it) }
            if (bytes.size > CONFLUENT_HEADER_SIZE && bytes[0] == CONFLUENT_MAGIC_BYTE) {
                return ConfluentEncoded(ByteBuffer.wrap(bytes, 1, 4).int, bytes.size)
            }
            return Binary(Base64.getEncoder().encodeToString(bytes), bytes.size)
        }

        private fun isGlueFramed(bytes: ByteArray) =
            bytes.size > GLUE_HEADER_SIZE && bytes[0] == GLUE_HEADER_VERSION && bytes[1] in GLUE_COMPRESSION_BYTES

        private fun decodeUtf8(bytes: ByteArray): String? =
            try {
                Charsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString()
                    .takeIf { text -> text.none { it.isISOControl() && it !in "\n\r\t" } }
            } catch (_: CharacterCodingException) {
                null
            }
    }
}
