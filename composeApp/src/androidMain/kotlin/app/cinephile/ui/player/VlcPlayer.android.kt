package app.cinephile.ui.player

import android.content.Context
import android.content.pm.ActivityInfo
import android.net.Uri
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import app.cinephile.core.ui.theme.Beam
import app.cinephile.core.ui.theme.GeistMono
import kotlinx.coroutines.delay
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout

private val VLC_SPEEDS = listOf(0.25f, 0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f, 2.5f, 3f)
private const val AUTO_HIDE_MS = 5000L

/**
 * Playback through libVLC, with the full control set.
 *
 * Media3 cannot render this catalogue - the streams are 10-bit H.264 (High 10) and
 * every decoder path it offers ends at a black surface. libVLC is the engine that
 * already plays these files on this phone, so it is the playback path, and it
 * brings back everything the other player had: speed, audio and subtitle tracks,
 * aspect modes, brightness and volume drags, tap-to-hide controls and a draggable
 * timeline.
 */
@Composable
fun VlcPlaybackScreen(
    title: String,
    streamUrl: String,
    subtitles: List<PlayerSubtitle>,
    startPositionMs: Long,
    resumeKey: String,
    onBack: () -> Unit,
    onProgress: (Long, Long) -> Unit = { _, _ -> },
) {
    val colors = Beam.colors
    val context = LocalContext.current
    val appCtx = context.applicationContext

    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var videoLayout by remember { mutableStateOf<VLCVideoLayout?>(null) }
    var playing by remember { mutableStateOf(true) }
    var position by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }
    var failed by remember { mutableStateOf(false) }
    var controlsVisible by remember { mutableStateOf(true) }
    var seeking by remember { mutableStateOf(false) }
    var seekTarget by remember { mutableFloatStateOf(0f) }
    var speed by remember { mutableStateOf(1f) }
    var volume by remember { mutableFloatStateOf(1f) }
    var brightness by remember { mutableFloatStateOf(1f) }
    var aspect by remember { mutableStateOf(0) }
    var audioTracks by remember { mutableStateOf<List<Pair<Int, String>>>(emptyList()) }
    var subTracks by remember { mutableStateOf<List<Pair<Int, String>>>(emptyList()) }
    var menu by remember { mutableStateOf<String?>(null) }

    val aspects = remember {
        listOf(
            "Fit" to MediaPlayer.ScaleType.SURFACE_BEST_FIT,
            "Fill" to MediaPlayer.ScaleType.SURFACE_FILL,
            "16:9" to MediaPlayer.ScaleType.SURFACE_16_9,
            "4:3" to MediaPlayer.ScaleType.SURFACE_4_3,
            "Crop" to MediaPlayer.ScaleType.SURFACE_FIT_SCREEN,
        )
    }

    DisposableEffect(streamUrl) {
        val libVlc = LibVLC(
            appCtx,
            arrayListOf(
                "--network-caching=1200",
                "--no-drop-late-frames",
                "--no-skip-frames",
                "--audio-time-stretch",
            ),
        )
        val mp = MediaPlayer(libVlc)
        val media = Media(libVlc, Uri.parse(streamUrl)).apply {
            setHWDecoderEnabled(true, true)
            addOption(":http-referrer=https://vidaraa.cc/")
            addOption(":http-user-agent=Mozilla/5.0 (Linux; Android 14)")
        }
        mp.media = media
        media.release()

        mp.setEventListener { event ->
            when (event.type) {
                MediaPlayer.Event.Playing -> {
                    playing = true
                    runCatching { audioTracks = mp.audioTracks.orEmpty().map { it.id to it.name } }
                    runCatching { subTracks = mp.spuTracks.orEmpty().map { it.id to it.name } }
                }
                MediaPlayer.Event.Paused -> playing = false
                MediaPlayer.Event.TimeChanged -> if (!seeking) position = event.timeChanged
                MediaPlayer.Event.LengthChanged -> duration = event.lengthChanged
                MediaPlayer.Event.EncounteredError -> failed = true
                MediaPlayer.Event.EndReached ->
                    runCatching { PlaybackStore.markCompleted(appCtx, resumeKey, title, duration, streamUrl) }
            }
        }
        player = mp
        if (startPositionMs > 0L) runCatching { mp.time = startPositionMs }
        runCatching { volume = (mp.volume / 100f).coerceIn(0f, 1f) }

        onDispose {
            runCatching {
                val pos = mp.time
                val len = mp.length
                if (PlaybackStore.isFinished(pos, len)) {
                    PlaybackStore.markCompleted(appCtx, resumeKey, title, len, streamUrl)
                } else if (pos >= 3_000L) {
                    PlaybackStore.saveProgress(appCtx, resumeKey, title, pos, len, streamUrl)
                }
            }
            runCatching { mp.setEventListener(null) }
            runCatching { mp.stop() }
            runCatching { mp.detachViews() }
            runCatching { mp.release() }
            runCatching { libVlc.release() }
            player = null
        }
    }

    // The surface must be attached once BOTH the player and the view exist.
    LaunchedEffect(player, videoLayout) {
        val mp = player ?: return@LaunchedEffect
        val view = videoLayout ?: return@LaunchedEffect
        runCatching { mp.attachViews(view, null, true, false) }
        runCatching { mp.play() }
    }

    // Landscape while the player owns the screen, portrait again afterwards.
    DisposableEffect(streamUrl) {
        val activity = activityOf(context)
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose { activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT }
    }

    // Tap toggles the chrome; it hides itself while playing.
    LaunchedEffect(controlsVisible, playing, menu) {
        if (controlsVisible && playing && menu == null) {
            delay(AUTO_HIDE_MS)
            controlsVisible = false
        }
    }

    // Report progress upward for the caller's bookkeeping.
    LaunchedEffect(position, duration) {
        if (duration > 0L) {
            onProgress(position, duration)
            delay(1_000)
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { controlsVisible = !controlsVisible })
                },
            factory = { ctx ->
                VLCVideoLayout(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    videoLayout = this
                }
            },
            update = { v -> videoLayout = v },
        )

        // Brightness dim (left half drag), volume (right half drag) - as before.
        Row(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .pointerInput(Unit) {
                        detectVerticalDragGestures { _, drag ->
                            brightness = (brightness - drag / size.height.toFloat()).coerceIn(0.15f, 1f)
                        }
                    },
            )
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .pointerInput(Unit) {
                        detectVerticalDragGestures { _, drag ->
                            volume = (volume - drag / size.height.toFloat()).coerceIn(0f, 1f)
                            runCatching { player?.setVolume((volume * 100).toInt()) }
                        }
                    },
            )
        }
        Box(Modifier.fillMaxSize().alpha(1f - brightness).background(Color.Black))

        if (controlsVisible) {
            Column(Modifier.fillMaxSize()) {
                // ---- top: back, title, speed, aspect ----
                Row(
                    Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0x99000000))
                            .border(1.dp, Color(0x1FFFFFFF), CircleShape)
                            .clickable { onBack() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Close, "Close", tint = Color.White, modifier = Modifier.size(19.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = title,
                        color = Color.White,
                        fontFamily = GeistMono,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Chip(speedLabel(speed)) {
                        val next = VLC_SPEEDS[(VLC_SPEEDS.indexOf(speed).takeIf { it >= 0 } ?: 3).let { (it + 1) % VLC_SPEEDS.size }]
                        speed = next
                        runCatching { player?.rate = next }
                    }
                    Spacer(Modifier.width(8.dp))
                    Chip(aspects[aspect].first) {
                        aspect = (aspect + 1) % aspects.size
                        runCatching { player?.setVideoScale(aspects[aspect].second) }
                    }
                }

                Spacer(Modifier.weight(1f))

                if (failed) {
                    Text(
                        text = "libVLC could not open this stream.",
                        color = Color(0xFFFFB4AB),
                        fontFamily = GeistMono,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 18.dp),
                    )
                }

                // ---- bottom: timeline + transport ----
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(Color(0xB3000000))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stamp(if (seeking) seekTarget.toLong() else position), color = Color.White, fontFamily = GeistMono, fontSize = 11.sp)
                        Slider(
                            value = if (duration > 0L) (if (seeking) seekTarget.toLong() else position).toFloat() / duration else 0f,
                            onValueChange = { v ->
                                seeking = true
                                seekTarget = (v * duration)
                            },
                            onValueChangeFinished = {
                                runCatching { player?.time = seekTarget.toLong() }
                                position = seekTarget.toLong()
                                seeking = false
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = colors.amber500,
                                activeTrackColor = colors.amber500,
                                inactiveTrackColor = Color(0x33FFFFFF),
                            ),
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                        )
                        Text(stamp(duration), color = Color.White.copy(alpha = 0.7f), fontFamily = GeistMono, fontSize = 11.sp)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconChip(if (playing) "❚❚" else "▶", primary = true) {
                            player?.let { mp -> runCatching { if (mp.isPlaying) mp.pause() else mp.play() } }
                        }
                        IconChip("-10s") { player?.let { mp -> runCatching { mp.time = (mp.time - 10_000).coerceAtLeast(0L) } } }
                        IconChip("+10s") { player?.let { mp -> runCatching { mp.time = (mp.time + 10_000).coerceAtMost(duration) } } }
                        if (audioTracks.size > 1) Chip("Audio") { menu = "audio" }
                        if (subTracks.size > 1) Chip("Subs") { menu = "subs" }
                    }
                }
            }
        }

        // ---- menus ----
        val open = menu
        if (open != null) {
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(14.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xE6000000))
                    .padding(vertical = 8.dp),
            ) {
                val list = if (open == "audio") audioTracks else subTracks
                LazyColumn {
                    items(list, key = { "$open-${it.first}" }) { entry ->
                        Text(
                            text = entry.second,
                            color = Color.White,
                            fontFamily = GeistMono,
                            fontSize = 13.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    runCatching {
                                        if (open == "audio") player?.setAudioTrack(entry.first)
                                        else player?.setSpuTrack(entry.first)
                                    }
                                    menu = null
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Chip(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        color = Color.White,
        fontFamily = GeistMono,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color(0x4D000000))
            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(50))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 7.dp),
    )
}

