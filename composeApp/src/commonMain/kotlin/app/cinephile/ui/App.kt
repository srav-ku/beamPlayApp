package app.cinephile.ui

import androidx.compose.foundation.background
import app.cinephile.ui.settings.BeamSubtitleSettingsScreen
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.draw.alpha
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import app.cinephile.core.ui.theme.GeistMono
import app.cinephile.ui.player.BeamPlayerScreen
import app.cinephile.ui.player.PlayerSubtitle
import app.cinephile.data.Api
import app.cinephile.data.MediaItem
import app.cinephile.data.SessionManager
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.graphics.graphicsLayer

@Composable
fun App(sessionStore: app.cinephile.data.SessionStore) {
    app.cinephile.data.SessionManager.init(sessionStore)
    var appStarted by remember { mutableStateOf(false) }
    var authed by remember { mutableStateOf(false) }

    BeamTheme {
        val session by app.cinephile.data.SessionManager.session.collectAsState()
        println("[CinephileAuth] session=" + (session?.method ?: "none") + " authed=" + authed)
        when {
            !appStarted -> Splash { appStarted = true }
            session == null && !authed -> AuthScreen(onAuthed = { authed = true })
            else -> AppNavHost()
        }
    }
}

@Composable
private fun Splash(onDone: () -> Unit) {
    // Design rule taken from Netflix / Apple TV+ / Disney+ and Material's launch-screen guidance:
    // stillness reads premium, travel reads gimmick. So: long slow fades, a held mark, a glow
    // that blooms instead of moves, and an exit that fades rather than cuts. No sweep, no loop.
    var stage by remember { mutableStateOf(0) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(120); stage = 1   // field warms up
        kotlinx.coroutines.delay(400); stage = 2   // mark fades in + settles its tracking
        kotlinx.coroutines.delay(900); stage = 3   // the amber rule draws under it
        kotlinx.coroutines.delay(1400)             // stillness - the mark is simply held
        stage = 4                                  // fade out, no cut
        kotlinx.coroutines.delay(440)
        onDone()
    }

    val starsIn by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (stage >= 1) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween<Float>(1200), label = "sp-stars",
    )
    val markIn by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (stage >= 2) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween<Float>(1100), label = "sp-mark",
    )
    val glowIn by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (stage >= 2) 0.14f else 0f,
        animationSpec = androidx.compose.animation.core.tween<Float>(1500), label = "sp-glow",
    )
    // Letter spacing tightening is the whole trick: type that settles feels typeset, not animated.
    val tracking by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (stage >= 2) -0.5f else 7f,
        animationSpec = androidx.compose.animation.core.tween<Float>(1500), label = "sp-track",
    )
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (stage >= 2) 1f else 0.965f,
        animationSpec = androidx.compose.animation.core.tween<Float>(1500), label = "sp-scale",
    )
    val ruleW by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (stage >= 3) 128.dp else 0.dp,
        animationSpec = androidx.compose.animation.core.tween<androidx.compose.ui.unit.Dp>(850), label = "sp-rule",
    )
    val rootOut by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (stage >= 4) 0f else 1f,
        animationSpec = androidx.compose.animation.core.tween<Float>(440), label = "sp-out",
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF0E0E0D))
            .alpha(rootOut),
        contentAlignment = Alignment.Center,
    ) {
        BoxWithConstraints(Modifier.fillMaxSize().alpha(starsIn)) {
            val w = maxWidth
            val h = maxHeight
            SplashStars.forEach { s ->
                Box(
                    Modifier
                        .offset(x = w * s[0], y = h * s[1])
                        .size((1f + s[2] * 1.6f).dp)
                        .alpha(0.20f + s[2] * 0.45f)
                        .background(Color.White, CircleShape),
                )
            }
        }

        Box(
            Modifier
                .size(360.dp)
                .alpha(glowIn)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xE8E8A13A), Color(0x00E8A13A)),
                    ),
                    CircleShape,
                ),
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale },
        ) {
            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.alpha(markIn)) {
                Text(
                    "Cine",
                    color = Color(0xFFF5F4F1),
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 40.sp,
                    letterSpacing = tracking.sp,
                )
                Text(
                    "phile",
                    color = Color(0xFFE8A13A),
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                    fontWeight = FontWeight.SemiBold,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    fontSize = 40.sp,
                    letterSpacing = tracking.sp,
                )
            }
            Spacer(Modifier.height(18.dp))
            Box(
                Modifier
                    .width(ruleW)
                    .height(2.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFFE8A13A)),
            )
        }
    }
}

