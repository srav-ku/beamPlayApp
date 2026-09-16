package dev.beam.beamplay.ui

/**
 * One locally remembered stream.
 *
 * [sourceUrl] is the stable identity - the Vidara embed page. It never expires,
 * so resuming re-resolves a fresh, unexpired manifest exactly like the Play
 * button does. [lastUrl] is the manifest that was actually playing, kept only as
 * an offline fallback.
 */
data class ContinueItem(
    val title: String,
    val sourceUrl: String,
    val lastUrl: String,
    val positionMs: Long,
    val durationMs: Long,
    val updatedAt: Long,
    val art: String? = null,
)

/** Reads the offline playback memory. Empty on platforms without one. */
expect fun localContinueWatching(): List<ContinueItem>

/** Remembers the artwork path for a stream, for the Continue Watching rail. */
expect fun rememberContinueArt(streamUrl: String, art: String?)

/** Forget a stream: it leaves Continue Watching. */
expect fun removeContinueWatching(sourceUrl: String)

/** Mark a stream finished: it leaves Continue Watching and enters History. */
expect fun markContinueWatchingWatched(sourceUrl: String, title: String, durationMs: Long)