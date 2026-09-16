package dev.beam.beamplay.core.network

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Live smoke tests for every Beam worker endpoint family Phase 1 exposes.
 * These hit the real `beamplay.beam-api.workers.dev` backend.
 */
class BeamApiTest {

    private val api = BeamApi(httpClient(InMemoryBeamTokenStore(), isDebug = true))

    @Test
    fun recentMoviesReturnItems() = runTest {
        val recent = api.getRecentMovies(limit = 5)

        assertTrue(recent.items.isNotEmpty(), "recent movies came back empty")
        assertTrue(recent.items.first().title.isNotBlank(), "first movie has a blank title")
    }

    @Test
    fun movieByIdHasNonBlankTitle() = runTest {
        val any = api.getMovies(limit = 1).items.first()
        val movie = api.getMovie(any.id)

        assertTrue(movie.title.isNotBlank(), "movie ${any.id} has a blank title")
    }

    @Test
    fun seriesListAndSeasonsResolve() = runTest {
        val anySeries = api.getSeries(limit = 1).items.first()
        val series = api.getSeries(anySeries.id)
        val seasons = api.getSeasons(anySeries.id)

        assertTrue(series.title.isNotBlank(), "series ${anySeries.id} has a blank title")
        assertTrue(seasons.items.isNotEmpty(), "series ${anySeries.id} has no seasons")
    }

    @Test
    fun filtersArePopulated() = runTest {
        val filters = api.getFilters()

        assertTrue(filters.genres.isNotEmpty(), "no genres returned")
        assertTrue(filters.years.isNotEmpty(), "no years returned")
    }

    @Test
    fun searchFindsResultsForCommonQuery() = runTest {
        val results = api.search("bad")

        assertTrue(results.items.isNotEmpty(), "search('bad') came back empty")
    }

    @Test
    fun movieLinksPointAtVidaraEmbeds() = runTest {
        val movie = api.getRecentMovies(limit = 10).items.first()
        val links = api.getMovieLinks(movie.id)

        assertTrue(links.items.isNotEmpty(), "no stream links for movie ${movie.id}")
        assertTrue(links.items.first().url.contains("/e/"), "link is not a Vidara embed: ${links.items.first().url}")
    }
}
