package app.cinephile.ui.player

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.net.Uri
import android.util.Rational
import androidx.compose.animation.core.animateFloatAsState
import app.cinephile.core.ui.theme.GeistMono
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.runtime.DisposableEffect
import kotlin.math.roundToInt
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay

private val SPEED_STEPS = listOf(0.25f, 0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f, 2.5f, 3f)
private const val AUTO_HIDE_MS = 5000L

// Forced ratios need a real aspect box, not an ExoPlayer resize mode.
private const val ASPECT_16_9 = -2
private const val ASPECT_4_3 = -3

/** Text shadow used by every overlay label (black .7, blur 4, offset 0/1). */
private val OverlayShadow = Shadow(
    color = Color(0xB3000000),
    offset = Offset(2f, 2f),
    blurRadius = 4f,
)

@UnstableApi
@Composable
actual fun BeamPlayerScreen(
    title: String,
    streamUrl: String,
    subtitles: List<PlayerSubtitle>,
    startPositionMs: Long,
    resumeKey: String,
    onBack: () -> Unit,
    onProgress: (Long, Long) -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val activity = remember(context) { context.findActivity() }

    var player by remember { mutableStateOf<ExoPlayer?>(null) }
    var controlsVisible by remember { mutableStateOf(true) }
    var locked by remember { mutableStateOf(false) }

    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var bufferedMs by remember { mutableLongStateOf(0L) }
    var isPlaying by remember { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(true) }
    var playbackError by remember { mutableStateOf<String?>(null) }
    // Set when the hardware decoder fails mid-decode; the player is then rebuilt once
    // on Android software decoders, which is what actually plays those streams.
    var softwareOnly by remember { mutableStateOf(false) }

    var speed by remember { mutableFloatStateOf(1f) }
    var boost by remember { mutableStateOf(false) }
    var muted by remember { mutableStateOf(false) }
    var resizeMode by remember { mutableStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }

    var audioTracks by remember { mutableStateOf<List<PlayerTrack>>(emptyList()) }
    var textTracks by remember { mutableStateOf<List<PlayerTrack>>(emptyList()) }
    var audioApplied by remember { mutableStateOf(false) }

    var sheet by remember { mutableStateOf<SheetKind?>(null) }
    var tab by remember { mutableStateOf(0) }
    var captionScale by remember { mutableFloatStateOf(1f) }
    var captionBg by remember { mutableStateOf(true) }
    val appCtx = androidx.compose.ui.platform.LocalContext.current.applicationContext
    val videoKey = title + "|" + streamUrl
    // Resume records are keyed on the stable source link when we have one.
    val storeKey = if (resumeKey.isBlank()) streamUrl else resumeKey
    // Hold-to-boost speed: one value for the whole app, remembered.
    var boostSpeed by remember { mutableFloatStateOf(SubtitlePrefs.boostSpeed(appCtx)) }
    // Drag maths: the boosted value is derived from TOTAL finger travel from the
    // press point, never from the previous value. That removes drift, stops it
    // running away at 0.1-per-jitter, and keeps it reversible.
    var boostBase by remember { mutableFloatStateOf(2f) }
    var boostTravel by remember { mutableFloatStateOf(0f) }

    // The audio language chosen for THIS stream, re-applied the moment the
    // tracks appear, so reopening a film keeps the language you picked.
    fun applySavedAudio(exo: ExoPlayer, tracks: List<PlayerTrack>) {
        if (audioApplied || tracks.isEmpty()) return
        val saved = SubtitlePrefs.loadAudio(appCtx, storeKey) ?: return
        val match = tracks.firstOrNull { it.id == saved } ?: return
        audioApplied = true
        selectTrack(exo, C.TRACK_TYPE_AUDIO, match.id)
    }
    var subtitleStyle by remember {
        mutableStateOf(SubtitlePrefs.load(appCtx).copy(syncMs = SubtitlePrefs.loadSync(appCtx, videoKey)))
    }
    LaunchedEffect(subtitleStyle) {
        SubtitlePrefs.save(appCtx, subtitleStyle)
        SubtitlePrefs.saveSync(appCtx, videoKey, subtitleStyle.syncMs)
    }
    var playerView by remember { mutableStateOf<PlayerView?>(null) }
    var currentCues by remember { mutableStateOf<List<androidx.media3.common.text.Cue>>(emptyList()) }
    var cueSets by remember { mutableStateOf<List<List<VttCue>>>(emptyList()) }
    var subSetIndex by remember { mutableStateOf<Int?>(null) }
    var tickPos by remember { mutableStateOf(0L) }
    var cueBuffer by remember { mutableStateOf<List<Triple<String, Long, Long>>>(emptyList()) }
    var lastCueText by remember { mutableStateOf<String?>(null) }
    var lastCueStart by remember { mutableStateOf(0L) }
    var vttOffset by remember { mutableStateOf(0L) }

    var zoom by remember { mutableFloatStateOf(1f) }
    // Brightness / volume readout shown while dragging.
    var statusPill by remember { mutableStateOf<String?>(null) }
    var dragSide by remember { mutableIntStateOf(0) }
    var brightStart by remember { mutableFloatStateOf(0.5f) }
    var brightTravel by remember { mutableFloatStateOf(0f) }
    var volStart by remember { mutableIntStateOf(0) }
    var volTravel by remember { mutableFloatStateOf(0f) }
    var scrubbing by remember { mutableStateOf(false) }
    var showRemaining by remember { mutableStateOf(PlayerPrefs.showRemaining(appCtx)) }
    var scrubPosition by remember { mutableLongStateOf(0L) }


    // ---- Immersive: hide system bars so nothing collides with the phone UI ----
    DisposableEffect(Unit) {
        val window = activity?.window
        val controller = window?.let { WindowInsetsControllerCompat(it, view) }
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        PlayerSession.isOpen = true
        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
            window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            PlayerSession.isOpen = false
            player?.release()
            player = null
        }
    }

    DisposableEffect(streamUrl, softwareOnly) {
         val loadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
             .setBufferDurationsMs(30_000, 300_000, 1_500, 3_000)
             .build()
         // Some sources publish 4K and 10-bit HEVC renditions that a mid-range device
         // cannot decode. Letting the renderers fall back to software decoders and
         // capping the adaptive selection to 1080p means the player picks a rendition
         // it can actually play, instead of dying with a decoding error.
         val renderers = androidx.media3.exoplayer.DefaultRenderersFactory(context)
             .setEnableDecoderFallback(true)
             .setMediaCodecSelector(
                 if (softwareOnly) SoftwareOnlyCodecs
                 else androidx.media3.exoplayer.mediacodec.MediaCodecSelector.DEFAULT,
             )
         val exo = ExoPlayer.Builder(context, renderers)
             .setLoadControl(loadControl)
             .build()
         exo.trackSelectionParameters = androidx.media3.common.TrackSelectionParameters.Builder()
             .setMaxVideoSize(1920, 1080)
             .setMaxVideoBitrate(12_000_000)
             .build()
        val builder = MediaItem.Builder().setUri(streamUrl)
        val subs = subtitles.map { sub ->
            MediaItem.SubtitleConfiguration.Builder(Uri.parse(sub.url))
                .setMimeType(MimeTypes.TEXT_VTT)
                .setLabel(sub.label)
                .setLanguage(sub.label.lowercase().take(3))
                .setSelectionFlags(if (sub == subtitles.firstOrNull()) C.SELECTION_FLAG_DEFAULT else 0)
                .build()
        }
        if (subs.isNotEmpty()) builder.setSubtitleConfigurations(subs)

        exo.setMediaItem(builder.build())
        // Speed is remembered per video: movie A can sit at 2x while movie B stays 1x.
        SubtitlePrefs.loadSpeed(appCtx, storeKey)?.let { saved -> speed = saved }
        SubtitlePrefs.loadAspect(appCtx, storeKey)?.let { saved -> resizeMode = saved }
        exo.setPlaybackSpeed(speed)
        exo.prepare()
        exo.playWhenReady = true
         val resumeAt = if (startPositionMs > 0L) startPositionMs
             else if (PlaybackStore.isCompleted(appCtx, storeKey)) 0L
             else PlaybackStore.resumeMs(appCtx, storeKey)
         if (resumeAt > 0L) exo.seekTo(resumeAt)
        player = exo

        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
            override fun onPlaybackStateChanged(state: Int) {
                isBuffering = state == Player.STATE_BUFFERING
                if (state == Player.STATE_READY) durationMs = exo.duration.coerceAtLeast(0)
                refreshTracks(exo) { a, t ->
                    audioTracks = a
                    textTracks = t
                    applySavedAudio(exo, a)
                }
            }
            override fun onTracksChanged(tracks: Tracks) {
                refreshTracks(exo) { a, t ->
                    audioTracks = a
                    textTracks = t
                    applySavedAudio(exo, a)
                }
            }
            override fun onCues(cueGroup: androidx.media3.common.text.CueGroup) {
                currentCues = cueGroup.cues
                val txt = cueGroup.cues.joinToString("\n") { it.text?.toString().orEmpty() }.ifBlank { null }
                if (txt != lastCueText) {
                    val prev = lastCueText
                    if (prev != null) cueBuffer = (cueBuffer + Triple(prev, lastCueStart, tickPos)).takeLast(600)
                    lastCueText = txt
                    lastCueStart = tickPos
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                val code = error.errorCodeName
                val format = runCatching { exo.videoFormat }.getOrNull()
                val describe = { f: androidx.media3.common.Format ->
                    (f.codecs ?: f.sampleMimeType ?: "?") + " " + f.width + "x" + f.height
                }
                val described = format?.let(describe) ?: runCatching {
                    exo.currentTracks.groups
                        .flatMap { g -> (0 until g.length).map { g.getTrackFormat(it) } }
                        .firstOrNull { it.height > 0 }
                        ?.let(describe)
                }.getOrNull() ?: "?"
                println(
                    "[CinephilePlayer] " + code +
                        " software=" + softwareOnly +
                        " url=" + streamUrl +
                        " video=" + described +
                        " cause=" + (error.cause?.let { it::class.simpleName + ": " + it.message } ?: "none"),
                )
                // A chosen decoder that dies mid-decode is the one failure the renderer
                // cannot fall back from, so retry the stream on software decoders once.
                if (!softwareOnly && code.contains("DECODING")) {
                    softwareOnly = true
                } else {
                    playbackError = code
                }
            }
        }
        exo.addListener(listener)
         onDispose {
             val pos = exo.currentPosition
             val dur = exo.duration.coerceAtLeast(0L)
             if (PlaybackStore.isFinished(pos, dur)) {
                 PlaybackStore.markCompleted(appCtx, storeKey, title, dur, streamUrl)
             } else if (pos >= 3_000L) {
                 PlaybackStore.saveProgress(appCtx, storeKey, title, pos, dur, streamUrl)
             }
             exo.removeListener(listener)
             exo.release()
         }
    }
    LaunchedEffect(statusPill) { if (statusPill != null) { delay(1000); statusPill = null } }

    LaunchedEffect(subtitleStyle, playerView) {
        playerView?.subtitleView?.let { applyCaptionStyle(it, subtitleStyle) }
    }

    LaunchedEffect(Unit) {
        while (true) {
            player?.let { p ->
                if (!scrubbing) positionMs = p.currentPosition
                bufferedMs = p.bufferedPosition
                if (p.duration > 0) durationMs = p.duration
            }
            delay(200)
        }
    }

    LaunchedEffect(Unit) {
         while (true) {
             delay(5000)
             if (durationMs > 0) {
                 val cp = player?.currentPosition ?: positionMs
                 onProgress(cp, durationMs)
                 PlaybackStore.saveProgress(appCtx, storeKey, title, cp, durationMs, streamUrl)
             }
         }
    }

    LaunchedEffect(controlsVisible, locked, isPlaying, sheet, scrubbing) {
        if (controlsVisible && !locked && isPlaying && sheet == null && !scrubbing) {
            delay(AUTO_HIDE_MS)
            controlsVisible = false
        }
    }


    val shownPosition = if (scrubbing) scrubPosition else positionMs

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            modifier = (if (resizeMode == ASPECT_16_9) Modifier.aspectRatio(16f / 9f)
                else if (resizeMode == ASPECT_4_3) Modifier.aspectRatio(4f / 3f)
                else Modifier.fillMaxSize()).graphicsLayer(scaleX = zoom, scaleY = zoom),
            factory = { ctx ->
                PlayerView(ctx).also { playerView = it }.apply {
                    useController = false
                    setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                    keepScreenOn = true
                }
            },
            update = { v ->
                v.player = player
                v.resizeMode = if (resizeMode < 0) AspectRatioFrameLayout.RESIZE_MODE_FIT else resizeMode
                v.subtitleView?.let { applyCaptionStyle(it, subtitleStyle) }
            },
        )

        // Parse the .vtt files ourselves (same idea as the web app's vtt-parser) so sync can shift cue timing.
        LaunchedEffect(subtitles) {
            cueSets = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                subtitles.map { s ->
                    runCatching {
                        val c = java.net.URL(s.url).openConnection() as java.net.HttpURLConnection
                        c.setRequestProperty("User-Agent", "Mozilla/5.0")
                        c.connectTimeout = 10000
                        c.readTimeout = 15000
                        parseVtt(c.inputStream.bufferedReader().readText())
                    }.getOrDefault(emptyList())
                }
            }
        }
        LaunchedEffect(player) {
            while (true) {
                player?.let { tickPos = it.currentPosition }
                kotlinx.coroutines.delay(80)
            }
        }
        LaunchedEffect(cueSets) {
        }
        val mediaText = currentCues.firstOrNull()?.text?.toString()
        LaunchedEffect(mediaText) {
            if (mediaText != null) {
                val norm = { s: String -> s.replace(Regex("\\s+"), " ").trim() }
                val mediaNorm = norm(mediaText)
                val c = cueSets.firstOrNull { set -> set.any { norm(it.text) == mediaNorm } }
                    ?.firstOrNull { norm(it.text) == mediaNorm }
                if (c != null) {
                    vttOffset = c.start - tickPos
                }
            }
        }
        LaunchedEffect(mediaText) {
            if (mediaText != null) {
                val idx = cueSets.indexOfFirst { set -> set.any { it.text == mediaText } }
                if (idx >= 0) subSetIndex = idx
            }
        }
        val captionText: String? = run {
            val mediaLine = currentCues.joinToString("\n") { it.text?.toString().orEmpty() }.ifBlank { null }
            val set = subSetIndex?.let { cueSets.getOrNull(it) }?.takeIf { it.isNotEmpty() }
                ?: cueSets.firstOrNull { it.isNotEmpty() }
            if (subtitleStyle.syncMs == 0) {
                mediaLine
            } else {
                val tPlayer = tickPos - subtitleStyle.syncMs
                val t = tPlayer + vttOffset
                val fromVtt = set?.firstOrNull { t >= it.start && t <= it.end }?.text

                val fromBuf = cueBuffer.lastOrNull { tPlayer >= it.second && tPlayer < it.third }?.first
                fromVtt ?: fromBuf ?: mediaLine
            }
        }

        // ---- Captions rendered by us, so the plate can be genuinely translucent ----
        if (subtitleStyle.enabled && !captionText.isNullOrBlank()) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = subtitleStyle.bottomOffsetPx.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                listOf(captionText).forEach { cue ->
                    val line = cue.orEmpty()
                    if (line.isNotBlank()) {
                        Text(
                            text = line,
                            color = Color(subtitleStyle.textColor),
                            fontSize = subtitleStyle.fontPx.sp,
                            fontFamily = when (subtitleStyle.fontFamily) {
                                "Serif" -> FontFamily.Serif
                                "Monospace" -> FontFamily.Monospace
                                else -> FontFamily.Default
                            },
                            textAlign = TextAlign.Center,
                            style = TextStyle(
                                shadow = when (subtitleStyle.bgStyle) {
                                    "Shadow" -> Shadow(Color.Black, Offset(1f, 3f), 5f)
                                    "Outline" -> Shadow(Color.Black, Offset(0f, 0f), 6f)
                                    else -> null
                                },
                            ),
                            modifier = Modifier
                                .background(
                                    if (subtitleStyle.bgStyle == "Box") {
                                        Color(subtitleStyle.bgColor).copy(alpha = subtitleStyle.bgOpacity.coerceIn(0, 100) / 100f)
                                    } else {
                                        Color.Transparent
                                    },
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
            }
        }
        // ---------------- gesture layer ----------------
        if (!locked) {
            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { controlsVisible = !controlsVisible },
                            onDoubleTap = { offset ->
                                val third = size.width / 3f
                                when {
                                    offset.x < third -> {
                                        player?.let { it.seekTo((it.currentPosition - 10_000).coerceAtLeast(0)) }
                                    }
                                    offset.x > third * 2 -> {
                                        player?.let { it.seekTo(it.currentPosition + 10_000) }
                                    }
                                    else -> {
                                        player?.let { if (it.isPlaying) it.pause() else it.play() }
                                    }
                                }
                            },

                        )
                    }
                    .pointerInput(Unit) {
                        detectTransformGestures { _, _, zoomChange, _ ->
                            zoom = (zoom * zoomChange).coerceIn(1f, 3f)
                        }
                    }
                    .pointerInput(boost) {
                        // Skipped while boosting so the hold-to-boost drag keeps the finger.
                        if (boost) return@pointerInput
                        detectVerticalDragGestures(
                            onDragStart = { offset ->
                                dragSide = if (offset.x < size.width / 2f) 1 else 2
                                if (dragSide == 1) {
                                    val current = activity?.window?.attributes?.screenBrightness ?: -1f
                                    brightStart = if (current < 0f) 0.5f else current
                                    brightTravel = 0f
                                } else {
                                    val am = context.getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager
                                    volStart = am.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)
                                    volTravel = 0f
                                }
                            },
                            onVerticalDrag = { _, dy ->
                                val height = size.height.toFloat().coerceAtLeast(1f)
                                if (dragSide == 1) {
                                    brightTravel += dy
                                    val next = (brightStart - brightTravel / height * 1.4f).coerceIn(0.02f, 1f)
                                    activity?.window?.let { w ->
                                        val lp = w.attributes
                                        lp.screenBrightness = next
                                        w.attributes = lp
                                    }
                                    statusPill = "\u2600 " + (next * 100).roundToInt() + "%"
                                } else {
                                    val am = context.getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager
                                    val max = am.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
                                    volTravel += dy
                                    val moved = (-volTravel / height * max * 1.4f).roundToInt()
                                    val next = (volStart + moved).coerceIn(0, max)
                                    am.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, next, 0)
                                    statusPill = "\uD83D\uDD0A " + next + "/" + max
                                }
                            },
                            onDragEnd = { },
                            onDragCancel = { },
                        )
                    }                    .pointerInput(Unit) {
                        // MX Player style: hold to boost, drag sideways to tune it,
                        // release to drop straight back to the chosen speed.
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                boost = true
                                boostBase = boostSpeed
                                boostTravel = 0f
                                player?.setPlaybackSpeed(boostSpeed)
                            },
                            onDragEnd = {
                                boost = false
                                player?.setPlaybackSpeed(speed)
                            },
                            onDragCancel = {
                                boost = false
                                player?.setPlaybackSpeed(speed)
                            },
                            onDrag = { _, dragAmount ->
                                boostTravel += dragAmount.x
                                // ~1.6 px per 0.01x: half a second of travel either way is
                                // about 0.15x, a full comfortable swipe spans the range.
                                val raw = boostBase + boostTravel / 160f
                                val stepped = (raw * 10f).roundToInt() / 10f
                                if (stepped != boostSpeed) {
                                    boostSpeed = stepped.coerceIn(0.5f, 4f)
                                    player?.setPlaybackSpeed(boostSpeed)
                                    SubtitlePrefs.setBoostSpeed(appCtx, boostSpeed)
                                }
                            },
                        )
                    },
            )
        }

        if (boost) {
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 26.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0x99000000))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            ) {
                Text(
                    text = fmtSpeed(boostSpeed),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontFamily = GeistMono,
                )
            }
        }

        statusPill?.let { label ->

            Box(

                Modifier

                    .align(Alignment.TopCenter)

                    .padding(top = 26.dp)

                    .clip(RoundedCornerShape(999.dp))

                    .background(Color(0x99000000))

                    .padding(horizontal = 14.dp, vertical = 6.dp),

            ) {

                Text(text = label, color = Color.White, fontSize = 13.sp, fontFamily = GeistMono)

            }

        }


        if (isBuffering && playbackError == null) {
            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 2.5.dp,
                modifier = Modifier.align(Alignment.Center).size(52.dp),
            )
        }

        playbackError?.let { code ->
            Column(
                Modifier.align(Alignment.Center).padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Can't play this stream", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text(code, color = Color(0xFFA1A1AA), fontSize = 12.sp)
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "Retry",
                    color = Color.Black,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.White)
                        .clickable { playbackError = null; player?.prepare(); player?.play() }
                        .padding(horizontal = 22.dp, vertical = 10.dp),
                )
            }
        }



        // locked: only the unlock affordance
        if (locked) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 20.dp)
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color(0x99000000))
                    .clickable { locked = false },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Lock, "Unlock", tint = Color.White, modifier = Modifier.size(21.dp))
            }
        }

        // ---------------- overlay ----------------
        // Flat replication of the reference player: black background, white glyphs,
        // no gradients, no scrims, no centred play button, no kebab menu.
        val alpha by animateFloatAsState(
            targetValue = if (controlsVisible && !locked) 1f else 0f,
            animationSpec = tween(200),
            label = "overlay-alpha",
        )

        if (alpha > 0.01f && !locked) {
            Box(Modifier.fillMaxSize().graphicsLayer(alpha = alpha)) {
                // TOP: no background at all - a white chevron and the title only.
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0x99000000), Color.Transparent),
                            ),
                        )
                        .padding(start = 26.dp, end = 20.dp, top = 26.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onBack),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ChevronLeft,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                    Spacer(Modifier.width(2.dp))
                    Text(
                        text = title,
                        style = TextStyle(shadow = OverlayShadow),
                        color = Color.White,
                        fontSize = 17.sp,
                        fontFamily = GeistMono,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        
                        modifier = Modifier.weight(1f),
                    )
                }

                // BOTTOM: thin seek bar, then exactly one row of controls.
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color(0xB3000000)),
                            ),
                        )
                        .padding(start = 28.dp, end = 28.dp, bottom = 22.dp),
                ) {
                    if (scrubbing) {
                        Text(
                            text = "${fmt(shownPosition)} / ${fmt(durationMs)}",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            style = TextStyle(shadow = OverlayShadow),
                            modifier = Modifier.padding(start = 4.dp, bottom = 2.dp),
                        )
                    }

                    ThinSeekBar(
                        position = shownPosition,
                        duration = durationMs,
                        buffered = bufferedMs,
                        onScrub = { fraction ->
                            scrubbing = true
                            scrubPosition = (fraction * durationMs).toLong()
                        },
                        onSeek = { fraction ->
                            player?.let { p -> p.seekTo((fraction * durationMs).toLong()) }
                            positionMs = (fraction * durationMs).toLong()
                            scrubbing = false
                        },
                    )

                    Spacer(Modifier.height(16.dp))
                    Box(Modifier.fillMaxWidth().height(48.dp)) {
                        // cluster 1 - subtitles, audio, timestamp (pinned to the start edge)
                        Row(
                            modifier = Modifier.align(Alignment.CenterStart),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            BarIcon(Icons.Filled.Subtitles, "Subtitles") { tab = 2; sheet = SheetKind.Settings }
                            BarIcon(
                                icon = Icons.Filled.VolumeUp,
                                description = "Audio track",
                            ) { tab = 1; sheet = SheetKind.Settings }
                            Text(
                                text = if (showRemaining) {
                                    "${fmt(shownPosition)} / -${fmt((durationMs - shownPosition).coerceAtLeast(0))}"
                                } else {
                                    "${fmt(shownPosition)} / ${fmt(durationMs)}"
                                },
                                color = Color(0xFFEDEDED),
                                fontSize = 13.sp,
                                fontFamily = GeistMono,
                                maxLines = 1,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable {
                                        val next = !showRemaining
                                        showRemaining = next
                                        PlayerPrefs.setShowRemaining(appCtx, next)
                                    }
                                    .padding(horizontal = 4.dp, vertical = 4.dp),
                            )
                        }

                        // cluster 2 - transport, pinned to the TRUE screen centre
                        Row(
                            modifier = Modifier.align(Alignment.Center),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(30.dp),
                        ) {
                            BarIcon(Icons.Filled.Replay10, "Back 10 seconds") {
                                player?.let { it.seekTo((it.currentPosition - 10_000).coerceAtLeast(0)) }
                            }
                            BarIcon(
                                icon = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                description = if (isPlaying) "Pause" else "Play",
                                iconSize = 36.dp,
                            ) {
                                player?.let { if (it.isPlaying) it.pause() else it.play() }
                            }
                            BarIcon(Icons.Filled.Forward10, "Forward 10 seconds") {
                                player?.let { it.seekTo(it.currentPosition + 10_000) }
                            }
                        }

                        // cluster 3 - speed + aspect ratio (pinned to the end edge)
                        Row(
                            modifier = Modifier.align(Alignment.CenterEnd),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            BarIcon(Icons.Filled.Speed, "Playback speed") { tab = 0; sheet = SheetKind.Settings }
                            BarIcon(Icons.Filled.AspectRatio, "Aspect ratio") { tab = 3; sheet = SheetKind.Settings }
                        }
                    }
                }
            }
        }

        sheet?.let { kind ->
            PlayerSettingsSheet(
                kind = kind,
                title = title,
                speed = speed,
                boost = boost,
                onSpeed = { s ->
                    speed = s
                    boost = false
                    player?.setPlaybackSpeed(s)
                    SubtitlePrefs.saveSpeed(appCtx, storeKey, s)
                },
                audioTracks = audioTracks,
                textTracks = textTracks,
                onSelectAudio = { t ->
                    player?.let { p -> selectTrack(p, C.TRACK_TYPE_AUDIO, t.id) }
                    SubtitlePrefs.saveAudio(appCtx, storeKey, t.id)
                },
                onSelectText = { t -> player?.let { p -> selectTrack(p, C.TRACK_TYPE_TEXT, t.id) } },
                captionScale = captionScale,
                onCaptionScale = { captionScale = it },
                captionBg = captionBg,
                onCaptionBg = { captionBg = it },
                style = subtitleStyle,
                onStyle = { subtitleStyle = it },
                tab = tab,
                // The "more" list only switches the tab; make it actually open that pane.
                onTab = { t -> tab = t; sheet = SheetKind.Settings },
                resizeMode = resizeMode,
                onResize = {
                    resizeMode = it
                    SubtitlePrefs.saveAspect(appCtx, storeKey, it)
                },
                onPip = { activity?.let { enterPip(it) } },
                onLock = { locked = true; sheet = null },
                onDismiss = { sheet = null },
            )
        }
    }
}

