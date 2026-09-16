package dev.beam.beamplay.ui

/**
 * One locally remembered stream, surfaced by the platform layer.
 * [streamUrl] identifies the exact stream (quality / language / episode),
 * so resuming always re-opens the stream the viewer actually watched.
 */
data class ContinueItem(
    val title: String,
    val streamUrl: String,
    val positionMs: Long,
    val durationMs: Long,
    val updatedAt: Long,
)

/** Reads the offline playback memory. Empty on platforms without one. */
expect fun localContinueWatching(): List<ContinueItem>