package dev.lucascosta.awslocalmanager.data.model.health

enum class ServiceStatus(val aliases: List<String> = emptyList()) {
    RUNNING,
    AVAILABLE,
    STOPPED(listOf("stopped", "disabled")),
    ERROR,
    UNKNOWN,
    ;

    companion object {
        fun from(value: String): ServiceStatus {
            val search = value.lowercase()
            return entries.find {
                it.name.lowercase() == search || it.aliases.contains(search)
            } ?: UNKNOWN
        }
    }
}
