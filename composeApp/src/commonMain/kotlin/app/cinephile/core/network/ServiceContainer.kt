package app.cinephile.core.network

import io.ktor.client.HttpClient

/**
 * Poor-man's DI: a single holder for the HTTP clients and typed API facades.
 * No DI framework needed for a KMP app this size.
 */
class ServiceContainer(
    val http: HttpClient,
    val tmdbHttp: HttpClient,
    val beamApi: BeamApi,
    val tmdbApi: TmdbApi,
    val vidaraApi: VidaraApi,
    val tokenStore: BeamTokenStore,
) {
    /** Releases both connection pools. Called when the app process is torn down. */
    fun close() {
        http.close()
        tmdbHttp.close()
    }

    companion object {
        fun create(isDebug: Boolean): ServiceContainer {
            val tokenStore = InMemoryBeamTokenStore()
            val http = httpClient(tokenStore, isDebug)
            val tmdbHttp = tmdbClient(isDebug)
            return ServiceContainer(
                http = http,
                tmdbHttp = tmdbHttp,
                beamApi = BeamApi(http),
                tmdbApi = TmdbApi(tmdbHttp),
                vidaraApi = VidaraApi(http),
                tokenStore = tokenStore,
            )
        }
    }
}