/** Full subtitle styling state, mirroring the web player's Sub Settings panel. */
internal data class SubtitleStyle(
    val enabled: Boolean = true,
    val fontPx: Int = 18,
    val bgOpacity: Int = 0,
    val bottomOffsetPx: Int = 80,
    val syncMs: Int = 0,
    val textColor: Int = -0x1,
    val bgColor: Int = -0x1000000,
    val bgStyle: String = "Shadow",
    val fontFamily: String = "Default",
)

private enum class SheetKind { More, Settings }

/**
 * One flat bottom-bar control: a bare white glyph on a transparent background.
 * 42dp touch target, 21dp glyph (the play/pause glyph is a touch larger).
 */
@Composable
private fun BarIcon(
    icon: ImageVector,
    description: String,
    iconSize: Dp = 28.dp,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier.size(46.dp).clip(CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = Color.White,
            modifier = Modifier.size(iconSize),
        )
    }
}

/**
 * Thin 4dp progress line: unplayed track is translucent white, the played part is
 * amber. No thumb while resting; while dragging the amber fill follows the finger
 * and a small white dot is shown. The touch area stays a comfortable 28dp tall.
 */
@Composable
private fun ThinSeekBar(
    position: Long,
    duration: Long,
    buffered: Long = 0L,
    onScrub: (Float) -> Unit,
    onSeek: (Float) -> Unit,
) {
    var dragFraction by remember { mutableFloatStateOf(-1f) }
    val played = if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f
    val bufferedFraction = if (duration > 0L) (buffered.toFloat() / duration).coerceIn(0f, 1f) else 0f
    val shown = if (dragFraction >= 0f) dragFraction else played

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .pointerInput(duration) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        dragFraction = (offset.x / size.width).coerceIn(0f, 1f)
                        onScrub(dragFraction)
                    },
                    onDragEnd = { if (dragFraction >= 0f) onSeek(dragFraction); dragFraction = -1f },
                    onDragCancel = { if (dragFraction >= 0f) onSeek(dragFraction); dragFraction = -1f },
                ) { change, _ ->
                    dragFraction = (change.position.x / size.width).coerceIn(0f, 1f)
                    onScrub(dragFraction)
                }
            }
            .pointerInput(duration) {
                detectTapGestures { offset ->
                    onSeek((offset.x / size.width).coerceIn(0f, 1f))
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Canvas(Modifier.fillMaxWidth().height(28.dp)) {
            val cy = size.height / 2f
            val trackH = 5.dp.toPx()
            val r = CornerRadius(trackH / 2f, trackH / 2f)

            drawRoundRect(
                color = Color(0x59FFFFFF),
                topLeft = Offset(0f, cy - trackH / 2f),
                size = Size(size.width, trackH),
                cornerRadius = r,
            )
            // Buffered ahead: lighter section for what is already downloaded.
            if (bufferedFraction > 0f) {
                drawRoundRect(
                    color = Color(0x8CFFFFFF),
                    topLeft = Offset(0f, cy - trackH / 2f),
                    size = Size(size.width * bufferedFraction, trackH),
                    cornerRadius = r,
                )
            }
            if (shown > 0f) {
                drawRoundRect(
                    color = Color(0xFFF5A623),
                    topLeft = Offset(0f, cy - trackH / 2f),
                    size = Size(size.width * shown, trackH),
                    cornerRadius = r,
                )
            }
            if (dragFraction >= 0f) {
                drawCircle(
                    color = Color.White,
                    radius = 5.dp.toPx(),
                    center = Offset(size.width * shown, cy),
                )
            }
        }
    }
}

