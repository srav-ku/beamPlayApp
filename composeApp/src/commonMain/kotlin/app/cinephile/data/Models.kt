package app.cinephile.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class MediaItem(
    val id: Long,
    val tmdb_id: Long? = null,
    val imdb_id: String? = null,
    val title: String,
    val overview: String? = null,
    val poster_path: String? = null,
    val backdrop_path: String? = null,
    val release_year: Int? = null,
    val first_release_year: Int? = null,
    val runtime: Int? = null,
    val total_seasons: Int? = null,
    val total_episodes: Int? = null,
    val tmdb_rating: Double? = null,
    val imdb_rating: Double? = null,
    // Enrichment fields - null until the enrichment job fills them.
    val rt_rating: String? = null,
    val trailer_key: String? = null,
    val metacritic: Int? = null,
    val budget: Long? = null,
    val revenue: Long? = null,
    val genres: String? = null,
    val type: String? = null,
) {
    val year: Int? get() = release_year ?: first_release_year

    fun genreList(): List<String> = try {
        genres?.let { Json.decodeFromString<List<String>>(it) } ?: emptyList()
    } catch (_: Exception) {
        emptyList()
    }
}

@Serializable
data class MediaPage(val page: Int = 1, val limit: Int = 20, val items: List<MediaItem> = emptyList())

@Serializable
data class MediaList(val items: List<MediaItem> = emptyList())

@Serializable
data class AuthUser(val id: Long, val email: String, val role: String, val created_at: Long? = null)

@Serializable
data class AuthResponse(val token: String, val user: AuthUser)
