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
    // Five beats, ~2.2s. One signature gesture (the amber sweep), no infinite loops,
    // nothing that repeats - which is what keeps it premium instead of busy.
    var stage by remember { mutableStateOf(0) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(140); stage = 1  // stars breathe in
        kotlinx.coroutines.delay(220); stage = 2  // "Cine" arrives
        kotlinx.coroutines.delay(520); stage = 3  // "phile" follows
        kotlinx.coroutines.delay(140); stage = 4  // rule draws + light sweeps
        kotlinx.coroutines.delay(560); stage = 5  // settles
        kotlinx.coroutines.delay(700)
        onDone()
    }

    val starsIn by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (stage >= 1) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween<Float>(750), label = "sp-stars",
    )
    val cineIn by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (stage >= 2) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween<Float>(620), label = "sp-cine",
    )
    val phileIn by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (stage >= 3) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween<Float>(620), label = "sp-phile",
    )
    val rise by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (stage >= 2) 0f else 26f,
        animationSpec = androidx.compose.animation.core.tween<Float>(700), label = "sp-rise",
    )
    val ruleW by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (stage >= 4) 132.dp else 0.dp,
        animationSpec = androidx.compose.animation.core.tween<androidx.compose.ui.unit.Dp>(650), label = "sp-rule",
    )
    val sweep by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (stage >= 4) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween<Float>(950), label = "sp-sweep",
    )
    val settle by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (stage >= 5) 1f else 0.97f,
        animationSpec = androidx.compose.animation.core.tween<Float>(650), label = "sp-settle",
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF0E0E0D)),
        contentAlignment = Alignment.Center,
    ) {
        // Stars arrive first: the room lights up before the brand does.
        BoxWithConstraints(Modifier.fillMaxSize().alpha(starsIn)) {
            val w = maxWidth
            val h = maxHeight
            SplashStars.forEach { s ->
                Box(
                    Modifier
                        .offset(x = w * s[0], y = h * s[1])
                        .size((1f + s[2] * 1.7f).dp)
                        .alpha(0.22f + s[2] * 0.5f)
                        .background(Color.White, CircleShape),
                )
            }
        }

        // The light sweep: one translucent amber bar travelling across the wordmark.
        Box(
            Modifier
                .size(width = 96.dp, height = 64.dp)
                .offset(x = (-170f + sweep * 340f).dp)
                .alpha((1f - sweep) * 0.55f)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, Color(0x66E8A13A), Color.Transparent),
                    ),
                ),
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .offset(y = rise.dp)
                .graphicsLayer { scaleX = settle; scaleY = settle },
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    "Cine",
                    color = Color(0xFFF5F4F1),
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 38.sp,
                    letterSpacing = (-1).sp,
                    modifier = Modifier.alpha(cineIn),
                )
                Text(
                    "phile",
                    color = Color(0xFFE8A13A),
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                    fontWeight = FontWeight.SemiBold,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    fontSize = 38.sp,
                    letterSpacing = (-1).sp,
                    modifier = Modifier.alpha(phileIn),
                )
            }
            Spacer(Modifier.height(14.dp))
            Box(
                Modifier
                    .width(ruleW)
                    .height(2.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFFE8A13A)),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Discover, Access, Track.",
                color = Color(0xFF8E8A82),
                fontSize = 11.5.sp,
                letterSpacing = 0.8.sp,
                modifier = Modifier.alpha(starsIn),
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
