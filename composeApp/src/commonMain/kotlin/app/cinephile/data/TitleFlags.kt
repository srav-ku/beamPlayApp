package app.cinephile.data

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

expect fun readTitleFlags(): String

expect fun writeTitleFlags(json: String)

@kotlinx.serialization.Serializable
private data class FlagState(val watched: List<Long> = emptyList(), val liked: List<Long> = emptyList())

/**
 * Watched and liked, remembered per title.
 *
 * These used to live only in the detail screen's memory, so the state vanished on
 * reopen while Watch Later survived. Same durability as the lists now, and the
 * two default lists are kept in step so the state is visible in one place.
 */
object TitleFlags {
    private val json = Json { ignoreUnknownKeys = true }
    private var state = FlagState()

    fun ensureLoaded() {
        if (state.watched.isNotEmpty() || state.liked.isNotEmpty()) return
        state = runCatching { json.decodeFromString(FlagState.serializer(), readTitleFlags()) }.getOrDefault(FlagState())
    }

    fun isWatched(id: Long): Boolean = id != 0L && state.watched.contains(id)

    fun isLiked(id: Long): Boolean = id != 0L && state.liked.contains(id)

    fun setWatched(id: Long, watched: Boolean) {
        if (id == 0L) return
        state = if (watched) state.copy(watched = (state.watched + id).distinct()) else state.copy(watched = state.watched - id)
        persist()
        runCatching { CollectionsRepo.setWatched(id, watched) }
    }

    fun setLiked(id: Long, liked: Boolean) {
        if (id == 0L) return
        state = if (liked) state.copy(liked = (state.liked + id).distinct()) else state.copy(liked = state.liked - id)
        persist()
        // Mirror into the Favorites list so "liked" reads the same as "saved".
        runCatching {
            val favorites = CollectionsRepo.byId(CollectionsRepo.FAVORITES) ?: return@runCatching
            val already = favorites.items.any { it.tmdbId == id }
            if (liked && !already) {
                CollectionsRepo.addItems(
                    listOf(CollectionsRepo.FAVORITES),
                    listOf(CollItem(tmdbId = id)),
                )
            } else if (!liked && already) {
                CollectionsRepo.removeItem(CollectionsRepo.FAVORITES, id)
            }
        }
    }

    private fun persist() {
        runCatching { writeTitleFlags(json.encodeToString(FlagState.serializer(), state)) }
    }
}