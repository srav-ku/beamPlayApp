package app.cinephile.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** One TMDB multi-search hit: a movie, a series or a person. */
@Serializable
data class TmdbSearchItem(
    val id: Long = 0,
    val media_type: String? = null,
    val title: String? = null,
    val name: String? = null,
    val poster_path: String? = null,
    val profile_path: String? = null,
    val release_date: String? = null,
    val first_air_date: String? = null,
    val vote_average: Double? = null,
    val genre_ids: List<Long> = emptyList(),
    val original_language: String? = null,
    val known_for_department: String? = null,
    val popularity: Double? = null,
) {
    val isPerson: Boolean get() = media_type == "person"
    val displayTitle: String get() = title ?: name ?: ""
    val year: Int? get() = (release_date ?: first_air_date)?.take(4)?.toIntOrNull()
    val isTv: Boolean get() = media_type == "tv"

    /** 16 is Animation - the practical test for "anime". */
    val isAnimation: Boolean get() = genre_ids.contains(16L)

    /** Languages that make a title read as "Asian" in this catalogue's sense. */
    val isAsian: Boolean
        get() = original_language in setOf("ja", "ko", "zh", "cn", "hi", "ta", "te", "ml", "kn", "bn", "th", "id", "vi", "tl")

    fun toMediaItem(): MediaItem = MediaItem(
        id = 0,
        tmdb_id = id,
        title = displayTitle,
        poster_path = poster_path,
        backdrop_path = poster_path,
        release_year = year,
        first_release_year = year,
        tmdb_rating = vote_average,
        type = if (isTv) "series" else "movie",
    )
}

@Serializable
data class TmdbSearchPage(
    val results: List<TmdbSearchItem> = emptyList(),
    val total_results: Int = 0,
)

/** Lenient decoder: search payloads carry fields this app does not model. */
val SearchJson: Json = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
}
