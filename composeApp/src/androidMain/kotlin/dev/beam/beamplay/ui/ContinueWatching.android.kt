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