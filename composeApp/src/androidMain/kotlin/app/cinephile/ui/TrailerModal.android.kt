package app.cinephile.ui

import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Opens a URL in the phone's browser / YouTube app.
 *
 * Needed because YouTube returns **error 153** for videos whose owner disabled
 * embedding - no trick inside a WebView can play those. The modal offers this as
 * the escape hatch.
 */
actual fun openExternalUrl(url: String) {
    // `ctx` is captured by the composable below; see PlatformUrlOpenerHolder.
    PlatformUrlOpenerHolder.open(url)
}

/** Holds the current activity context so [openExternalUrl] can launch an Intent. */
object PlatformUrlOpenerHolder {
    var context: android.content.Context? = null

    fun open(url: String) {
        val ctx = context ?: return
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            ctx.startActivity(intent)
        } catch (_: Exception) {
            // No browser / YouTube app available - nothing sensible to do.
        }
    }
}

/**
 * YouTube trailer embed.
 *
 * Loaded as an HTML document with an explicit base URL of
 * `https://www.youtube.com/` instead of calling `loadUrl` directly. The base URL
 * gives the page a real origin, which is what the embed handshake needs - a bare
 * `loadUrl` sends no referrer and YouTube answers with error 153
 * ("video player configuration error").
 *
 * Also fixes the two other causes of 153: a missing `origin` query parameter and
 * a WebView user agent that YouTube treats as an unknown client.
 */
@Composable
actual fun PlatformYouTubeEmbed(videoKey: String, modifier: Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            PlatformUrlOpenerHolder.context = context.applicationContext

            val embedUrl = buildString {
                append("https://www.youtube.com/embed/")
                append(videoKey)
                append("?autoplay=1&playsinline=1&rel=0&modestbranding=1&controls=1")
                append("&origin=https%3A%2F%2Fwww.youtube.com")
            }

            val html = """
                <!DOCTYPE html>
                <html>
                  <head>
                    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                    <style>
                      html, body { margin:0; padding:0; background:#000; height:100%; overflow:hidden; }
                      iframe { position:absolute; inset:0; width:100%; height:100%; border:0; }
                    </style>
                    <meta name="referrer" content="origin">
                  </head>
                  <body>
                    <iframe
                      src="$embedUrl"
                      title="Trailer"
                      frameborder="0"
                      allow="accelerometer; autoplay; encrypted-media; gyroscope; picture-in-picture"
                      allowfullscreen></iframe>
                  </body>
                </html>
            """.trimIndent()

            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                // YouTube rejects the default "wv" agent for some embeds.
                settings.userAgentString = settings.userAgentString
                    ?.replace("; wv)", ")")
                    ?: settings.userAgentString
                setBackgroundColor(0xFF000000.toInt())
                webChromeClient = WebChromeClient()
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?,
                    ): Boolean = false
                }
                setLayerType(WebView.LAYER_TYPE_HARDWARE, null)
            }
        },
        update = { web ->
            // Only (re)load when the requested trailer actually changes - setting
            // the content on every recomposition would restart playback.
            if (web.tag != videoKey) {
                web.tag = videoKey
                val embedUrl = buildString {
                    append("https://www.youtube.com/embed/")
                    append(videoKey)
                    append("?autoplay=1&playsinline=1&rel=0&modestbranding=1&controls=1")
                    append("&origin=https%3A%2F%2Fwww.youtube.com")
                }
                val html = """
                    <!DOCTYPE html>
                    <html>
                      <head>
                        <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                        <style>
                          html, body { margin:0; padding:0; background:#000; height:100%; overflow:hidden; }
                          iframe { position:absolute; inset:0; width:100%; height:100%; border:0; }
                        </style>
                        <meta name="referrer" content="origin">
                      </head>
                      <body>
                        <iframe
                          src="$embedUrl"
                          title="Trailer"
                          frameborder="0"
                          allow="accelerometer; autoplay; encrypted-media; gyroscope; picture-in-picture"
                          allowfullscreen></iframe>
                      </body>
                    </html>
                """.trimIndent()
                web.loadDataWithBaseURL(
                    "https://www.youtube.com/",
                    html,
                    "text/html",
                    "utf-8",
                    null,
                )
            }
        },
        onRelease = { web ->
            web.stopLoading()
            web.destroy()
        },
    )
}
