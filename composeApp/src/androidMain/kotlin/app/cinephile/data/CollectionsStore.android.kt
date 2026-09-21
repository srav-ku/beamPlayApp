package app.cinephile.data

import android.content.Context

private const val PREFS = "cinephile_collections"
private const val KEY = "***"

actual fun loadCollectionsJson(): String {
    val c = app.cinephile.ui.AppContextHolder.ctx ?: return ""
    return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "") ?: ""
}

actual fun saveCollectionsJson(json: String) {
    val c = app.cinephile.ui.AppContextHolder.ctx ?: return
    c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY, json)
        .apply()
}

actual fun nowMillis(): Long = System.currentTimeMillis()