@Composable
private fun IconChip(label: String, primary: Boolean = false, onClick: () -> Unit) {
    val colors = Beam.colors
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (primary) colors.amber500 else Color(0x33FFFFFF))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (primary) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = label,
            color = if (primary) Color.Black else Color.White,
            fontFamily = GeistMono,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/** "1x", "1.25x" - the same steps the other player offered. */
private fun speedLabel(speed: Float): String =
    if (speed == speed.toInt().toFloat()) speed.toInt().toString() + "x" else speed.toString() + "x"

/** mm:ss (or h:mm:ss) for the transport readout. */
private fun stamp(ms: Long): String {
    if (ms <= 0L) return "0:00"
    val total = ms / 1000
    val hours = total / 3600
    val minutes = (total % 3600) / 60
    val seconds = total % 60
    return if (hours > 0) {
        hours.toString() + ":" + minutes.toString().padStart(2, '0') + ":" + seconds.toString().padStart(2, '0')
    } else {
        minutes.toString() + ":" + seconds.toString().padStart(2, '0')
    }
}

/** The hosting activity, wherever it sits in the context chain. */
private fun activityOf(context: Context): android.app.Activity? {
    var current: Context? = context
    while (current is android.content.ContextWrapper) {
        if (current is android.app.Activity) return current
        current = current.baseContext
    }
    return null
}
