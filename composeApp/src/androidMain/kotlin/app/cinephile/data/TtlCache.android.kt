package app.cinephile.data

import android.content.Context

private const val PREFS = "cinephile_cache"
private const val KEY = "entries"

actual fun readDiskCache(): String {
    val c = app.cinephile.ui.AppContextHolder.ctx ?: return ""
    return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "") ?: ""
}

actual fun writeDiskCache(json: String) {
    val c = app.cinephile.ui.AppContextHolder.ctx ?: return
    c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, json).apply()
}