@Composable
private fun AppNavHost() {
    var backStack by remember { mutableStateOf(listOf<MediaItem>()) }
    var playback by remember { mutableStateOf<PlaybackRequest?>(null) }

    val scope = androidx.compose.runtime.rememberCoroutineScope()

    // Back button: close the player, then unwind the detail stack; only when there is
    // nothing left to close does the press fall through to the platform and exit the app.
    PlatformBackHandler(enabled = playback != null || backStack.isNotEmpty()) {
        if (playback != null) playback = null else backStack = backStack.dropLast(1)
    }

    // Premium state: fetched once per launch, cached, never blocking the UI.
    androidx.compose.runtime.LaunchedEffect(Unit) {
        app.cinephile.core.network.servicesOrNull?.beamApi?.let {
            app.cinephile.data.EntitlementsState.refresh(it)
        }
    }
    val request = playback
    val current = backStack.lastOrNull()

    when {
                request != null -> {
            androidx.compose.runtime.LaunchedEffect(request.url) { rememberContinueArt(request.resumeKey.ifBlank { request.url }, request.art) }
            BeamPlayerScreen(
            title = request.title,
            streamUrl = request.url,
            subtitles = request.subtitles,
            startPositionMs = 0L,
            resumeKey = request.resumeKey,
            onBack = { playback = null },
            onProgress = { _, _ -> },
        )
        }

        current == null -> MainScreen(
            onOpenMedia = { backStack = backStack + it },
            subtitleSettings = { BeamSubtitleSettingsScreen() },
            onResumeContinue = { ci ->
                // Re-resolve the manifest from the stable embed link, exactly like
                // the Play button does, so an expired token can never break resume.
                scope.launch {
                    val meta = runCatching {
                        app.cinephile.core.network.servicesOrNull?.vidaraApi?.getStreamMetadata(ci.sourceUrl)
                    }.getOrNull()
                    val playUrl = meta?.streaming_url?.takeIf { it.isNotBlank() } ?: ci.lastUrl
                    val subs = meta?.subtitles?.map { sub ->
                        val abs = if (sub.file_path.startsWith("http")) sub.file_path
                        else (app.cinephile.core.network.servicesOrNull?.vidaraApi?.originOf(ci.sourceUrl) ?: "") + sub.file_path
                        PlayerSubtitle(sub.language, abs)
                    } ?: emptyList()
                    if (playUrl.isNotBlank()) {
                        playback = PlaybackRequest(ci.title, playUrl, subs, ci.art, ci.sourceUrl)
                    }
                }
            },
        )

        else -> BeamDetailScreen(
            item = current,
            onBack = { backStack = backStack.dropLast(1) },
            onOpenMedia = { backStack = backStack + it },
            onPlay = { url, subs, src -> playback = PlaybackRequest(current.title, url, subs, current.backdrop_path ?: current.poster_path, src) },
        )
    }
}

private data class PlaybackRequest(
    val title: String,
    val url: String,
    val subtitles: List<PlayerSubtitle>,
    val art: String? = null,
    val resumeKey: String = "",
)



/** 44 stars at fixed pseudo-random positions, so the splash is identical every launch. */
private val SplashStars: List<FloatArray> = run {
    val out = ArrayList<FloatArray>(44)
    var seed = 7
    repeat(44) {
        val r = IntArray(3)
        for (k in 0..2) {
            seed = (seed * 1103515245 + 12345) and 0x7fffffff
            r[k] = seed % 1000
        }
        out.add(floatArrayOf(r[0] / 1000f, r[1] / 1000f, r[2] / 1000f))
    }
    out
}
