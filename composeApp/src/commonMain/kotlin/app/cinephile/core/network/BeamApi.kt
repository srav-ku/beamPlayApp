package app.cinephile.core.network

import app.cinephile.core.model.AuthResponse
import app.cinephile.core.model.DownloadOptions
import app.cinephile.core.model.Entitlements
import app.cinephile.core.model.Episode
import app.cinephile.core.model.EpisodeList
import app.cinephile.core.model.FilterOptions
import app.cinephile.core.model.LibraryList
import app.cinephile.core.model.LibraryUpsertBody
import app.cinephile.core.model.LinkList
import app.cinephile.core.model.Movie
import app.cinephile.core.model.MovieList
import app.cinephile.core.model.PagedMovies
import app.cinephile.core.model.PagedSeries
import app.cinephile.core.model.PlaybackItem
import app.cinephile.core.model.PlaybackList
import app.cinephile.core.model.ReportBody
import app.cinephile.core.model.RequestBody
import app.cinephile.core.model.SearchResult
import app.cinephile.core.model.SeasonList
import app.cinephile.core.model.Series
import app.cinephile.core.model.SeriesList
import app.cinephile.core.model.TmdbSearchResult
import app.cinephile.core.model.UserSettings
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Typed client for every BeamPlay Cloudflare Worker endpoint.
 *
 * The `Authorization` header is injected by the client's auth plugin (see
 * [beamAuthPlugin]); this class never touches the token itself.
 */
class BeamApi(private val client: HttpClient) {

    private val base = BEAM_BASE_URL

    /* ------------------------------ Movies ------------------------------ */

    suspend fun getMovies(
        page: Int = 1,
        limit: Int = 20,
        q: String? = null,
        genre: String? = null,
        year: String? = null,
        language: String? = null,
        sort: String? = null,
    ): PagedMovies = client.get("$base/movies") {
        parameter("page", page)
        parameter("limit", limit)
        optional("q" to q, "genre" to genre, "year" to year, "language" to language, "sort" to sort)
    }.body()

    suspend fun getRecentMovies(limit: Int = 20): MovieList =
        client.get("$base/movies/recent") { parameter("limit", limit) }.body()

    suspend fun getMovie(id: Long): Movie = client.get("$base/movies/$id").body()

    suspend fun getMovieByTmdb(tmdbId: Long): Movie = client.get("$base/movies/tmdb/$tmdbId").body()

    suspend fun getMovieLinks(id: Long): LinkList = client.get("$base/movies/$id/links").body()

    /* ------------------------------ Series ------------------------------ */

    suspend fun getSeries(
        page: Int = 1,
        limit: Int = 20,
        q: String? = null,
        genre: String? = null,
        year: String? = null,
        language: String? = null,
        sort: String? = null,
    ): PagedSeries = client.get("$base/series") {
        parameter("page", page)
        parameter("limit", limit)
        optional("q" to q, "genre" to genre, "year" to year, "language" to language, "sort" to sort)
    }.body()

    suspend fun getRecentSeries(limit: Int = 20): SeriesList =
        client.get("$base/series/recent") { parameter("limit", limit) }.body()

    suspend fun getSeries(id: Long): Series = client.get("$base/series/$id").body()

    suspend fun getSeriesByTmdb(tmdbId: Long): Series = client.get("$base/series/tmdb/$tmdbId").body()

    suspend fun getSeasons(seriesId: Long): SeasonList = client.get("$base/series/$seriesId/seasons").body()

    /* ----------------------------- Episodes ----------------------------- */

    suspend fun getEpisodes(seasonId: Long): EpisodeList = client.get("$base/seasons/$seasonId/episodes").body()

    suspend fun getEpisode(id: Long): Episode = client.get("$base/episodes/$id").body()

    suspend fun getEpisodeLinks(id: Long): LinkList = client.get("$base/episodes/$id/links").body()

    /* -------------------------- Search / filters ------------------------ */

    suspend fun search(q: String): SearchResult =
        client.get("$base/search") { parameter("q", q) }.body()

    suspend fun tmdbSearch(q: String): TmdbSearchResult =
        client.get("$base/tmdb-search") { parameter("q", q) }.body()

    suspend fun getFilters(): FilterOptions = client.get("$base/filters").body()

    /** What this user may do. Server-driven, so the paywall can be switched without an APK update. */
    suspend fun getEntitlements(): Entitlements = client.get("$base/user/entitlements").body()

