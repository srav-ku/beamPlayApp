package app.cinephile.data

import android.content.Context

private const val PREFS = "cinephile_search"

private const val KEY = "history"

private fun prefs() = app.cinephile.ui.AppContextHolder.ctx
    ?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

actual fun readSearchHistory(): String = prefs()?.getString(KEY, "") ?: ""

actual fun writeSearchHistory(json: String) {
    prefs()?.edit()?.putString(KEY, json)?.apply()
}

actual fun clearSearchHistoryStore() {
    prefs()?.edit()?.remove(KEY)?.apply()
}