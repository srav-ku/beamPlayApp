package app.cinephile.ui

import android.content.Context

private const val PREFS = "cinephile_interests"
private const val KEY = "genres"

actual fun saveInterests(genres: List<String>) {
    val c = AppContextHolder.ctx ?: return
    c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY, genres.joinToString("|"))
        .apply()
}

actual fun loadInterests(): List<String> {
    val c = AppContextHolder.ctx ?: return emptyList()
    val raw = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "") ?: ""
    return raw.split("|").filter { it.isNotBlank() }
}