package app.cinephile.data

import app.cinephile.core.model.Entitlements
import app.cinephile.core.network.BeamApi
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The single in-memory holder for what this user is allowed to do.
 *
 * Read once per launch, never awaited on a cold path: if the call fails the last known
 * values stay, and the very first launch falls back to "everything allowed", which matches
 * the free phase the product is in right now.
 */
object EntitlementsState {

    @Volatile
    var current: Entitlements? = null
        private set

    @Volatile
    var lastError: String? = null
        private set

    private val mutex = Mutex()

    /** Everything allowed - the safe default before the first successful fetch. */
    private val freeForAll = Entitlements()

    val effective: Entitlements get() = current ?: freeForAll

    val canStream: Boolean get() = effective.canStream
    val canDirectDownload: Boolean get() = effective.canDirectDownload
    val streamLockedForThisUser: Boolean get() = effective.streamLockedForThisUser
    val adsVisible: Boolean get() = effective.ads.show
    val planName: String get() = effective.plan?.name ?: "Free"

    suspend fun refresh(api: BeamApi) = mutex.withLock {
        runCatching { api.getEntitlements() }
            .onSuccess { current = it; lastError = null }
            .onFailure { lastError = it.message ?: "entitlements unavailable" }
    }

    /** Called on sign-out so the next account does not inherit this one's plan. */
    fun clear() { current = null }
}