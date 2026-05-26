package dev.lucascosta.awslocalmanager.data.model.resources

enum class ElastiCacheEngine(val cliValue: String, val defaultPort: Int) {
    REDIS("redis", 6379),
    MEMCACHED("memcached", 11211),
}
