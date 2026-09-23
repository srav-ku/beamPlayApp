package app.cinephile.data

import android.content.Context

private const val PREFS = "cinephile_title_flags"

private const val KEY = "***"

private fun prefs() = app.cinephile.ui.AppContextHolder.ctx
    ?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

actual fun readTitleFlags(): String = prefs()?.getString(KEY, "") ?: ""

actual fun writeTitleFlags(json: String) {
    prefs()?.edit()?.putString(KEY, json)?.apply()
}