@Composable
private fun PlayerSettingsSheet(
    kind: SheetKind,
    title: String,
    speed: Float,
    boost: Boolean,
    onSpeed: (Float) -> Unit,
    audioTracks: List<PlayerTrack>,
    textTracks: List<PlayerTrack>,
    onSelectAudio: (PlayerTrack) -> Unit,
    onSelectText: (PlayerTrack) -> Unit,
    captionScale: Float,
    onCaptionScale: (Float) -> Unit,
    captionBg: Boolean,
    onCaptionBg: (Boolean) -> Unit,
    style: SubtitleStyle,
    onStyle: (SubtitleStyle) -> Unit,
    tab: Int,
    onTab: (Int) -> Unit,
    resizeMode: Int,
    onResize: (Int) -> Unit,
    onPip: () -> Unit,
    onLock: () -> Unit,
    onDismiss: () -> Unit,
) {
    var subSettings by remember { mutableStateOf(false) }
    val menuTitle = when {
        kind == SheetKind.More -> title
        tab == 3 -> "Aspect ratio"
        tab == 0 -> "Playback speed"
        tab == 1 -> "Audio"
        else -> "Subtitles"
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xCC000000))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .fillMaxWidth(0.68f)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF1E1E1E))
                .clickable(enabled = false) {},
        ) {
            // Header strip with the menu name and a thin close cross.
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(Color(0xFF252525))
                    .padding(start = 16.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = menuTitle,
                    color = Color.White,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    Modifier.size(34.dp).clip(CircleShape).clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Close, "Close", tint = Color(0xFFAAAAAA), modifier = Modifier.size(16.dp))
                }
            }

            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                when {
                    kind == SheetKind.More -> {
                        ModalRow("Playback speed", false) { onTab(0) }
                        ModalRow("Audio", false) { onTab(1) }
                        ModalRow("Subtitles", false) { onTab(2) }
                        ModalRow("Aspect ratio", false) { onTab(3) }
                        ModalRow("Picture in picture", false) { onPip(); onDismiss() }
                        ModalRow("Lock screen", false) { onLock() }
                    }

                    tab == 3 -> {
                        listOf(
                            "Fit" to AspectRatioFrameLayout.RESIZE_MODE_FIT,
                            "Fill (crop)" to AspectRatioFrameLayout.RESIZE_MODE_ZOOM,
                            "Stretch" to AspectRatioFrameLayout.RESIZE_MODE_FILL,
                            "16:9" to ASPECT_16_9,
                            "4:3" to ASPECT_4_3,                        ).forEach { (label, mode) ->
                            ModalRow(label, resizeMode == mode) { onResize(mode); onDismiss() }
                        }
                    }

                    tab == 0 -> {
                        SPEED_STEPS.chunked(4).forEach { row ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                row.forEach { s ->
                                    val on = !boost && speed == s
                                    Box(
                                        Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (on) Color(0xFFF5A623) else Color(0x1FFFFFFF))
                                            .clickable { onSpeed(s) }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = if (s == 1f) "1x" else fmtSpeed(s),
                                            color = if (on) Color(0xFF101014) else Color(0xFFEDEDED),
                                            fontSize = 12.sp,
                                        )
                                    }
                                }
                            }
                        }
                        SliderRow("CUSTOM SPEED", speed, 0.25f, 4f, fmtSpeed(speed)) { onSpeed(it) }
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SyncNudgeButton("\u22120.05x") { onSpeed((speed - 0.05f).coerceIn(0.25f, 4f)) }
                            SyncNudgeButton("+0.05x") { onSpeed((speed + 0.05f).coerceIn(0.25f, 4f)) }
                            Spacer(Modifier.weight(1f))
                            SyncNudgeButton("Reset 1x") { onSpeed(1f) }
                        }
                    }

                    tab == 1 -> {
                        if (audioTracks.isEmpty()) {
                            ModalHint("Only one audio track available.")
                        } else {
                            audioTracks.forEach { track ->
                                ModalRow(track.label, track.selected) { onSelectAudio(track) }
                            }
                        }
                    }

                    else -> {
                        if (subSettings) {





                            SliderRow("SUBTITLE SYNC", style.syncMs.toFloat(), -30000f, 30000f, syncLabel(style.syncMs)) { onStyle(style.copy(syncMs = it.toInt())) }
                            ModalHint("Negative = subtitles appear EARLIER  \u00b7  Positive = LATER")
                            SyncNudgeRow(
                                onNudge = { d -> onStyle(style.copy(syncMs = (style.syncMs + d).coerceIn(-30000, 30000))) },
                                onReset = { onStyle(style.copy(syncMs = 0)) },
                            )














                            ModalRow("Back to subtitles", false) { subSettings = false }
                        } else {
                            ModalRow("Off", !style.enabled) { onStyle(style.copy(enabled = false)) }
                            if (textTracks.isEmpty()) {
                                ModalHint("No subtitle tracks in this stream.")
                            } else {
                                textTracks.forEach { track ->
                                    ModalRow(track.label, track.selected && style.enabled) {
                                        onSelectText(track)
                                        onStyle(style.copy(enabled = true))
                                    }
                                }
                            }
                            ModalRow("Subtitle Sync", false) { subSettings = true }
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

/** One tall, left-aligned row; the selected one gets the amber dot. */
@Composable
private fun ModalRow(
    label: String,
    selected: Boolean,
    trailing: String? = null,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 45.dp)
            .background(if (selected) Color(0xFF3E3A20) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 15.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        trailing?.let {
            Text(it, color = Color(0xFFAAAAAA), fontSize = 13.sp)
        }
        if (selected) {
            Spacer(Modifier.width(10.dp))
            Box(Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFF2C94C)))
        }
    }
}

