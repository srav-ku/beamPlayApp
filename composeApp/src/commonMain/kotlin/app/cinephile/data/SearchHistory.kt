package app.cinephile.data

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer

/** Platform storage for recent searches. */
expect fun readSearchHistory(): String

expect fun writeSearchHistory(json: String)

expect fun clearSearchHistoryStore()

/**
 * Recent searches, newest first, capped so the list stays a shortcut rather than
 * an archive. Local, so it works signed out.
 */
object SearchHistory {
    private const val LIMIT = 8
    private val serializer = ListSerializer(String.serializer())

    var entries: List<String> = emptyList()
        private set

    fun ensureLoaded() {
        if (entries.isNotEmpty()) return
        entries = runCatching { SearchJson.decodeFromString(serializer, readSearchHistory()) }.getOrDefault(emptyList())
    }

    fun remember(query: String) {
        val q = query.trim()
        if (q.length < 2) return
        entries = (listOf(q) + entries.filterNot { it.equals(q, ignoreCase = true) }).take(LIMIT)
        persist()
    }

    fun clear() {
        entries = emptyList()
        runCatching { clearSearchHistoryStore() }
    }

    private fun persist() {
        runCatching { writeSearchHistory(SearchJson.encodeToString(serializer, entries)) }
    }
}