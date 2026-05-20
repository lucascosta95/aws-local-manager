package dev.lucascosta.awslocalmanager.features.quick

enum class DynamoKeyType(val awsValue: String) {
    STRING("S"),
    NUMBER("N"),
    BINARY("B"),
}
