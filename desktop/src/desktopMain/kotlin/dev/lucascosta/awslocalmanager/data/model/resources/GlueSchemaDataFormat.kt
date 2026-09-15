package dev.lucascosta.awslocalmanager.data.model.resources

enum class GlueSchemaDataFormat {
    AVRO,
    JSON,
    PROTOBUF,
    ;

    companion object {
        fun fromCliValue(value: String): GlueSchemaDataFormat? = entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
    }
}
