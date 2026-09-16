package dev.beam.beamplay.core.network

import dev.beam.beamplay.core.model.VidaraStreamMetadata
import dev.beam.beamplay.core.model.VidaraSubtitle
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Turns a Vidara embed URL into a playable `.m3u8` manifest.
 *
 * ### Why this runs on the device
 * The `.m3u8` URL is IP-locked to whoever called `/api/stream`. On the web the
 * request is proxied through the same edge node that later serves the video, so
 * the IPs match. On Android there is no CORS at all, so the phone can call
 * Vidara **directly** and ExoPlayer then plays a manifest that is locked to the
 * phone's own IP — exactly the behaviour we want.
 *
 * ### Host handling
 * The site's current links point at `vidaraa.cc` while older docs/links use
 * `vidara.to`; both are the same service. Rather than hardcoding either host we
 * derive the API origin from the embed URL itself, so both work.
 *
 * No `User-Agent` is set explicitly — the shared client installs a mobile
 * Chrome UA via `DefaultRequest`, matching the flow in `src/lib/vidara.ts`.
 */
class VidaraApi(private val client: HttpClient) {

    private val embedHostRegex = Regex("""^(https?://[^/]+)""")
    private val versionedPathRegex = Regex("""^(https?://[^/]+)/v/(.+)$""")
    private val filecodeRegex = Regex("""/e/([a-zA-Z0-9]+)""")

    private val fallbackOrigin = "https://vidara.to"

    /** `https://<host>/v/<code>` → `https://<host>/e/<code>`. */
    fun normalizeUrl(url: String): String {
        val trimmed = url.trim()
        val match = versionedPathRegex.find(trimmed) ?: return trimmed
        return "${match.groupValues[1]}/e/${match.groupValues[2]}"
    }

    /** Extracts `<code>` from a `/e/<code>` embed URL. */
    fun extractFilecode(url: String): String? =
        filecodeRegex.find(normalizeUrl(url))?.groupValues?.getOrNull(1)

    /** `https://vidaraa.cc` from `https://vidaraa.cc/e/abc`. */
    fun originOf(url: String): String =
        embedHostRegex.find(normalizeUrl(url))?.groupValues?.getOrNull(1) ?: fallbackOrigin

    /**
     * Replicates `src/lib/vidara.ts` → `getLiveVideoManifest()`:
     * 1. GET the embed page (drops the `session_token` cookie into our jar).
     * 2. POST `/api/stream` with `{ filecode, device: "web" }` + same-origin
     *    headers, yielding the IP-locked playlist URL.
     */
    suspend fun getStreamMetadata(embedUrl: String): VidaraStreamMetadata {
        val normalized = normalizeUrl(embedUrl)
        val filecode = extractFilecode(normalized)
            ?: error("Invalid URL. Expected format: https://vidara.to/e/<filecode>")
        val origin = originOf(normalized)

        // Step 1 — prime the session cookie. Best-effort: the POST below can
        // still succeed on hosts that don't require it.
        runCatching {
            client.get(normalized) {
                headers {
                    append(HttpHeaders.Accept, "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    append(HttpHeaders.AcceptLanguage, "en-US,en;q=0.9")
                }
            }
        }

        // Step 2 — the actual extraction.
        val response: HttpResponse = client.post("$origin/api/stream") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.AcceptLanguage, "en-US,en;q=0.9")
                append(HttpHeaders.ContentType, "application/json")
                append(HttpHeaders.Origin, origin)
                append(HttpHeaders.Referrer, normalized)
                append("Sec-Fetch-Mode", "cors")
                append("Sec-Fetch-Site", "same-origin")
            }
            setBody(buildJsonObject {
                put("filecode", filecode)
                put("device", "web")
            }.toString())
        }

        if (response.status.value !in 200..299) {
            error("Vidara /api/stream returned ${response.status}")
        }

        val bodyText = response.bodyAsText()
        val parsed = BeamJson.decodeFromString<VidaraStreamMetadata>(bodyText)
        if (parsed.streaming_url.isBlank()) {
            error("Vidara returned no streaming_url. Body: ${bodyText.take(300)}")
        }
        return parsed
    }

    /** Pick the best English subtitle, else the first one. */
    fun pickDefaultEnglishSub(subs: List<VidaraSubtitle>): VidaraSubtitle? {
        if (subs.isEmpty()) return null
        return subs.firstOrNull { it.language == "English" }
            ?: subs.firstOrNull { it.language == "English (SDH)" }
            ?: subs.firstOrNull { it.language.contains("English") }
            ?: subs.first()
    }
}
