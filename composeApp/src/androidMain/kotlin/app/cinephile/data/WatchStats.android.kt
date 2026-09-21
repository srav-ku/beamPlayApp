package app.cinephile.data

import app.cinephile.ui.player.PlaybackStore
import coil3.SingletonImageLoader
import java.time.Instant
import java.time.ZoneId

private fun ctx() = app.cinephile.ui.AppContextHolder.ctx

actual fun localWatchRecords(): List<WatchRecord> {
    val context = ctx() ?: return emptyList()
    return PlaybackStore.snapshot(context)
        .map { entry ->
            WatchRecord(
                title = entry.title,
                sourceUrl = entry.url.orEmpty(),
                lastUrl = entry.url.orEmpty(),
                art = entry.art,
                positionMs = entry.positionMs,
                durationMs = entry.durationMs,
                updatedAt = entry.updatedAt,
                completed = entry.completed,
            )
        }
        .sortedByDescending { it.updatedAt }
}

actual fun clearWatchHistory() {
    val context = ctx() ?: return
    PlaybackStore.snapshot(context).forEach { PlaybackStore.clear(context, it.key) }
}

actual fun clearImageCache() {
    val context = ctx() ?: return
    runCatching {
        val loader = SingletonImageLoader.get(context)
        loader.memoryCache?.clear()
        loader.diskCache?.clear()
    }
}

actual fun yearOfInstant(ms: Long): Int =
    Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).year