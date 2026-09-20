package app.cinephile.data

import io.ktor.client.plugins.HttpTimeout
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.delay
import app.cinephile.core.network.servicesOrNull
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

class ApiException(message: String, val code: Int) : Exception(message)

object Api {
    const val BASE = "https://beamplay.beam-api.workers.dev"
    const val TMDB_IMG = "https://image.tmdb.org/t/p"
    const val TMDB_KEY = "5701acdc0ce0fd37222a3c46fa2ac9aa"

    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

        /**
     * Prefer the shared hardened client from ServiceContainer (OkHttp engine,
     * timeouts, browser User-Agent, connection-reset retry). The fallback is
     * only used if initServices() has not run yet.
     */
    private val fallbackClient = HttpClient {
        install(ContentNegotiation) { json(json) }
        install(HttpTimeout) {
            connectTimeoutMillis = 15_000
            requestTimeoutMillis = 30_000
            socketTimeoutMillis = 60_000
        }
    }

    /** Dedicated TMDB client (OkHttp, timeouts, browser UA). Never shares the Beam client. */
    private val tmdbClientInstance: HttpClient by lazy { app.cinephile.core.network.tmdbClient(isDebug = false) }

    private val client: HttpClient
        get() = tmdbClientInstance ?: fallbackClient

    var authToken: String? = null

    fun posterUrl(path: String?, width: String = "w500") =
        if (path.isNullOrBlank()) null else "$TMDB_IMG/$width$path"

    fun backdropUrl(path: String?, width: String = "w780") =
        if (path.isNullOrBlank()) null else "$TMDB_IMG/$width$path"

    // ── TMDB Direct Fetches ──────────────────────────────────────────────────

    /**
     * TMDB GET with a small retry loop: mobile networks routinely reset the
     * first TLS/HTTP2 connection ("Connection reset"), which a single retry
     * clears up.
     */
    private suspend fun fetchTmdbRaw(path: String): String {
        val sep = if (path.contains("?")) "&" else "?"
        val url = "https://api.themoviedb.org/3$path${sep}api_key=$TMDB_KEY"
        var lastError: Exception? = null
        repeat(3) { attempt ->
            try {
                return client.get(url) {
                    header(HttpHeaders.Accept, "application/json")
                }.bodyAsText()
            } catch (e: Exception) {
                lastError = e
                if (attempt < 2) delay(350L * (attempt + 1))
            }
        }
        throw lastError ?: IllegalStateException("TMDB request failed: $path")
    }

