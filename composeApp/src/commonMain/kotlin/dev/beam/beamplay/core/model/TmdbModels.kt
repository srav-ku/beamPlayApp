package dev.beam.beamplay.core.model

import kotlinx.serialization.Serializable

/* ------------------------------------------------------------------ */
/*  TMDB models — used directly by the Home screen (no worker proxy).  */
/* ------------------------------------------------------------------ */

@Serializable
data class TmdbItem(
    val id: Long = 0,
    val media_type: String? = null,
    val title: String? = null,
    val name: String? = null,
    val poster_path: String? = null,
    val backdrop_path: String? = null,
    val release_date: String? = null,
    val first_air_date: String? = null,
    val vote_average: Double? = null,
    val overview: String? = null,
    val genre_ids: List<Long>? = null,
) {
    val displayTitle: String get() = title ?: name ?: "Unknown"
    val displayYear: String get() = (release_date ?: first_air_date)?.take(4) ?: ""
    val isMovie: Boolean get() = media_type == "movie" || (media_type == null && title != null)
}

/** Generic TMDB list envelope used by trending, movie, tv and similar endpoints. */
@Serializable
data class TmdbPage(
    val page: Int = 1,
    val total_pages: Int = 1,
    val results: List<TmdbItem> = emptyList(),
)

@Serializable
data class TmdbSeason(
    val id: Long = 0,
    val season_number: Int = 0,
    val name: String = "",
    val overview: String? = null,
    val episodes: List<TmdbEpisode> = emptyList(),
)

@Serializable
data class TmdbEpisode(
    val id: Long = 0,
    val episode_number: Int = 0,
    val season_number: Int = 0,
    val name: String = "",
    val overview: String? = null,
    val still_path: String? = null,
    val runtime: Int? = null,
    val air_date: String? = null,
)

@Serializable
data class TmdbMovieDetails(
    val id: Long = 0,
    val imdb_id: String? = null,
    val title: String = "",
    val overview: String? = null,
    val poster_path: String? = null,
    val backdrop_path: String? = null,
    val release_date: String? = null,
    val runtime: Int? = null,
    val vote_average: Double? = null,
    val genres: List<TmdbGenre> = emptyList(),
    val credits: TmdbCredits? = null,
    val videos: TmdbVideos? = null,
    val similar: TmdbPage? = null,
    val production_companies: List<TmdbCompany> = emptyList(),
)

@Serializable
data class TmdbTvDetails(
    val id: Long = 0,
    val name: String = "",
    val overview: String? = null,
    val poster_path: String? = null,
    val backdrop_path: String? = null,
    val first_air_date: String? = null,
    val number_of_seasons: Int = 0,
    val number_of_episodes: Int = 0,
    val vote_average: Double? = null,
    val genres: List<TmdbGenre> = emptyList(),
    val seasons: List<TmdbSeason> = emptyList(),
    val credits: TmdbCredits? = null,
    val videos: TmdbVideos? = null,
    val similar: TmdbPage? = null,
    val episode_run_time: List<Int> = emptyList(),
    val production_companies: List<TmdbCompany> = emptyList(),
)

@Serializable
data class TmdbCompany(val id: Long = 0, val name: String = "")

@Serializable
data class TmdbGenre(val id: Long = 0, val name: String = "")

@Serializable
data class TmdbCredits(
    val cast: List<TmdbCast> = emptyList(),
    val crew: List<TmdbCrew> = emptyList(),
)

@Serializable
data class TmdbCast(
    val id: Long = 0,
    val name: String = "",
    val character: String? = null,
    val profile_path: String? = null,
    val order: Int = 0,
)

@Serializable
data class TmdbCrew(
    val id: Long = 0,
    val name: String = "",
    val job: String? = null,
    val department: String? = null,
    val profile_path: String? = null,
)

@Serializable
data class TmdbVideos(val results: List<TmdbVideo> = emptyList())

@Serializable
data class TmdbVideo(
    val id: String = "",
    val key: String = "",
    val site: String = "",
    val type: String = "",
    val name: String = "",
    val official: Boolean = false,
) {
    val isYouTubeTrailer: Boolean get() = site == "YouTube" && type == "Trailer"
}
