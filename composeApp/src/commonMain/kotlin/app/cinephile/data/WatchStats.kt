package app.cinephile.data

import app.cinephile.ui.ContinueItem

/* ------------------------------------------------------------------ */
/*  Local watch record: what the profile's stats are built from.       */
/*  Everything here is on-device, so the profile works for guests too. */
/* ------------------------------------------------------------------ */

data class WatchRecord(
    val title: String,
    val sourceUrl: String,
    val lastUrl: String,
    val art: String?,
    val positionMs: Long,
    val durationMs: Long,
    val updatedAt: Long,
    val completed: Boolean,
) {
    /** 0f..1f, guarded against a missing or nonsense duration. */
    val progress: Float
        get() = if (durationMs <= 0L) 0f else (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)

    fun toContinueItem(): ContinueItem = ContinueItem(
        title = title,
        sourceUrl = sourceUrl,
        lastUrl = lastUrl,
        positionMs = positionMs,
        durationMs = durationMs,
        updatedAt = updatedAt,
        art = art,
    )
}

/** Every stream the app remembers, newest first - finished ones included. */
expect fun localWatchRecords(): List<WatchRecord>

/** Wipes the local playback memory: Continue Watching and history both empty. */
expect fun clearWatchHistory()

/** Drops cached artwork, for when the user wants the space back. */
expect fun clearImageCache()

/** Calendar year of an instant, so history can be grouped by year. */
expect fun yearOfInstant(ms: Long): Int