    private fun parseTmdbResults(jsonString: String, defaultType: String): List<MediaItem> {
        return try {
            val root = json.parseToJsonElement(jsonString).jsonObject
            val results = root["results"]?.jsonArray ?: return emptyList()
            results.mapNotNull { element ->
                try {
                    val obj = element.jsonObject
                    val id = obj["id"]?.jsonPrimitive?.longOrNull ?: return@mapNotNull null
                    val mediaType = obj["media_type"]?.jsonPrimitive?.content ?: defaultType
                    val title = obj["title"]?.jsonPrimitive?.content ?: obj["name"]?.jsonPrimitive?.content ?: ""
                    val poster = obj["poster_path"]?.jsonPrimitive?.content
                    val backdrop = obj["backdrop_path"]?.jsonPrimitive?.content ?: poster
                    val releaseDate = obj["release_date"]?.jsonPrimitive?.content ?: obj["first_air_date"]?.jsonPrimitive?.content
                    val year = releaseDate?.take(4)?.toIntOrNull()
                    val rating = obj["vote_average"]?.jsonPrimitive?.doubleOrNull ?: 0.0
                    val overview = obj["overview"]?.jsonPrimitive?.content

                    MediaItem(
                        id = 0,
                        tmdb_id = id,
                        title = title,
                        overview = overview,
                        poster_path = poster,
                        backdrop_path = backdrop,
                        release_year = year,
                        first_release_year = year,
                        tmdb_rating = rating,
                        type = mediaType,
                    )
                } catch (_: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            println("TMDB parsing error: ${e.message}")
            emptyList()
        }
    }

    suspend fun getTmdbTrendingMovies(): List<MediaItem> =
        parseTmdbResults(fetchTmdbRaw("/trending/movie/week"), "movie").take(10)

    suspend fun getTmdbTrendingSeries(): List<MediaItem> =
        parseTmdbResults(fetchTmdbRaw("/trending/tv/week"), "series").take(10)

    suspend fun getTmdbNowPlayingMovies(): List<MediaItem> =
        parseTmdbResults(fetchTmdbRaw("/movie/now_playing"), "movie").take(14)

    suspend fun getTmdbAiringTodaySeries(): List<MediaItem> =
        parseTmdbResults(fetchTmdbRaw("/tv/airing_today"), "series").take(14)

    suspend fun getTmdbTopRatedMovies(): List<MediaItem> =
        parseTmdbResults(fetchTmdbRaw("/movie/top_rated"), "movie").take(14)

    suspend fun getTmdbTopRatedSeries(): List<MediaItem> =
        parseTmdbResults(fetchTmdbRaw("/tv/top_rated"), "series").take(14)

    suspend fun getTmdbPopularMovies(): List<MediaItem> =
        parseTmdbResults(fetchTmdbRaw("/movie/popular"), "movie").take(14)

    suspend fun getTmdbPopularSeries(): List<MediaItem> =
        parseTmdbResults(fetchTmdbRaw("/tv/popular"), "series").take(14)

    // ── Backend Database Endpoints ──────────────────────────────────────────

    suspend fun getMovieByTmdb(tmdbId: Long): MediaItem? {
        return try {
            client.get("$BASE/movies/tmdb/$tmdbId").body()
        } catch (_: Exception) {
            null
        }
    }

    suspend fun getSeriesByTmdb(tmdbId: Long): MediaItem? {
        return try {
            client.get("$BASE/series/tmdb/$tmdbId").body()
        } catch (_: Exception) {
            null
        }
    }

    suspend fun getMovie(id: Long): MediaItem = client.get("$BASE/movies/$id").body()
    suspend fun getSeries(id: Long): MediaItem = client.get("$BASE/series/$id").body()

    suspend fun submitRequest(title: String, tmdbId: Long?, type: String): Boolean {
        return try {
            client.post("$BASE/requests") {
                contentType(ContentType.Application.Json)
                setBody(mapOf(
                    "title" to title,
                    "tmdb_id" to (tmdbId?.toString() ?: ""),
                    "type" to type,
                    "note" to "Requested from mobile app card click",
                ))
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun movies(
        page: Int = 1,
        limit: Int = 24,
        sort: String = "latest",
        genre: String? = null,
        year: String? = null,
        yearFrom: String? = null,
        yearTo: String? = null,
        language: String? = null,
        q: String? = null,
    ): MediaPage = client.get("$BASE/movies") {
        parameter("page", page); parameter("limit", limit); parameter("sort", sort)
        if (!genre.isNullOrBlank()) parameter("genre", genre)
        if (!year.isNullOrBlank()) parameter("year", year)
        if (!yearFrom.isNullOrBlank()) parameter("year_from", yearFrom)
        if (!yearTo.isNullOrBlank()) parameter("year_to", yearTo)
        if (!language.isNullOrBlank()) parameter("language", language)
        if (!q.isNullOrBlank()) parameter("q", q)
    }.body()

    suspend fun seriesList(
        page: Int = 1,
        limit: Int = 24,
        sort: String = "latest",
        genre: String? = null,
        year: String? = null,
        yearFrom: String? = null,
        yearTo: String? = null,
        language: String? = null,
        q: String? = null,
    ): MediaPage = client.get("$BASE/series") {
        parameter("page", page); parameter("limit", limit); parameter("sort", sort)
        if (!genre.isNullOrBlank()) parameter("genre", genre)
        if (!year.isNullOrBlank()) parameter("year", year)
        if (!yearFrom.isNullOrBlank()) parameter("year_from", yearFrom)
        if (!yearTo.isNullOrBlank()) parameter("year_to", yearTo)
        if (!language.isNullOrBlank()) parameter("language", language)
        if (!q.isNullOrBlank()) parameter("q", q)
    }.body()


    // --- Filter options straight from the database (not hardcoded) ---
    @Serializable
    data class FilterOptions(
        val genres: List<String> = emptyList(),
        val years: List<Int> = emptyList(),
        val languages: List<String> = emptyList(),
    )

    suspend fun getFilterOptions(): FilterOptions = client.get("$BASE/filters").body()
    suspend fun search(q: String): MediaList =
        client.get("$BASE/search") { parameter("q", q); parameter("type", "all") }.body()

    suspend fun login(email: String, password: String): AuthResponse = authPost("/auth/login", email, password)
    suspend fun signup(email: String, password: String): AuthResponse = authPost("/auth/signup", email, password)

    private suspend fun authPost(path: String, email: String, password: String): AuthResponse {
        try {
            return client.post(BASE + path) {
                contentType(ContentType.Application.Json)
                setBody(mapOf("email" to email, "password" to password))
            }.body()
        } catch (e: ClientRequestException) {
            val msg = try {
                e.response.body<Map<String, String>>()["error"] ?: "Invalid credentials"
            } catch (_: Exception) { "Invalid credentials" }
            throw ApiException(msg, e.response.status.value)
        }
    }

    suspend fun library(listType: String): MediaList = client.get("$BASE/user/library/$listType") {
        authToken?.let { header("Authorization", "Bearer $it") }
    }.body()
}
