package app.cinephile.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Platform storage for the cached payloads, so a cold start is not a cold fetch. */
expect fun readDiskCache(): String

expect fun writeDiskCache(json: String)

@Serializable
private data class DiskEntry(val time: Long, val data: String)

/**
 * TTL cache with optional persistence.
 *
 * Two jobs:
 *  1. Keep tab switching instant. Home fires ten TMDB calls, Browse queries the
 *     catalogue; re-entering either inside the window returns the previous answer
 *     with no request at all.
 *  2. Survive a restart. Catalogue buffers and rail payloads are written to disk,
 *     so opening the app again paints immediately instead of waiting on the
 *     network - and if the stored copy is older than its TTL it is simply ignored.
 *
 * Only list payloads (MediaItem) are persisted; they are the expensive ones.
 */
object TtlCache {
    /**
     * How long each kind of data stays good. Chosen against how often the source
     * actually changes, not for maximum staleness savings:
     *
     *  - TMDB rails: refreshed upstream about daily, so six hours is fresh enough
     *    to be useful and old enough to avoid refetching on every visit.
     *  - Catalogue pages: the catalogue changes only when an admin imports, so
     *    half an hour keeps things current while making browsing feel local.
     *  - Filters: genres/years/languages change only with the catalogue itself.
     */
    const val TMDB_TTL = 6 * 60 * 60 * 1000L
    const val PAGE_TTL = 30 * 60 * 1000L
    const val FILTERS_TTL = 12 * 60 * 60 * 1000L

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val entries = mutableMapOf<String, Pair<Long, Any>>()
    private var lastSave = 0L

    private fun persistable(key: String) = key.startsWith("browse.") || key.startsWith("tmdb.")

    /** Loads whatever is still within its TTL. Call once at start-up. */
    fun hydrate() {
        val raw = runCatching { readDiskCache() }.getOrDefault("")
        if (raw.isBlank()) return
        val stored = runCatching { json.decodeFromString<Map<String, DiskEntry>>(raw) }
            .getOrDefault(emptyMap())
        stored.forEach { (key, entry) ->
            val list = runCatching { json.decodeFromString<List<MediaItem>>(entry.data) }.getOrNull()
            if (list != null) entries[key] = entry.time to list
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> get(key: String, ttlMs: Long): T? {
        val entry = entries[key] ?: return null
        if (nowMillis() - entry.first > ttlMs) {
            entries.remove(key)
            return null
        }
        return entry.second as? T
    }

    fun put(key: String, value: Any) {
        entries[key] = nowMillis() to value
        if (persistable(key)) saveSoon()
    }

    /** Writes at most once every few seconds - several rails land in one burst. */
    private fun saveSoon() {
        val now = nowMillis()
        if (now - lastSave < 3_000L) return
        lastSave = now
        val payload = entries
            .filterKeys { persistable(it) }
            .mapNotNull { (key, entry) ->
                val list = entry.second as? List<MediaItem> ?: return@mapNotNull null
                key to DiskEntry(entry.first, json.encodeToString<List<MediaItem>>(list))
            }
            .toMap()
        runCatching { writeDiskCache(json.encodeToString(payload)) }
    }

    fun clear() {
        entries.clear()
        runCatching { writeDiskCache("") }
    }
}

/** Returns the cached value when it is still fresh, otherwise loads and stores it. */
suspend fun <T : Any> cached(key: String, ttlMs: Long, load: suspend () -> T): T {
    TtlCache.get<T>(key, ttlMs)?.let { return it }
    val fresh = load()
    TtlCache.put(key, fresh)
    return fresh
}
