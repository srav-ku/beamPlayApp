package dev.beam.beamplay.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

@Serializable
data class Session(
    val method: String, // "guest" | "email"
    val token: String? = null,
    val email: String? = null,
    val role: String? = null,
)

interface SessionStore {
    fun load(): Session?
    fun save(session: Session?)
}

object SessionManager {
    private lateinit var store: SessionStore
    private val _session = MutableStateFlow<Session?>(null)
    val session: StateFlow<Session?> = _session

    fun init(store: SessionStore) {
        if (this::store.isInitialized) return
        this.store = store
        _session.value = store.load()
        Api.authToken = _session.value?.token
    }

    fun set(session: Session?) {
        _session.value = session
        Api.authToken = session?.token
        store.save(session)
    }
}
