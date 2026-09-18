package app.cinephile.core.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/* ------------------------------------------------------------------ */
/*  BeamPlay worker models — mirrors the website's src/lib/types.ts    */
/*  Field names stay snake_case so they serialise 1:1 with the API.    */
/* ------------------------------------------------------------------ */

@Serializable
data class Movie(
    val id: Long = 0,
    val tmdb_id: Long? = null,
    val imdb_id: String? = null,
    val title: String = "",
    val overview: String? = null,
    val poster_path: String? = null,
    val backdrop_path: String? = null,
    val release_year: Int? = null,
    val runtime: Int? = null,
    val tmdb_rating: Double? = null,
    val imdb_rating: Double? = null,
    val cast: String? = null,
    val genres: String? = null,
    val created_at: Long? = null,
) {
    fun genreList(): List<String> = parseStringList(genres)
    fun castList(): List<CastMember> = parseCast(cast)
    val isMovie: Boolean get() = true
}

@Serializable
data class Series(
    val id: Long = 0,
    val tmdb_id: Long? = null,
    val imdb_id: String? = null,
    val title: String = "",
    val overview: String? = null,
    val poster_path: String? = null,
    val backdrop_path: String? = null,
    val first_release_year: Int? = null,
    val total_seasons: Int? = null,
    val total_episodes: Int? = null,
    val tmdb_rating: Double? = null,
    val imdb_rating: Double? = null,
    val cast: String? = null,
    val genres: String? = null,
    val created_at: Long? = null,
) {
    fun genreList(): List<String> = parseStringList(genres)
    fun castList(): List<CastMember> = parseCast(cast)
    val isMovie: Boolean get() = false
}

/** A single streaming link row. `url` points at a Vidara embed page. */
@Serializable
data class Link(
    val id: Long = 0,
    val movie_id: Long? = null,
    val episode_id: Long? = null,
    val url: String = "",
    val quality: String = "",
    val audio_languages: String? = null,
    val created_at: Long? = null,
) {
    fun audioLanguageList(): List<String> = parseStringList(audio_languages)
}

@Serializable
data class Season(
    val id: Long = 0,
    val series_id: Long = 0,
    val tmdb_season_id: Long? = null,
    val season_number: Int = 0,
    val name: String = "",
    val created_at: Long? = null,
)

@Serializable
data class Episode(
    val id: Long = 0,
    val series_id: Long = 0,
    val season_id: Long = 0,
    val episode_number: Int = 0,
    val tmdb_episode_number: Long? = null,
    val name: String = "",
    val overview: String? = null,
    val thumbnail_path: String? = null,
    val runtime: Int? = null,
    val created_at: Long? = null,
)

/** Telegram-backed download entry. The worker only returns a subset of these. */
@Serializable
data class DownloadFile(
    val id: Long = 0,
    val content_type: String? = null,
    val content_id: Long? = null,
    val tmdb_id: Long? = null,
    val channel_msg_id: Long? = null,
    val telegram_file_id: String? = null,
    val file_unique_id: String? = null,
    val file_name: String = "",
    val original_name: String? = null,
    val quality: String = "",
    val audio_languages: String = "[]",
    val has_subtitles: Int = 0,
    val file_size: Long? = null,
    val created_at: Long? = null,
    val episode_number: Int? = null,
) {
    fun audioLanguageList(): List<String> = parseStringList(audio_languages)
    val hasSubtitles: Boolean get() = has_subtitles != 0
}

/**
 * One row of `GET /user/playback`.
 *
 * NOTE: the worker stores `is_finished` as a SQLite integer (0/1) and selects
 * it verbatim, so this MUST be decoded as [Int]. Declaring it `Boolean` makes
 * every decode throw (`Expected valid boolean literal prefix, but had '0'`),
 * and `coerceInputValues` does not rescue that case.
 */
@Serializable
data class PlaybackItem(
    val media_id: String = "",
    val media_type: String = "movie",
    val title: String = "",
    val tmdb_id: Long? = null,
    val backdrop_path: String? = null,
    val season_number: Int? = null,
    val episode_number: Int? = null,
    val episode_id: Long? = null,
    val progress_seconds: Long = 0,
    val total_seconds: Long = 0,
    val is_finished: Int = 0,
    val updated_at: Long = 0,
) {
    val isFinished: Boolean get() = is_finished != 0

    val progressFraction: Float
        get() = if (total_seconds <= 0) 0f else (progress_seconds.toFloat() / total_seconds).coerceIn(0f, 1f)
}

/**
 * One row of `GET /user/library/:listType`.
 *
 * The worker maps the DB row to `{ key, type, tmdb_id, title, season, episode,
 * date }` — this is NOT the same shape as the POST body. See [LibraryUpsertBody].
 */
@Serializable
data class LibraryItem(
    val key: String = "",
    val type: String = "movie",
    val tmdb_id: Long? = null,
    val title: String = "",
    val season: Int? = null,
    val episode: Int? = null,
    val date: String? = null,
) {
    /** `key` is the DB `media_id`; exposed under the write-side name for callers. */
    val mediaId: String get() = key
    val mediaType: String get() = type
    val isSeries: Boolean get() = type != "movie"
}

/** Body for `POST /user/library/:listType` (the write-side shape). */
@Serializable
data class LibraryUpsertBody(
    val media_id: String,
    val media_type: String,
    val title: String,
    val tmdb_id: Long? = null,
    val season_number: Int? = null,
    val episode_number: Int? = null,
    val backdrop_path: String? = null,
)

