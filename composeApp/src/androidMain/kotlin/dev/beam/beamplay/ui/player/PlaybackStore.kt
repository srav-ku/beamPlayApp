package dev.beam.beamplay.ui.player

import android.content.Context
import dev.beam.beamplay.ui.ContinueItem
import org.json.JSONObject

/**
 * Local, offline playback memory. No database and no network calls.
 *
 * Keyed per *stream* (not per title): each quality / language stream, and each
 * series episode, keeps its own resume point. Opening the 1080p stream again
 * resumes exactly where that stream stopped.
 */
internal object PlaybackStore {

    private const val PREFS = "beam_playback_v1"
    private const val IDS = "keys"
    private const val MIN_SAVE_MS = 3_000L
    private const val COMPLETE_FRACTION = 0.99

    data class Entry(
        val key: String,
        val title: String,
        val positionMs: Long,
        val durationMs: Long,
        val updatedAt: Long,
        val completed: Boolean,
    )

    private fun prefs(ctx: Context) =
        ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun slot(key: String) = "p_" + Integer.toHexString(key.hashCode())

    private fun ids(ctx: Context): Set<String> =
        prefs(ctx).getStringSet(IDS, emptySet()) ?: emptySet()

    private fun write(
        ctx: Context,
        key: String,
        title: String,
        positionMs: Long,
        durationMs: Long,
        done: Boolean,
    ) {
        if (key.isBlank()) return
        val p = prefs(ctx)
        val o = JSONObject()
            .put("key", key)
            .put("title", title)
            .put("pos", positionMs.coerceAtLeast(0L))
            .put("dur", durationMs.coerceAtLeast(0L))
            .put("ts", System.currentTimeMillis())
            .put("done", done)
        val id = slot(key)
        p.edit().putString(id, o.toString()).putStringSet(IDS, ids(ctx) + id).apply()
    }

    /** Called every few seconds while playing, and on pause / leave. */
    fun saveProgress(ctx: Context, key: String, title: String, positionMs: Long, durationMs: Long) =
        write(ctx, key, title, positionMs, durationMs, false)

    /** Reached the end: leaves Continue Watching and moves to History. */
    fun markCompleted(ctx: Context, key: String, title: String, durationMs: Long) =
        write(ctx, key, title, durationMs, durationMs, true)

    fun entry(ctx: Context, key: String): Entry? =
        prefs(ctx).getString(slot(key), null)?.let { parse(key, it) }

    /** Where to resume this exact stream. 0 = start from the beginning. */
    fun resumeMs(ctx: Context, key: String): Long {
        val e = entry(ctx, key) ?: return 0L
        if (e.completed) return 0L
        return if (e.positionMs >= MIN_SAVE_MS) e.positionMs else 0L
    }

    fun isCompleted(ctx: Context, key: String): Boolean = entry(ctx, key)?.completed == true

    fun clear(ctx: Context, key: String) {
        val id = slot(key)
        prefs(ctx).edit().remove(id).putStringSet(IDS, ids(ctx) - id).apply()
    }

    fun continueWatching(ctx: Context): List<Entry> = read(ctx).filter {
        !it.completed && it.positionMs >= MIN_SAVE_MS &&
            (it.durationMs <= 0L ||
                it.positionMs.toDouble() / it.durationMs < COMPLETE_FRACTION)
    }

    fun history(ctx: Context): List<Entry> = read(ctx).filter { it.completed }

    fun snapshot(ctx: Context): List<Entry> = read(ctx)

    fun isFinished(positionMs: Long, durationMs: Long): Boolean =
        durationMs > 0L && positionMs.toDouble() / durationMs >= COMPLETE_FRACTION

    private fun read(ctx: Context): List<Entry> {
        val p = prefs(ctx)
        return ids(ctx)
            .mapNotNull { id -> p.getString(id, null)?.let { parse(id, it) } }
            .sortedByDescending { it.updatedAt }
    }

    fun continueItems(ctx: Context): List<ContinueItem> =
        continueWatching(ctx).map {
            ContinueItem(
                title = it.title.ifBlank { "Untitled" },
                streamUrl = it.key,
                positionMs = it.positionMs,
                durationMs = it.durationMs,
                updatedAt = it.updatedAt,
            )
        }

    private fun parse(fallbackKey: String, raw: String): Entry? = try {
        val o = JSONObject(raw)
        Entry(
            key = o.optString("key", fallbackKey),
            title = o.optString("title", ""),
            positionMs = o.optLong("pos", 0L),
            durationMs = o.optLong("dur", 0L),
            updatedAt = o.optLong("ts", 0L),
            completed = o.optBoolean("done", false),
        )
    } catch (e: Exception) {
        null
    }
}