    /* ----------------------------- Downloads ---------------------------- */

    suspend fun getDownloadOptions(type: String, id: Long): DownloadOptions =
        client.get("$base/downloads/options/$type/$id").body()

    /* -------------------------------- Auth ------------------------------ */

    /** Posts the Firebase ID token; the worker verifies it and derives the identity. */
    suspend fun authGoogleToken(idToken: String, displayName: String): AuthResponse =
        client.post("$base/auth/google") {
            contentType(ContentType.Application.Json)
            setBody(GoogleTokenBody(idToken = idToken, displayName = displayName))
        }.body()

    suspend fun authGoogle(email: String, displayName: String, firebaseUid: String): AuthResponse =
        client.post("$base/auth/google") {
            contentType(ContentType.Application.Json)
            setBody(GoogleAuthBody(email = email, displayName = displayName, firebaseUid = firebaseUid))
        }.body()

    /* --------------------------- User (authed) -------------------------- */

    suspend fun getSettings(): UserSettings =
        client.get("$base/user/settings").body()

    suspend fun updateSettings(patch: UserSettings) {
        val response = client.put("$base/user/settings") {
            contentType(ContentType.Application.Json)
            setBody(patch)
        }
        response.requireSuccess("updateSettings")
    }

    suspend fun getPlayback(): PlaybackList =
        client.get("$base/user/playback").body()

    suspend fun upsertPlayback(item: PlaybackItem) {
        val response = client.post("$base/user/playback") {
            contentType(ContentType.Application.Json)
            setBody(item)
        }
        response.requireSuccess("upsertPlayback")
    }

    suspend fun deletePlayback(mediaId: String) {
        val response = client.delete("$base/user/playback/$mediaId")
        response.requireSuccess("deletePlayback")
    }

    suspend fun queueNextEpisode(seriesTmdbId: Long, season: Int, episode: Int) {
        val response = client.post("$base/user/playback/next") {
            contentType(ContentType.Application.Json)
            setBody(NextEpisodeBody(seriesTmdbId = seriesTmdbId, season = season, episode = episode))
        }
        response.requireSuccess("queueNextEpisode")
    }

    /** Returns the GET shape; use [LibraryUpsertBody] when writing. */
    suspend fun getLibrary(listType: String, page: Int = 1): LibraryList =
        client.get("$base/user/library/$listType") { parameter("page", page) }.body()

    suspend fun addToLibrary(listType: String, item: LibraryUpsertBody) {
        val response = client.post("$base/user/library/$listType") {
            contentType(ContentType.Application.Json)
            setBody(item)
        }
        response.requireSuccess("addToLibrary")
    }

    suspend fun removeFromLibrary(listType: String, mediaId: String) {
        val response = client.delete("$base/user/library/$listType/$mediaId")
        response.requireSuccess("removeFromLibrary")
    }

    /* --------------------------- Reports/requests ----------------------- */

    suspend fun report(body: ReportBody) {
        val response = client.post("$base/reports") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        response.requireSuccess("report")
    }

    suspend fun request(body: RequestBody) {
        val response = client.post("$base/requests") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        response.requireSuccess("request")
    }
}

/* ---------------------------- helpers ------------------------------ */

/** Appends only the query parameters that are non-null. */
private fun HttpRequestBuilder.optional(vararg pairs: Pair<String, Any?>) {
    pairs.forEach { (key, value) -> if (value != null) parameter(key, value) }
}

/**
 * The client runs with `expectSuccess = false`, so write helpers must check the
 * status themselves — otherwise a 400/401/500 is indistinguishable from success.
 */
private fun HttpResponse.requireSuccess(action: String) {
    if (status.value !in 200..299) {
        error("BeamApi.$action failed with HTTP ${status.value} ${status.description}")
    }
}

/** Body for `POST /auth/google`, matched to the worker's expected shape. */
@Serializable
private data class GoogleAuthBody(
    val email: String,
    val displayName: String,
    val firebaseUid: String,
)

/** Body for `POST /user/playback/next`. */
@Serializable
private data class NextEpisodeBody(
    @SerialName("series_tmdb_id") val seriesTmdbId: Long,
    @SerialName("current_season_number") val season: Int,
    @SerialName("current_episode_number") val episode: Int,
)

/** Body for `POST /auth/google` after the token-verification change. */
private data class GoogleTokenBody(
    val idToken: String,
    val displayName: String,
)
