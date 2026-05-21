package dev.lucascosta.awslocalmanager.data.model.aws

object ResourceRegistry {
    private val definitions = mutableListOf<AwsResourceDefinition>()

    fun register(vararg resources: AwsResourceDefinition) {
        definitions.addAll(resources)
    }

    fun all(): List<AwsResourceDefinition> = definitions.toList()

    fun fromTerraformPrefix(prefix: String): AwsResourceDefinition? = definitions.find { it.terraformPrefix == prefix }

    fun fromHealthKey(key: String): AwsResourceDefinition? = definitions.find { it.healthKey == key.lowercase() }
}
