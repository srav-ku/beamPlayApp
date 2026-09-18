package app.cinephile.core.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

/** Base URL of the BeamPlay Cloudflare Worker backend. */
const val BEAM_BASE_URL: String = "https://beamplay.beam-api.workers.dev"

/** TMDB v3 REST base. Called directly from the device; the key is public. */
const val TMDB_BASE_URL: String = "https://api.themoviedb.org/3"

/** Public TMDB key that already ships inside the web bundle. */
const val TMDB_API_KEY: String = "5701acdc0ce0fd37222a3c46fa2ac9aa"

/** TMDB image CDN base: `TMDB_IMAGE_BASE + "/w500" + poster_path`. */
const val TMDB_IMAGE_BASE: String = "https://image.tmdb.org/t/p"

/**
 * Browser-ish User-Agent. The Beam worker ignores it, but Vidara's bot wall
 * rejects requests that look like they came from an unknown HTTP client.
 */
const val BEAM_USER_AGENT: String =
    "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

/** Host that is allowed to receive the Beam JWT. */
private const val BEAM_HOST_SUFFIX = "beam-api.workers.dev"

/**
 * Single shared JSON config. `ignoreUnknownKeys` keeps the app alive when the
 * worker grows new columns; `explicitNulls = false` means PATCH-style bodies
 * (e.g. `/user/settings`) only send the fields that are actually set.
 */
@OptIn(ExperimentalSerializationApi::class)
val BeamJson: Json = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    explicitNulls = false
    prettyPrint = false
    isLenient = true
}

/**
 * Storage abstraction for the Beam JWT. Phase 1 ships [InMemoryBeamTokenStore];
 * Phase 8 swaps in a DataStore-backed implementation without touching callers.
 */
interface BeamTokenStore {
    suspend fun setToken(jwt: String?)
    suspend fun getToken(): String?
}

/** Phase-1 token store. Not persisted across process death. */
class InMemoryBeamTokenStore(initial: String? = null) : BeamTokenStore {
    private var token: String? = initial

    override suspend fun setToken(jwt: String?) {
        token = jwt
    }

    override suspend fun getToken(): String? = token
}

/**
 * Adds `Authorization: Bearer <jwt>` to requests aimed at the Beam worker.
 *
 * Two guards matter here:
 *  - **Host allow-list.** The same [HttpClient] is also used for Vidara, so
 *    without this check the Beam JWT would be sent to a third-party host.
 *  - **Explicit header wins.** Callers can still override per request.
 *
 * Implemented with a `createClientPlugin` hook (not `DefaultRequest`) because
 * the store lookup is `suspend` — this is the stable Ktor 3 API for suspend
 * header injection, and it is engine-agnostic, so it can be unit-tested
 * against `MockEngine`.
 */
fun beamAuthPlugin(tokenStore: BeamTokenStore) = createClientPlugin("BeamAuth") {
    onRequest { request, _ ->
        val jwt = tokenStore.getToken()
        val isBeamHost = request.url.host.endsWith(BEAM_HOST_SUFFIX)
        if (!jwt.isNullOrBlank() && isBeamHost && request.headers[HttpHeaders.Authorization] == null) {
            request.header(HttpHeaders.Authorization, "Bearer $jwt")
        }
    }
}

/**
 * Beam worker client: JSON content negotiation, cookie jar (needed for the
 * Vidara embed-page → session-cookie handshake), timeouts, header logging and
 * the JWT auth plugin.
 */
expect fun httpClient(tokenStore: BeamTokenStore, isDebug: Boolean): HttpClient

/**
 * TMDB client. Separate instance on purpose: different host, no auth header
 * (the `api_key` is passed as a query parameter instead).
 */
expect fun tmdbClient(isDebug: Boolean): HttpClient
