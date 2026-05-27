package dev.lucascosta.awslocalmanager.data.remote

import dev.lucascosta.awslocalmanager.data.model.inspector.CacheEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket

private const val MEMCACHED_TIMEOUT_MS = 5_000
private const val DEFAULT_KEY_LIMIT = 100

class MemcachedCacheClient(private val host: String, private val port: Int) {
    suspend fun listKeys(limit: Int = DEFAULT_KEY_LIMIT): Result<List<CacheEntry>> =
        withContext(Dispatchers.IO) {
            runCatching {
                Socket(host, port).use { socket ->
                    socket.soTimeout = MEMCACHED_TIMEOUT_MS
                    val writer = PrintWriter(socket.getOutputStream(), true)
                    val reader = BufferedReader(InputStreamReader(socket.getInputStream()))

                    val slabIds = fetchSlabIds(writer, reader)
                    val keyInfos = fetchKeysFromSlabs(writer, reader, slabIds, limit)
                    val keys = keyInfos.map { it.first }
                    if (keys.isEmpty()) {
                        return@runCatching emptyList()
                    }

                    val values = fetchValues(writer, reader, keys)
                    keyInfos.map { (key, ttl) ->
                        CacheEntry(key = key, value = values[key], ttl = ttl)
                    }
                }
            }
        }

    private fun fetchSlabIds(
        writer: PrintWriter,
        reader: BufferedReader,
    ): List<Int> {
        writer.print("stats items\r\n")
        writer.flush()
        val slabIds = mutableSetOf<Int>()
        while (true) {
            val line = reader.readLine() ?: break
            if (line == "END") break
            val match = Regex("STAT items:(\\d+):number").find(line)
            if (match != null) {
                slabIds.add(match.groupValues[1].toInt())
            }
        }
        return slabIds.toList()
    }

    private fun fetchKeysFromSlabs(
        writer: PrintWriter,
        reader: BufferedReader,
        slabIds: List<Int>,
        limit: Int,
    ): List<Pair<String, Long?>> {
        val now = System.currentTimeMillis() / 1000L
        val result = mutableListOf<Pair<String, Long?>>()
        for (slab in slabIds) {
            if (result.size >= limit) {
                break
            }

            writer.print("stats cachedump $slab $limit\r\n")
            writer.flush()

            while (true) {
                val line = reader.readLine() ?: break
                if (line == "END") break
                val match = Regex("ITEM (\\S+) \\[\\d+ b; (\\d+) s\\]").find(line)
                if (match != null) {
                    val key = match.groupValues[1]
                    val expiry = match.groupValues[2].toLongOrNull() ?: 0L
                    val ttl =
                        when {
                            expiry == 0L -> null
                            expiry > now -> expiry - now
                            else -> 0L
                        }

                    result.add(key to ttl)

                    if (result.size >= limit) {
                        break
                    }
                }
            }
        }
        return result
    }

    private fun fetchValues(
        writer: PrintWriter,
        reader: BufferedReader,
        keys: List<String>,
    ): Map<String, String> {
        val command = "get ${keys.joinToString(" ")}\r\n"
        writer.print(command)
        writer.flush()

        val values = mutableMapOf<String, String>()
        var currentKey: String? = null
        val valueBuilder = StringBuilder()

        while (true) {
            val line = reader.readLine() ?: break
            when {
                line == "END" -> break
                line.startsWith("VALUE ") -> {
                    if (currentKey != null) {
                        values[currentKey] = valueBuilder.toString().trimEnd()
                        valueBuilder.clear()
                    }
                    currentKey = line.split(" ").getOrNull(1)
                }

                currentKey != null -> {
                    if (valueBuilder.isNotEmpty()){
                        valueBuilder.append('\n')
                    }
                    valueBuilder.append(line)
                }
            }
        }

        if (currentKey != null) {
            values[currentKey] = valueBuilder.toString().trimEnd()
        }

        return values
    }
}
