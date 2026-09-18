package app.cinephile.ui

import android.content.Context

/** App context, captured at startup so common code can reach the store. */
object AppContextHolder {
    @Volatile
    var ctx: Context? = null
}

private fun store(): app.cinephile.ui.player.PlaybackStore? =
    if (AppContextHolder.ctx != null) app.cinephile.ui.player.PlaybackStore else null

actual fun localContinueWatching(): List<ContinueItem> {
    val c = AppContextHolder.ctx ?: return emptyList()
    return app.cinephile.ui.player.PlaybackStore.continueItems(c)
}

actual fun rememberContinueArt(streamUrl: String, art: String?) {
    val c = AppContextHolder.ctx ?: return
    app.cinephile.ui.player.PlaybackStore.putArt(c, streamUrl, art)
}

actual fun removeContinueWatching(sourceUrl: String) {
    val c = AppContextHolder.ctx ?: return
    app.cinephile.ui.player.PlaybackStore.clear(c, sourceUrl)
}

actual fun markContinueWatchingWatched(sourceUrl: String, title: String, durationMs: Long) {
    val c = AppContextHolder.ctx ?: return
    app.cinephile.ui.player.PlaybackStore.markCompleted(c, sourceUrl, title, durationMs)
}