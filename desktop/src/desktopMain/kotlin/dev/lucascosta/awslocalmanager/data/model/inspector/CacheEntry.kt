package dev.lucascosta.awslocalmanager.data.model.inspector

data class CacheEntry(
    val key: String,
    val value: String?,
    val ttl: Long?,
)