@Composable
private fun ModalHint(text: String) {
    Text(
        text = text,
        color = Color(0xFFAAAAAA),
        fontSize = 14.sp,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 22.dp),
    )
}

@Composable
private fun SheetAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier.width(64.dp).clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(28.dp))
        Spacer(Modifier.height(6.dp))
        Text(label, color = Color(0xFFA1A1AA), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun TrackList(tracks: List<PlayerTrack>, emptyLabel: String, onSelect: (PlayerTrack) -> Unit) {
    if (tracks.isEmpty()) {
        Text(emptyLabel, color = Color(0xFF71717A), fontSize = 12.sp)
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        tracks.forEach { track ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (track.selected) Color(0xFF232329) else Color.Transparent)
                    .clickable { onSelect(track) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = track.label,
                    color = if (track.selected) Color.White else Color(0xFFA1A1AA),
                    fontSize = 13.sp,
                    fontWeight = if (track.selected) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (track.selected) Text("Playing", color = Color(0xFF22C55E), fontSize = 11.sp)
            }
        }
    }
}

/* ------------------------------- helpers ------------------------------- */

@UnstableApi
private fun refreshTracks(player: ExoPlayer, apply: (List<PlayerTrack>, List<PlayerTrack>) -> Unit) {
    val audio = mutableListOf<PlayerTrack>()
    val text = mutableListOf<PlayerTrack>()
    player.currentTracks.groups.forEach { group ->
        for (i in 0 until group.length) {
            val format = group.getTrackFormat(i)
            val id = "${format.language ?: "und"}#${format.label ?: i}#$i"
            val label = buildString {
                append(format.label ?: format.language?.uppercase() ?: "Track ${i + 1}")
                if (format.channelCount > 0) append("  ${format.channelCount}ch")
            }
            when (group.type) {
                C.TRACK_TYPE_AUDIO -> audio += PlayerTrack(id, label, group.isTrackSelected(i))
                C.TRACK_TYPE_TEXT -> text += PlayerTrack(id, label, group.isTrackSelected(i))
            }
        }
    }
    apply(audio, text)
}

