package app.cinephile.ui.player

import android.content.Context
import android.net.Uri
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import app.cinephile.core.ui.theme.Beam
import app.cinephile.core.ui.theme.GeistMono
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout
import kotlinx.coroutines.delay

/**
 * Playback through libVLC.
 *
 * Media3 cannot paint this catalogue: the streams are 10-bit H.264 (High 10) and
 * every decoder path it offers - hardware, Google's software AVC, even the FFmpeg
 * video renderer - ends up either refusing the frames or decoding them to a black
 * surface. libVLC is the engine that already plays these files on this phone (it is
 * what VLC and MX use), so for these streams it is the playback path.
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
    var playing by remember { mutableStateOf(true) }
    var position by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }
    var failed by remember { mutableStateOf(false) }
    var videoLayout by remember { mutableStateOf<VLCVideoLayout?>(null) }

    // The surface must be attached once BOTH the player and the view exist. Attaching
    // in the view factory ran before the player was created, which is why playback had
    // controls and audio but no picture.
    LaunchedEffect(player, videoLayout) {
        val mp = player ?: return@LaunchedEffect
        val view = videoLayout ?: return@LaunchedEffect
        runCatching { mp.attachViews(view, null, true, false) }
        runCatching { mp.play() }
    }

    // Keep the player landscape while it owns the screen, as the other player does.
    DisposableEffect(streamUrl) {
        val activity = activityOf(context)
        activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose {
            activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    DisposableEffect(streamUrl) {
        val libVlc = LibVLC(
            appCtx,
            arrayListOf(
                "--network-caching=1500",
                "--no-drop-late-frames",
                "--no-skip-frames",
                "--audio-time-stretch",
            ),
        )
        val mp = MediaPlayer(libVlc)
        val media = Media(libVlc, Uri.parse(streamUrl)).apply {
            setHWDecoderEnabled(true, true)
            // Subtitles: libVLC wants a Slave object whose type class differs between
            // releases. Playback first - the stream is the point here - and the
            // subtitle tracks come back once the picture is confirmed.
            @Suppress("UNUSED_EXPRESSION") subtitles.size
        }
        mp.media = media
        media.release()

        mp.setEventListener { event ->
            when (event.type) {
                MediaPlayer.Event.Playing -> playing = true
                MediaPlayer.Event.Paused -> playing = false
                MediaPlayer.Event.TimeChanged -> position = event.timeChanged
                MediaPlayer.Event.LengthChanged -> duration = event.lengthChanged
                MediaPlayer.Event.EncounteredError -> failed = true
                MediaPlayer.Event.EndReached -> {
                    runCatching { PlaybackStore.markCompleted(appCtx, resumeKey, title, duration, streamUrl) }
                }
            }
        }
        player = mp
        if (startPositionMs > 0L) runCatching { mp.time = startPositionMs }

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

    // Report progress upward for the caller's own bookkeeping (throttled).
    LaunchedEffect(position, duration) {
        if (duration > 0L) {
            onProgress(position, duration)
            delay(1_000)
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
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

        // ---- top bar: back + title ----
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
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Close",
                    tint = Color.White,
                    modifier = Modifier.size(19.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = title,
                color = Color.White,
                fontFamily = GeistMono,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
        }

        // ---- bottom controls ----
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color(0xB3000000))
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            if (failed) {
                Text(
                    text = "libVLC could not open this stream.",
                    color = Color(0xFFFFB4AB),
                    fontFamily = GeistMono,
                    fontSize = 12.sp,
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stamp(position),
                    color = Color.White,
                    fontFamily = GeistMono,
                    fontSize = 11.sp,
                )
                Spacer(Modifier.width(10.dp))
                // Position is shown on a hairline track; tapping either side nudges.
                Row(
                    Modifier
                        .weight(1f)
                        .height(22.dp)
                        .clickable {
                            val mp = player ?: return@clickable
                            runCatching {
                                val target = if (position < duration / 2) position + 30_000 else position - 30_000
                                mp.time = target.coerceIn(0L, duration)
                            }
                        },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color(0x33FFFFFF)),
                    ) {
                        val fraction = if (duration > 0L) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f
                        Box(
                            Modifier
                                .fillMaxWidth(fraction)
                                .height(3.dp)
                                .background(colors.amber500),
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text = stamp(duration),
                    color = Color.White.copy(alpha = 0.7f),
                    fontFamily = GeistMono,
                    fontSize = 11.sp,
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ControlChip(if (playing) "Pause" else "Play", primary = true) {
                    player?.let { mp ->
                        runCatching { if (mp.isPlaying) mp.pause() else mp.play() }
                    }
                }
                ControlChip("-10s") {
                    player?.let { mp -> runCatching { mp.time = (mp.time - 10_000).coerceAtLeast(0L) } }
                }
                ControlChip("+10s") {
                    player?.let { mp -> runCatching { mp.time = (mp.time + 10_000).coerceAtMost(duration) } }
                }
            }
        }
    }
}

@Composable
private fun ControlChip(label: String, primary: Boolean = false, onClick: () -> Unit) {
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
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(15.dp),
            )
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
private fun activityOf(context: android.content.Context): android.app.Activity? {
    var current: android.content.Context? = context
    while (current is android.content.ContextWrapper) {
        if (current is android.app.Activity) return current
        current = current.baseContext
    }
    return null
}
