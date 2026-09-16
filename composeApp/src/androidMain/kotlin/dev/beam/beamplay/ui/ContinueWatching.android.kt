package dev.beam.beamplay.ui

import android.content.Context

/** App context, captured at startup so common code can reach the store. */
object AppContextHolder {
    @Volatile
    var ctx: Context? = null
}

actual fun localContinueWatching(): List<ContinueItem> {
    val c = AppContextHolder.ctx ?: return emptyList()
    return dev.beam.beamplay.ui.player.PlaybackStore.continueItems(c)
}

actual fun rememberContinueArt(streamUrl: String, art: String?) {
    val c = AppContextHolder.ctx ?: return
    dev.beam.beamplay.ui.player.PlaybackStore.putArt(c, streamUrl, art)
}