@UnstableApi
private fun selectTrack(player: ExoPlayer, type: Int, id: String) {
    player.currentTracks.groups.forEach { group ->
        if (group.type != type) return@forEach
        for (i in 0 until group.length) {
            val format = group.getTrackFormat(i)
            val candidate = "${format.language ?: "und"}#${format.label ?: i}#$i"
            if (candidate == id) {
                player.trackSelectionParameters = player.trackSelectionParameters
                    .buildUpon()
                    .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, i))
                    .build()
                return
            }
        }
    }
}

private fun fmt(ms: Long): String {
    if (ms <= 0) return "0:00"
    val total = ms / 1000
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    fun two(v: Long) = v.toString().padStart(2, '0')
    return if (h > 0) "$h:${two(m)}:${two(s)}" else "$m:${two(s)}"
}

private fun fmtSpeed(speed: Float): String {
    val rounded = kotlin.math.round(speed * 100f) / 100f
    val asInt = rounded.toInt()
    return if (rounded == asInt.toFloat()) "${asInt}.0x" else "${rounded}x"
}

private fun aspectLabel(mode: Int): String = when (mode) {
    ASPECT_16_9 -> "16:9"
    ASPECT_4_3 -> "4:3"
    AspectRatioFrameLayout.RESIZE_MODE_FIT -> "Fit"
    AspectRatioFrameLayout.RESIZE_MODE_FILL -> "Stretch"
    AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> "Crop"
    else -> "Fit"
}

