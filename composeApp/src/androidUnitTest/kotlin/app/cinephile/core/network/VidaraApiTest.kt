package app.cinephile.core.network

import app.cinephile.core.model.Link
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Vidara is the only piece that has to run on the device itself: the `.m3u8`
 * token is IP-locked to whoever called `/api/stream`.
 *
 * The live test is best-effort — if the catalogue has no links for the newest
 * titles it skips instead of failing, because that is a content gap, not a bug.
 */
class VidaraApiTest {

    @Test
    fun urlHelpersNormaliseBothHostsAndPaths() {
        val api = VidaraApi(httpClient(InMemoryBeamTokenStore(), isDebug = false))

        assertEquals("https://vidaraa.cc/e/abc123", api.normalizeUrl("https://vidaraa.cc/v/abc123"))
        assertEquals("https://vidara.to/e/xyz789", api.normalizeUrl("https://vidara.to/v/xyz789"))
        assertEquals("abc123", api.extractFilecode("https://vidaraa.cc/e/abc123"))
        assertEquals("https://vidara.to", api.originOf("https://vidara.to/e/abc123"))
    }

    @Test
    fun extractsPlayableManifestFromARealLink() = runTest {
        val client = httpClient(InMemoryBeamTokenStore(), isDebug = true)
        val beam = BeamApi(client)
        val vidara = VidaraApi(client)

        var link: Link? = null
        for (movie in beam.getRecentMovies(limit = 20).items.take(8)) {
            val links = runCatching { beam.getMovieLinks(movie.id) }.getOrNull()?.items
            if (!links.isNullOrEmpty()) {
                link = links.first()
                break
            }
        }

        val embedUrl = link?.url
        if (embedUrl.isNullOrBlank()) {
            client.close()
            return@runTest // no embeddable links in the current catalogue window
        }

        val metadata = vidara.getStreamMetadata(embedUrl)

        assertTrue(metadata.streaming_url.startsWith("http"), "streaming_url unsupported: ${metadata.streaming_url}")
        assertTrue(metadata.streaming_url.contains(".m3u8"), "streaming_url is not an HLS playlist: ${metadata.streaming_url}")
        assertTrue(metadata.filecode.isNotBlank(), "filecode came back blank")
        client.close()
    }
}
