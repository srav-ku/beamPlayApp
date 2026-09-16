package dev.beam.beamplay.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json

/** Keeps Ktor's request/response logs visible in Logcat without a dependency. */
private object BeamLogger : Logger {
    override fun log(message: String) {
        println("BeamHttp: $message")
    }
}

actual fun httpClient(tokenStore: BeamTokenStore, isDebug: Boolean): HttpClient =
    HttpClient(OkHttp) {
        expectSuccess = false

        engine {
            config {
                retryOnConnectionFailure(true)
                connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            }
        }


        install(ContentNegotiation) { json(BeamJson) }

        // Vidara drops a `session_token` cookie on the embed-page GET that the
        // follow-up POST to /api/stream expects. Without a cookie jar that
        // handshake silently does nothing.
        install(HttpCookies)

        install(DefaultRequest) {
            header(HttpHeaders.UserAgent, BEAM_USER_AGENT)
        }

        install(HttpTimeout) {
            connectTimeoutMillis = 15_000
            requestTimeoutMillis = 30_000
            socketTimeoutMillis = 60_000
        }

        install(Logging) {
            level = if (isDebug) LogLevel.HEADERS else LogLevel.NONE
            logger = BeamLogger
        }

        install(beamAuthPlugin(tokenStore))
    }

actual fun tmdbClient(isDebug: Boolean): HttpClient =
    HttpClient(OkHttp) {
        expectSuccess = false

        engine {
            config {
                retryOnConnectionFailure(true)
                connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            }
        }


        install(ContentNegotiation) { json(BeamJson) }
        install(HttpCookies)

        install(DefaultRequest) {
            header(HttpHeaders.UserAgent, BEAM_USER_AGENT)
        }

        install(HttpTimeout) {
            connectTimeoutMillis = 15_000
            requestTimeoutMillis = 30_000
            socketTimeoutMillis = 60_000
        }

        install(Logging) {
            level = if (isDebug) LogLevel.HEADERS else LogLevel.NONE
            logger = BeamLogger
        }
    }
