package app.cinephile.ui

import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * YouTube embed for the trailer overlay.
 *
 * A plain WebView (no YouTube SDK) keeps the app dependency-free; autoplay is
 * allowed because the user explicitly tapped "Trailer", and `playsinline`
 * keeps playback inside the card instead of handing off to the YouTube app.
 */
@Composable
actual fun PlatformYouTubeEmbed(videoKey: String, modifier: Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
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
                webChromeClient = WebChromeClient()
                setBackgroundColor(0xFF000000.toInt())
            }
        },
        update = { web ->
            // Only reload when the requested video actually changes - setting the
            // URL on every recomposition would restart playback mid-trailer.
            if (web.tag != videoKey) {
                web.tag = videoKey
                web.loadUrl(
                    "https://www.youtube.com/embed/" + videoKey +
                        "?autoplay=1&rel=0&modestbranding=1&controls=1&playsinline=1",
                )
            }
        },
        onRelease = { web ->
            web.stopLoading()
            web.destroy()
        },
    )
}
