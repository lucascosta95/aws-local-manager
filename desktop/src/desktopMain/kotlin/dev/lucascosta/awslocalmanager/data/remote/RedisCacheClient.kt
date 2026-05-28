package dev.lucascosta.awslocalmanager.data.remote

import dev.lucascosta.awslocalmanager.data.model.inspector.CacheEntry
import io.lettuce.core.RedisClient
import io.lettuce.core.ScanArgs
import io.lettuce.core.ScanCursor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.util.concurrent.TimeUnit

data class CacheScanPage(
    val entries: List<CacheEntry>,
    val nextCursor: String,
    val hasMore: Boolean,
)

class RedisCacheClient(host: String, port: Int) : Closeable {
    private val client: RedisClient = RedisClient.create("redis://$host:$port")

    suspend fun scanPage(
        cursor: String,
        pattern: String,
        pageSize: Long,
    ): Result<CacheScanPage> =
        withContext(Dispatchers.IO) {
            runCatching {
                val connection = client.connect()
                try {
                    val commands = connection.sync()
                    val scanCursor = ScanCursor.of(cursor)
                    val scanArgs =
                        if (pattern.isBlank() || pattern == "*") {
                            ScanArgs.Builder.limit(pageSize)
                        } else {
                            ScanArgs.Builder.matches("$pattern*").limit(pageSize)
                        }
                    val result = commands.scan(scanCursor, scanArgs)
                    val entries =
                        result.keys.mapNotNull { key ->
                            val value = runCatching { commands.get(key) }.getOrNull()
                            val rawTtl = runCatching { commands.ttl(key) }.getOrNull() ?: -2L
                            val ttl =
                                when {
                                    rawTtl == -1L -> null
                                    rawTtl < 0L -> -2L
                                    else -> rawTtl
                                }
                            CacheEntry(key = key, value = value, ttl = ttl)
                        }

                    CacheScanPage(
                        entries = entries,
                        nextCursor = result.cursor,
                        hasMore = !result.isFinished,
                    )
                } finally {
                    connection.close()
                }
            }
        }

    override fun close() {
        client.shutdown(0, 0, TimeUnit.SECONDS)
    }
}