private fun nextResize(current: Int): Int = when (current) {
    AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
    AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_FILL
    AspectRatioFrameLayout.RESIZE_MODE_FILL -> AspectRatioFrameLayout.RESIZE_MODE_FIT
    else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
}

private fun enterPip(activity: Activity) {
    runCatching {
        activity.enterPictureInPictureMode(
            PictureInPictureParams.Builder().setAspectRatio(Rational(16, 9)).build(),
        )
    }
}

private fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}


@Composable
private fun SliderRow(label: String, value: Float, from: Float, to: Float, trailing: String, onChange: (Float) -> Unit) {
    ModalRow(label, false, trailing = trailing) {}
    Column(Modifier.padding(horizontal = 16.dp, vertical = 2.dp)) {
        Slider(
            value = value.coerceIn(from, to),
            onValueChange = onChange,
            valueRange = from..to,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color(0x33FFFFFF),
            ),
        )
    }
}

@Composable
private fun StyleChips(options: List<String>, selected: String, onPick: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { o ->
            val on = selected == o
            Text(
                text = o,
                color = if (on) Color.Black else Color(0xFFA1A1AA),
                fontSize = 12.sp,
                maxLines = 1,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (on) Color.White else Color(0xFF232329))
                    .clickable { onPick(o) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun ColorRow(presets: List<Int>, selected: Int, onPick: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        presets.forEach { c ->
            Box(
                Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(Color(c))
                    .border(2.dp, if (selected == c) Color.White else Color(0x33FFFFFF), CircleShape)
                    .clickable { onPick(c) },
            )
        }
    }
}

private fun hexColor(argb: Int): String =
    java.lang.String.format("#%06X", argb and 0xFFFFFF)


/** Applies the subtitle style to Media3's renderer. Called on every style change AND every view update. */
private fun applyCaptionStyle(sv: androidx.media3.ui.SubtitleView, st: SubtitleStyle) {
    // Hide Media3's own caption view - it always paints an opaque plate, so we draw captions ourselves.
    sv.visibility = android.view.View.GONE
    sv.setApplyEmbeddedStyles(false)
    sv.setApplyEmbeddedFontSizes(false)
    sv.setStyle(captionStyleOf(st))
    sv.setFractionalTextSize(0.0533f)
    sv.setBottomPaddingFraction(0f)
}

private fun captionStyleOf(st: SubtitleStyle): CaptionStyleCompat {
    val edge = when (st.bgStyle) {
        "Outline" -> CaptionStyleCompat.EDGE_TYPE_OUTLINE
        "None" -> CaptionStyleCompat.EDGE_TYPE_NONE
        else -> CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW
    }
    val boxOn = st.bgStyle == "Box"
    val bg = if (boxOn) {
        android.graphics.Color.argb(
            (st.bgOpacity.coerceIn(0, 100) * 255 / 100),
            android.graphics.Color.red(st.bgColor),
            android.graphics.Color.green(st.bgColor),
            android.graphics.Color.blue(st.bgColor),
        )
    } else {
        android.graphics.Color.TRANSPARENT
    }
    val tf = when (st.fontFamily) {
        "Serif" -> android.graphics.Typeface.SERIF
        "Monospace" -> android.graphics.Typeface.MONOSPACE
        "Sans-serif", "Arial", "Verdana" -> android.graphics.Typeface.SANS_SERIF
        else -> null
    }
    // backgroundColor = the tight plate drawn behind the text only.
    // windowColor must stay transparent - it fills the entire caption window (whole screen band).
    return CaptionStyleCompat(
        st.textColor,
        if (boxOn) bg else android.graphics.Color.TRANSPARENT,
        android.graphics.Color.TRANSPARENT,
        edge,
        android.graphics.Color.BLACK,
        tf,
    )
}

/** One parsed WebVTT cue, in milliseconds. */
private data class VttCue(val start: Long, val end: Long, val text: String)

private fun vttTimeToMs(raw: String): Long {
    val m = Regex("(\\d+):(\\d{2}):(\\d{2})[.,](\\d{1,3})").find(raw.trim()) ?: return -1L
    val (h, mi, s, ms) = m.destructured
    return h.toLong() * 3600000L + mi.toLong() * 60000L + s.toLong() * 1000L + ms.padEnd(3, '0').toLong()
}

/** Minimal WebVTT parser: timing line plus the text lines that follow it. */
private fun parseVtt(raw: String): List<VttCue> {
    val out = mutableListOf<VttCue>()
    val blocks = raw.replace("\r\n", "\n").replace("\r", "\n").split("\n\n")
    for (block in blocks) {
        val lines = block.trim().lines()
        val i = lines.indexOfFirst { it.contains("-->") }
        if (i < 0) continue
        val halves = lines[i].split("-->")
        if (halves.size < 2) continue
        val start = vttTimeToMs(halves[0])
        val end = vttTimeToMs(halves[1].trim().substringBefore(" "))
        val text = lines.drop(i + 1)
            .joinToString("\n")
            .replace(Regex("<[^>]+>"), "")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .trim()
        if (start >= 0 && end > start && text.isNotEmpty()) out.add(VttCue(start, end, text))
    }
    return out
}

/** Plain-language sync offset, so the direction is never ambiguous. */
private fun syncLabel(ms: Int): String {
    if (ms == 0) return "0.0 s \u00b7 in sync"
    val secs = kotlin.math.abs(ms) / 1000f
    val txt = String.format(java.util.Locale.US, "%.1f", secs) + " s"
    return if (ms > 0) "+" + txt + " \u00b7 subtitles later" else "\u2212" + txt + " \u00b7 subtitles earlier"
}

/** Quick nudges plus reset, so the wider sync range stays usable. */
@Composable
private fun SyncNudgeRow(onNudge: (Int) -> Unit, onReset: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SyncNudgeButton("\u22125 s") { onNudge(-5000) }
        SyncNudgeButton("\u22120.5 s") { onNudge(-500) }
        SyncNudgeButton("+0.5 s") { onNudge(500) }
        SyncNudgeButton("+5 s") { onNudge(5000) }
        Spacer(Modifier.weight(1f))
        SyncNudgeButton("Reset") { onReset() }
    }
}

@Composable
private fun SyncNudgeButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0x1FFFFFFF))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 7.dp),
    ) {
        Text(label, color = Color(0xFFEDEDED), fontSize = 12.sp)
    }
}