@Serializable
data class UserSettings(
    val default_sub_lang: String? = null,
    val subtitle_font_size: Int? = null,
    val subtitle_color: String? = null,
    val subtitle_bg_style: String? = null,
    val subtitle_bg_opacity: Int? = null,
    val subtitle_position: Int? = null,
    val subtitle_delay_ms: Int? = null,
    val default_aspect_ratio: String? = null,
    val show_remaining_time: Int? = null,
)

/* ------------------------------ Vidara ---------------------------- */

@Serializable
data class VidaraSubtitle(
    val id: Long = 0,
    val status: Int = 0,
    val file_id: Long = 0,
    val type: Int = 0,
    val file_path: String = "",
    val language: String = "",
)

@Serializable
data class VidaraStreamMetadata(
    val default_sub_lang: String = "",
    val filecode: String = "",
    val streaming_url: String = "",
    val subtitles: List<VidaraSubtitle> = emptyList(),
    val thumbnail: String = "",
    val title: String = "",
    val vast_ads: String = "",
)

/* --------------------------- Auth / user -------------------------- */

@Serializable
data class AuthUser(
    val id: Long = 0,
    val email: String = "",
    val display_name: String? = null,
    val role: String? = null,
    val created_at: Long? = null,
)

@Serializable
data class AuthResponse(
    val token: String = "",
    val user: AuthUser? = null,
)

/* ----------------------------- Cast ------------------------------- */

@Serializable
data class CastMember(
    val name: String = "",
    val character: String = "",
    val profile_path: String? = null,
)

/* ------------------------- Response wrappers ---------------------- */

@Serializable
data class PagedMovies(val page: Int = 1, val limit: Int = 20, val items: List<Movie> = emptyList())

@Serializable
data class MovieList(val items: List<Movie> = emptyList())

@Serializable
data class PagedSeries(val page: Int = 1, val limit: Int = 20, val items: List<Series> = emptyList())

@Serializable
data class SeriesList(val items: List<Series> = emptyList())

@Serializable
data class SeasonList(val items: List<Season> = emptyList())

@Serializable
data class EpisodeList(val items: List<Episode> = emptyList())

@Serializable
data class LinkList(val items: List<Link> = emptyList())

/**
 * One row of `/search`. The worker returns a hybrid of Movie and Series per
 * row, so every field is nullable with a safe default.
 */
@Serializable
data class SearchHit(
    val id: Long = 0,
    val tmdb_id: Long? = null,
    val imdb_id: String? = null,
    val title: String? = null,
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
    val cast: String? = null,
    val genres: String? = null,
    val type: String? = null,
    val created_at: Long? = null,
) {
    val year: Int? get() = release_year ?: first_release_year
    val isSeries: Boolean get() = type == "series" || total_seasons != null
}

@Serializable
data class SearchResult(val items: List<SearchHit> = emptyList())

/** One row of `/tmdb-search` (used by the "request a title" flow). */
@Serializable
data class TmdbHit(
    val tmdb_id: Long = 0,
    val type: String? = null,
    val media_type: String? = null,
    val title: String? = null,
    val name: String? = null,
    val overview: String? = null,
    val poster_path: String? = null,
    val backdrop_path: String? = null,
    val release_year: Int? = null,
    val first_air_date: String? = null,
    val vote_average: Double? = null,
) {
    val displayTitle: String get() = title ?: name ?: "Unknown"
}

@Serializable
data class TmdbSearchResult(val results: List<TmdbHit> = emptyList())

@Serializable
data class FilterOptions(
    val genres: List<String> = emptyList(),
    val years: List<Int> = emptyList(),
    val languages: List<String> = emptyList(),
)

@Serializable
data class DownloadOptions(
    val items: List<DownloadFile> = emptyList(),
    val qualities: List<String>? = null,
    val total_episodes: Int? = null,
)

@Serializable
data class PlaybackList(val items: List<PlaybackItem> = emptyList())

@Serializable
data class LibraryList(val items: List<LibraryItem> = emptyList())

/* ------------------------- Request bodies ------------------------- */

/**
 * Body for `POST /reports`. The worker requires `contentType`, `issueType`
 * and the matching id — a `{type,id,reason}` body is rejected with HTTP 400.
 */
@Serializable
data class ReportBody(
    val contentType: String,
    val issueType: String,
    val movieId: Long? = null,
    val episodeId: Long? = null,
    val note: String? = null,
)

/** Body for `POST /requests`. The worker reads `title`, `tmdbId`, `note`. */
@Serializable
data class RequestBody(
    val title: String,
    val tmdbId: Long? = null,
    val note: String? = null,
)

/* ---------------------- Internal helper JSON ---------------------- */

private val listJson = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }

/** Parses the worker's JSON-encoded string arrays, e.g. `"[\"Action\"]"`. */
fun parseStringList(raw: String?): List<String> {
    if (raw.isNullOrBlank()) return emptyList()
    return runCatching { listJson.decodeFromString<List<String>>(raw) }.getOrDefault(emptyList())
}

/** Parses the `cast` column, which is a JSON-encoded array of cast members. */
fun parseCast(raw: String?): List<CastMember> {
    if (raw.isNullOrBlank()) return emptyList()
    return runCatching { listJson.decodeFromString<List<CastMember>>(raw) }.getOrDefault(emptyList())
}
