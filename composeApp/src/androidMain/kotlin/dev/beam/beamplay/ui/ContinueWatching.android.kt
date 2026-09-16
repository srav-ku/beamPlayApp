package dev.beam.beamplay.ui

import android.content.Context

/** App context, captured at startup so common code can reach the store. */
object AppContextHolder {
    @Volatile
    var ctx: Context? = null
}

private fun store(): dev.beam.beamplay.ui.player.PlaybackStore? =
    if (AppContextHolder.ctx != null) dev.beam.beamplay.ui.player.PlaybackStore else null

actual fun localContinueWatching(): List<ContinueItem> {
    val c = AppContextHolder.ctx ?: return emptyList()
    return dev.beam.beamplay.ui.player.PlaybackStore.continueItems(c)
}

actual fun rememberContinueArt(streamUrl: String, art: String?) {
    val c = AppContextHolder.ctx ?: return
    dev.beam.beamplay.ui.player.PlaybackStore.putArt(c, streamUrl, art)
}

actual fun removeContinueWatching(sourceUrl: String) {
    val c = AppContextHolder.ctx ?: return
    dev.beam.beamplay.ui.player.PlaybackStore.clear(c, sourceUrl)
}

actual fun markContinueWatchingWatched(sourceUrl: String, title: String, durationMs: Long) {
    val c = AppContextHolder.ctx ?: return
    dev.beam.beamplay.ui.player.PlaybackStore.markCompleted(c, sourceUrl, title, durationMs)
}