/** Remembers the subtitle style on the device so one choice applies to every video until changed. */
internal object SubtitlePrefs {
    private const val NAME = "beam_subtitle_style"

    private fun prefs(ctx: android.content.Context) =
        ctx.getSharedPreferences(NAME, android.content.Context.MODE_PRIVATE)

    /** Per-video subtitle sync, so each title keeps its own offset. */
    fun loadSync(ctx: android.content.Context, key: String): Int =
        prefs(ctx).getInt("sync:" + key.hashCode(), 0)

    fun saveSync(ctx: android.content.Context, key: String, ms: Int) {
        prefs(ctx).edit().putInt("sync:" + key.hashCode(), ms).apply()
    }

    /** Aspect ratio remembered per stream. */
    fun saveAspect(ctx: android.content.Context, key: String, mode: Int) {
        prefs(ctx).edit().putInt("aspect:" + key.hashCode(), mode).apply()
    }

    fun loadAspect(ctx: android.content.Context, key: String): Int? {
        val p = prefs(ctx)
        val k = "aspect:" + key.hashCode()
        return if (p.contains(k)) p.getInt(k, AspectRatioFrameLayout.RESIZE_MODE_FIT) else null
    }

    /** Hold-to-boost speed: shared by every video, changed by dragging. */
    fun boostSpeed(ctx: android.content.Context): Float =
        prefs(ctx).getFloat("boost_speed", 2f)

