package app.cinephile.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * A movie or series saved inside a collection.
 *
 * [tmdbId] is the identity: the catalogue row may not exist yet (a TMDB title
 * opened from a carousel), and when it does the id still matches, so one card
 * never ends up twice.
 */
@Serializable
data class CollItem(
    val tmdbId: Long = 0L,
    val mediaId: Long = 0L,
    val title: String = "",
    val posterPath: String? = null,
    val year: Int? = null,
    val type: String? = null,
    val addedAt: Long = 0L,
    val watched: Boolean = false,
)

/** A user's list. The two defaults - Watch Later and Favorites - cannot be deleted. */
@Serializable
data class CineCollection(
    val id: String = "",
    val name: String = "",
    val isDefault: Boolean = false,
    val pinned: Boolean = false,
    val createdAt: Long = 0L,
    val items: List<CollItem> = emptyList(),
)

/** Platform storage for the serialised collections. */
expect fun loadCollectionsJson(): String

expect fun saveCollectionsJson(json: String)

expect fun nowMillis(): Long

/**
 * Collections, held in memory as observable state and mirrored to local storage.
 *
 * Deliberately local-first: a guest has no account to attach lists to, and the
 * whole feature can be exercised with no server round trip. Moving this to the
 * worker later means replacing the platform functions and the persist() call -
 * the UI only ever talks to this object.
 */
object CollectionsRepo {
    const val WATCH_LATER = "watch-later"
    const val FAVORITES = "favorites"

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    var collections by mutableStateOf<List<CineCollection>>(emptyList())
        private set

    private var loaded = false

    fun ensureLoaded() {
        if (loaded) return
        loaded = true
        val decoded = runCatching {
            json.decodeFromString<List<CineCollection>>(loadCollectionsJson())
        }.getOrDefault(emptyList())
        collections = if (decoded.any { it.isDefault }) decoded else defaults() + decoded
        persist()
    }

    private fun defaults(): List<CineCollection> = listOf(
        CineCollection(id = WATCH_LATER, name = "Watch Later", isDefault = true, createdAt = nowMillis()),
        CineCollection(id = FAVORITES, name = "Favorites", isDefault = true, createdAt = nowMillis()),
    )

    private fun persist() {
        runCatching { saveCollectionsJson(json.encodeToString(collections)) }
    }

    private fun update(next: List<CineCollection>) {
        collections = next
        persist()
    }

    fun byId(id: String): CineCollection? = collections.firstOrNull { it.id == id }

    fun create(name: String): CineCollection {
        val created = CineCollection(
            id = "c" + nowMillis().toString(),
            name = name.trim().ifBlank { "New List" },
            createdAt = nowMillis(),
        )
        update(collections + created)
        return created
    }

    fun rename(id: String, name: String) {
        update(collections.map { if (it.id == id) it.copy(name = name.trim().ifBlank { it.name }) else it })
    }

    fun delete(id: String) {
        if (byId(id)?.isDefault == true) return
        update(collections.filterNot { it.id == id })
    }

    fun setPinned(id: String, pinned: Boolean) {
        // At most three pinned lists, matching the reference behaviour.
        if (pinned && collections.count { it.pinned } >= 3 && byId(id)?.pinned != true) return
        update(collections.map { if (it.id == id) it.copy(pinned = pinned) else it })
    }

    /** Adds the same items to several collections at once. */
    fun addItems(ids: List<String>, items: List<CollItem>) {
        update(
            collections.map { collection ->
                if (collection.id !in ids) return@map collection
                val existing = collection.items.map { it.tmdbId }.toSet()
                val fresh = items.filterNot { it.tmdbId in existing }
                collection.copy(items = collection.items + fresh.map { it.copy(addedAt = nowMillis()) })
            },
        )
    }

    /** Reorders inside one list - drives the Custom order view arrows. */
    fun moveItem(collectionId: String, tmdbId: Long, delta: Int) {
        update(
            collections.map { collection ->
                if (collection.id != collectionId) collection
                else {
                    val list = collection.items.toMutableList()
                    val from = list.indexOfFirst { it.tmdbId == tmdbId }
                    if (from >= 0) {
                        val to = (from + delta).coerceIn(0, list.size - 1)
                        if (to != from) {
                            val moved = list.removeAt(from)
                            list.add(to, moved)
                        }
                    }
                    collection.copy(items = list)
                }
            },
        )
    }

    fun removeItem(collectionId: String, tmdbId: Long) {
        update(
            collections.map {
                if (it.id == collectionId) it.copy(items = it.items.filterNot { item -> item.tmdbId == tmdbId })
                else it
            },
        )
    }

    /** The bookmark chip everywhere in the app toggles Watch Later. */
    fun toggleWatchLater(item: CollItem) {
        val current = byId(WATCH_LATER) ?: return
        val alreadySaved = current.items.any { it.tmdbId == item.tmdbId }
        update(
            collections.map {
                if (it.id != WATCH_LATER) it
                else if (alreadySaved) it.copy(items = it.items.filterNot { saved -> saved.tmdbId == item.tmdbId })
                else it.copy(items = it.items + item.copy(addedAt = nowMillis()))
            },
        )
    }

    fun inWatchLater(tmdbId: Long): Boolean =
        byId(WATCH_LATER)?.items?.any { it.tmdbId == tmdbId } == true

    /** Marks every copy of a title watched, so progress bars stay honest. */
    fun setWatched(tmdbId: Long, watched: Boolean) {
        update(
            collections.map { collection ->
                collection.copy(items = collection.items.map { if (it.tmdbId == tmdbId) it.copy(watched = watched) else it })
            },
        )
    }

    fun collectionsWith(tmdbId: Long): List<CineCollection> =
        collections.filter { collection -> collection.items.any { it.tmdbId == tmdbId } }

    fun watchedCount(collection: CineCollection): Int = collection.items.count { it.watched }

    /** Pinned first, then the defaults, then the rest - newest list at the top. */
    fun ordered(): List<CineCollection> = collections.sortedWith(
        compareByDescending<CineCollection> { it.pinned }
            .thenByDescending { it.isDefault }
            .thenByDescending { it.createdAt },
    )

    /** Turns any catalogue item into a saveable entry. */
    fun entryFrom(
        tmdbId: Long?,
        mediaId: Long,
        title: String,
        posterPath: String?,
        year: Int?,
        type: String?,
    ): CollItem = CollItem(
        tmdbId = tmdbId ?: mediaId,
        mediaId = mediaId,
        title = title,
        posterPath = posterPath,
        year = year,
        type = type,
    )
}
