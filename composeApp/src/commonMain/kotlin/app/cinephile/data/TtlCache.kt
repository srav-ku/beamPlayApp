package app.cinephile.data

/**
 * Tiny in-memory TTL cache.
 *
 * Home fires ten TMDB calls and Browse re-queries the catalogue every time a tab
 * is opened. Re-entering a tab within the TTL now returns the previous answer
 * instead of hitting the network again - which makes tab switching instant and
 * keeps us far away from any rate limit.
 *
 * Deliberately process-local: it is a speed layer, not storage. Anything that
 * must survive a restart belongs in a real store.
 */
object TtlCache {
    private val entries = mutableMapOf<String, Pair<Long, Any>>()

    /** TMDB refreshes these rails about once a day, so six hours is plenty. */
    const val TMDB_TTL = 6 * 60 * 60 * 1000L
    /** Genres/years/languages only change when the catalogue does. */
    const val FILTERS_TTL = 12 * 60 * 60 * 1000L
    /** Catalogue pages: long enough to feel instant, short enough to pick up imports. */
    const val PAGE_TTL = 30 * 60 * 1000L

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
    }

    fun clear() {
        entries.clear()
    }
}

/** Returns the cached value when it is still fresh, otherwise loads and stores it. */
suspend fun <T : Any> cached(key: String, ttlMs: Long, load: suspend () -> T): T {
    TtlCache.get<T>(key, ttlMs)?.let { return it }
    val fresh = load()
    TtlCache.put(key, fresh)
    return fresh
}
