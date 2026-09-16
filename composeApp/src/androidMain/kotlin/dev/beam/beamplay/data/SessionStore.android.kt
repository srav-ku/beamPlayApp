package dev.beam.beamplay.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class AndroidSessionStore(context: Context) : SessionStore {
    private val prefs: SharedPreferences = context.getSharedPreferences("beam_session", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    override fun load(): Session? = try {
        prefs.getString("session", null)?.let { json.decodeFromString<Session>(it) }
    } catch (_: Exception) {
        null
    }

    override fun save(session: Session?) {
        prefs.edit().putString("session", session?.let { json.encodeToString(it) }).apply()
    }
}
