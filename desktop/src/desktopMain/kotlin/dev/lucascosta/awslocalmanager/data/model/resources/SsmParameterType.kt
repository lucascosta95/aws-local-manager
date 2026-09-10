package dev.lucascosta.awslocalmanager.data.model.resources

enum class SsmParameterType(val cliValue: String) {
    STRING("String"),
    STRING_LIST("StringList"),
    SECURE_STRING("SecureString"),
    ;

    companion object {
        fun fromCliValue(value: String): SsmParameterType = entries.find { it.cliValue.equals(value, ignoreCase = true) } ?: STRING
    }
}
