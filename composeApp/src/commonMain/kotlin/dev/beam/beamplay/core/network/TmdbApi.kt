package dev.beam.beamplay.core.network

import dev.beam.beamplay.core.model.TmdbItem
import dev.beam.beamplay.core.model.TmdbMovieDetails
import dev.beam.beamplay.core.model.TmdbPage
import dev.beam.beamplay.core.model.TmdbSeason
import dev.beam.beamplay.core.model.TmdbTvDetails
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

/**
 * Thin wrapper over the TMDB v3 API for the endpoints the website's Home
 * screen calls directly from the browser. `api_key` is appended to every
 * request; the client itself carries no auth header.
 */
class TmdbApi(private val client: HttpClient) {

    private val base = TMDB_BASE_URL

    /** Shared list loader for the trending, movie and tv list endpoints. */
    private suspend fun list(path: String, page: Int? = null): List<TmdbItem> =
        client.get("$base$path") {
            parameter("api_key", TMDB_API_KEY)
            parameter("language", "en-US")
            if (page != null) parameter("page", page)
        }.body<TmdbPage>().results

    suspend fun trendingAllWeek(): List<TmdbItem> = list("/trending/all/week")

    suspend fun trendingMoviesWeek(): List<TmdbItem> = list("/trending/movie/week")

    suspend fun trendingTvWeek(): List<TmdbItem> = list("/trending/tv/week")

    suspend fun movieNowPlaying(): List<TmdbItem> = list("/movie/now_playing")

    suspend fun tvAiringToday(): List<TmdbItem> = list("/tv/airing_today")

    suspend fun movieTopRated(): List<TmdbItem> = list("/movie/top_rated")

    suspend fun tvTopRated(): List<TmdbItem> = list("/tv/top_rated")

    suspend fun moviePopular(): List<TmdbItem> = list("/movie/popular")

    suspend fun tvPopular(): List<TmdbItem> = list("/tv/popular")

    suspend fun tvSeason(tvId: Long, season: Int): TmdbSeason =
        client.get("$base/tv/$tvId/season/$season") {
            parameter("api_key", TMDB_API_KEY)
            parameter("language", "en-US")
        }.body()

    suspend fun movieDetails(id: Long): TmdbMovieDetails =
        client.get("$base/movie/$id") {
            parameter("api_key", TMDB_API_KEY)
            parameter("language", "en-US")
            parameter("append_to_response", "credits,videos,similar")
        }.body()

    suspend fun tvDetails(id: Long): TmdbTvDetails =
        client.get("$base/tv/$id") {
            parameter("api_key", TMDB_API_KEY)
            parameter("language", "en-US")
            parameter("append_to_response", "credits,videos,similar")
        }.body()
}
