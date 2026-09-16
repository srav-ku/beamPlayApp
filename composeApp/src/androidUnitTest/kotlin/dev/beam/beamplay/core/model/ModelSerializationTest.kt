package dev.beam.beamplay.core.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Regression guards for the exact shapes the worker actually returns.
 * These mirror `beam-worker.js`, not the (partly wrong) playbook prose.
 */
class ModelSerializationTest {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
        isLenient = true
    }

    @Test
    fun playbackDecodesSqliteIntegerIsFinished() {
        // The worker SELECTs is_finished straight out of SQLite, so it is 0/1.
        val raw = """
            {"items":[{"media_id":"movie:1","media_type":"movie","title":"X","tmdb_id":1,
            "backdrop_path":null,"season_number":null,"episode_number":null,"episode_id":null,
            "progress_seconds":10,"total_seconds":100,"is_finished":0,"updated_at":123}]}
        """.trimIndent()

        val list = json.decodeFromString<PlaybackList>(raw)

        assertEquals(1, list.items.size)
        assertFalse(list.items.first().isFinished)
        assertEquals(0.1f, list.items.first().progressFraction, 0.0001f)
    }

    @Test
    fun libraryDecodesTheGetShapeWithKeyAndType() {
        val raw = """
            {"items":[{"key":"tv:1399:1:2","type":"series","tmdb_id":1399,"title":"GoT",
            "season":1,"episode":2,"date":"2026-01-01T00:00:00.000Z"}]}
        """.trimIndent()

        val list = json.decodeFromString<LibraryList>(raw)
        val item = list.items.first()

        assertEquals("tv:1399:1:2", item.mediaId)
        assertEquals("series", item.mediaType)
        assertTrue(item.isSeries)
    }

    @Test
    fun reportBodyMatchesTheWorkerContract() {
        val encoded = json.encodeToString(
            ReportBody(contentType = "movie", issueType = "broken", movieId = 42L),
        )

        assertTrue(encoded.contains("\"contentType\":\"movie\""), encoded)
        assertTrue(encoded.contains("\"issueType\":\"broken\""), encoded)
        assertTrue(encoded.contains("\"movieId\":42"), encoded)
        assertFalse(encoded.contains("episodeId"), "null fields must be omitted: $encoded")
    }

    @Test
    fun requestBodyCarriesTmdbId() {
        val encoded = json.encodeToString(RequestBody(title = "Foo", tmdbId = 9L))

        assertTrue(encoded.contains("\"title\":\"Foo\""), encoded)
        assertTrue(encoded.contains("\"tmdbId\":9"), encoded)
    }

    @Test
    fun libraryUpsertBodyMatchesThePostContract() {
        val encoded = json.encodeToString(
            LibraryUpsertBody(media_id = "movie:1", media_type = "movie", title = "X"),
        )

        assertTrue(encoded.contains("\"media_id\":\"movie:1\""), encoded)
        assertTrue(encoded.contains("\"media_type\":\"movie\""), encoded)
    }
}
