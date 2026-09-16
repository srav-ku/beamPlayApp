package dev.beam.beamplay.ui.player

import androidx.compose.runtime.Composable

/** One selectable subtitle track handed to the player. */
data class PlayerSubtitle(
    val label: String,
    val url: String,
)

/** One selectable audio or subtitle track discovered inside the stream. */
data class PlayerTrack(
    val id: String,
    val label: String,
    val selected: Boolean,
)

/**
 * Native player screen.
 *
 * Implemented per platform (Android uses Media3/ExoPlayer, hardware decoded).
 * The stream URL is the IP-locked `.m3u8` resolved on the device, so playback
 * happens on the phone's own connection — same rule as the website.
 */
@Composable
expect fun BeamPlayerScreen(
    title: String,
    streamUrl: String,
    subtitles: List<PlayerSubtitle>,
    startPositionMs: Long,
    onBack: () -> Unit,
    onProgress: (positionMs: Long, durationMs: Long) -> Unit,
)

/**
 * Tracks whether the full-screen player currently owns the screen, so the host
 * activity can restore locked-portrait whenever the player is NOT open. Without
 * this, backgrounding while the player is open leaves the app in sensor-landscape.
 */
object PlayerSession {
    var isOpen: Boolean = false
}