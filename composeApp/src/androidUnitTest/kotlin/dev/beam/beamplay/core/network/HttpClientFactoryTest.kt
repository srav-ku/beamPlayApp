package dev.beam.beamplay.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Verifies the Ktor client wiring: the JWT auth plugin (including the host
 * allow-list), and that the real worker is reachable and gates authed routes.
 */
class HttpClientFactoryTest {

    private val beamUrl = "https://beamplay.beam-api.workers.dev/user/playback"
    private val thirdPartyUrl = "https://vidaraa.cc/e/abc123"

    /** MockEngine client with the auth plugin installed, recording what it saw. */
    private fun mockClient(store: BeamTokenStore, seen: MutableList<String?>): HttpClient {
        val engine = MockEngine { request ->
            seen += request.headers[HttpHeaders.Authorization]
            respond(
                content = """{"ok":true}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        return HttpClient(engine) { install(beamAuthPlugin(store)) }
    }

    @Test
    fun attachesBearerTokenForBeamHost() = runTest {
        val seen = mutableListOf<String?>()
        val store = InMemoryBeamTokenStore()
        val client = mockClient(store, seen)

        store.setToken("TEST_JWT")
        client.get(beamUrl)

        assertEquals("Bearer TEST_JWT", seen.last())
        client.close()
    }

    @Test
    fun omitsHeaderWhenNoToken() = runTest {
        val seen = mutableListOf<String?>()
        val client = mockClient(InMemoryBeamTokenStore(), seen)

        client.get(beamUrl)

        assertNull(seen.last())
        client.close()
    }

    @Test
    fun neverLeaksTokenToThirdPartyHosts() = runTest {
        val seen = mutableListOf<String?>()
        val client = mockClient(InMemoryBeamTokenStore("TEST_JWT"), seen)

        client.get(thirdPartyUrl)

        assertNull(seen.last(), "Beam JWT was sent to a non-Beam host")
        client.close()
    }

    @Test
    fun explicitHeaderIsNotOverwritten() = runTest {
        val seen = mutableListOf<String?>()
        val client = mockClient(InMemoryBeamTokenStore("STORE_JWT"), seen)

        client.get(beamUrl) {
            header(HttpHeaders.Authorization, "Bearer EXPLICIT")
        }

        assertEquals("Bearer EXPLICIT", seen.last())
        client.close()
    }

    @Test
    fun liveWorkerHealthEndpointRespondsOk() = runTest {
        val client = httpClient(InMemoryBeamTokenStore(), isDebug = true)
        val body = client.get("$BEAM_BASE_URL/").bodyAsText()

        assertTrue(body.contains("\"ok\":true"), "Unexpected health body: $body")
        client.close()
    }

    @Test
    fun authedRouteIsRejectedWithoutToken() = runTest {
        val client = httpClient(InMemoryBeamTokenStore(), isDebug = false)
        val response = client.get("$BEAM_BASE_URL/user/settings")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        client.close()
    }
}