    fun setBoostSpeed(ctx: android.content.Context, value: Float) {
        prefs(ctx).edit().putFloat("boost_speed", value).apply()
    }

    /** Playback speed remembered for one stream (per video). */
    fun saveSpeed(ctx: android.content.Context, key: String, speed: Float) {
        prefs(ctx).edit().putFloat("speed:" + key.hashCode(), speed).apply()
    }

    fun loadSpeed(ctx: android.content.Context, key: String): Float? {
        val p = prefs(ctx)
        val k = "speed:" + key.hashCode()
        return if (p.contains(k)) p.getFloat(k, 1f) else null
    }

    /** Audio track id remembered for one stream (per video, like the web app). */
    fun saveAudio(ctx: android.content.Context, key: String, id: String) {
        prefs(ctx).edit().putString("audio:" + key.hashCode(), id).apply()
    }

    fun loadAudio(ctx: android.content.Context, key: String): String? =
        prefs(ctx).getString("audio:" + key.hashCode(), null)

    fun save(ctx: android.content.Context, s: SubtitleStyle) {
        prefs(ctx).edit()
            .putBoolean("enabled", s.enabled)
            .putInt("fontPx", s.fontPx)
            .putInt("bgOpacity", s.bgOpacity)
            .putInt("bottomOffsetPx", s.bottomOffsetPx)
            .putInt("textColor", s.textColor)
            .putInt("bgColor", s.bgColor)
            .putString("bgStyle", s.bgStyle)
            .putString("fontFamily", s.fontFamily)
            .apply()
    }

    fun load(ctx: android.content.Context): SubtitleStyle {
        val p = prefs(ctx)
        return SubtitleStyle(
            enabled = p.getBoolean("enabled", true),
            fontPx = p.getInt("fontPx", 18),
            bgOpacity = p.getInt("bgOpacity", 0),
            bottomOffsetPx = p.getInt("bottomOffsetPx", 80),
            syncMs = p.getInt("syncMs", 0),
            textColor = p.getInt("textColor", -0x1),
            bgColor = p.getInt("bgColor", -0x1000000),
            bgStyle = p.getString("bgStyle", "Shadow") ?: "Shadow",
            fontFamily = p.getString("fontFamily", "Default") ?: "Default",
        )
    }
}

/** Player preferences that must outlive a single video. */
internal object PlayerPrefs {
    private const val FILE = "beam_player_prefs"

    private fun prefs(ctx: android.content.Context) =
        ctx.applicationContext.getSharedPreferences(FILE, android.content.Context.MODE_PRIVATE)

    /** true = show time remaining, false = show full duration. */
    fun showRemaining(ctx: android.content.Context): Boolean =
        prefs(ctx).getBoolean("show_remaining", false)

    fun setShowRemaining(ctx: android.content.Context, value: Boolean) {
        prefs(ctx).edit().putBoolean("show_remaining", value).apply()
    }
}

/**
 * Android software decoders only. Hardware decoders can accept a profile and then
 * fail while decoding, which no amount of renderer fallback recovers from - retrying
 * the same stream here does.
 */
@androidx.media3.common.util.UnstableApi
private object SoftwareOnlyCodecs : androidx.media3.exoplayer.mediacodec.MediaCodecSelector {
    override fun getDecoderInfos(
        mimeType: String,
        requiresSecureDecoder: Boolean,
        requiresTunnelingDecoder: Boolean,
    ): List<androidx.media3.exoplayer.mediacodec.MediaCodecInfo> =
        androidx.media3.exoplayer.mediacodec.MediaCodecSelector.DEFAULT
            .getDecoderInfos(mimeType, requiresSecureDecoder, requiresTunnelingDecoder)
            .filter { it.softwareOnly }
}
