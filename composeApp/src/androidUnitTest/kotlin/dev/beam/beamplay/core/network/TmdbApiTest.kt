package dev.beam.beamplay.core.network

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Live smoke tests for the direct TMDB calls the Home screen depends on.
 */
class TmdbApiTest {

    private val api = TmdbApi(tmdbClient(isDebug = true))

    @Test
    fun trendingAllWeekReturnsAFullPage() = runTest {
        assertTrue(api.trendingAllWeek().size >= 10, "trending/all/week returned too few items")
    }

    @Test
    fun nowPlayingItemsHaveTitles() = runTest {
        val first = api.movieNowPlaying().first()

        assertTrue(first.displayTitle.isNotBlank(), "now-playing item has a blank title")
    }

    @Test
    fun tvSeasonResolvesEpisodes() = runTest {
        // Game of Thrones (1399) season 1 has 10 episodes.
        assertTrue(api.tvSeason(1399L, 1).episodes.size > 5, "GoT S1 episode list looks wrong")
    }

    @Test
    fun movieDetailsIncludeCredits() = runTest {
        // The Dark Knight (155).
        val details = api.movieDetails(155L)

        assertTrue(details.title.isNotBlank(), "movie details have a blank title")
        assertTrue(
            details.credits?.cast?.firstOrNull()?.name?.isNotBlank() == true,
            "movie credits are missing",
        )
    }
}
