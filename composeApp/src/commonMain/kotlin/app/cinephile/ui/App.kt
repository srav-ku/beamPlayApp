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

@Composable
fun App(sessionStore: app.cinephile.data.SessionStore) {
    app.cinephile.data.SessionManager.init(sessionStore)
    var appStarted by remember { mutableStateOf(false) }

    BeamTheme {
        val session by app.cinephile.data.SessionManager.session.collectAsState()
        when {
            !appStarted -> Splash { appStarted = true }
            session == null -> AuthScreen()
            else -> AppNavHost()
        }
    }
}

@Composable
private fun Splash(onDone: () -> Unit) {
    androidx.compose.runtime.LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(650)
        onDone()
    }
    Box(
        Modifier.fillMaxSize().background(BeamColors.bg),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White),
                contentAlignment = Alignment.Center,
            ) {
                Text("â–¶", fontSize = 34.sp, color = Color.Black)
            }
            Spacer(Modifier.height(14.dp))
            Text(
                "Cinephile",
                fontSize = 28.sp,
                fontFamily = GeistMono,
                fontWeight = FontWeight.Black,
                color = Color.White,
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


