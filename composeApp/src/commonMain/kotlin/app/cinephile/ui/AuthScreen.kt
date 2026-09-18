package app.cinephile.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.cinephile.data.Session
import app.cinephile.data.SessionManager
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/* ------------------------------------------------------------------ *
 * Cinephile auth - built to the reference spec.
 *
 * Exact tokens from the spec:
 *   background #0e0e0d   foreground #f5f4f1   card #161514
 *   border     #3a3733   input      #2f2c28   accent #e8a13a
 *   muted      #8e8a82
 *
 * Values are literals on purpose: the app has three competing colour
 * systems and a token indirection is what let the wrong colour win
 * before. Nothing here can resolve to white by accident.
 * ------------------------------------------------------------------ */

private val Bg = Color(0xFF0E0E0D)
private val Fg = Color(0xFFF5F4F1)
private val Card = Color(0xFF161514)
private val CardBorder = Color(0xFF3A3733)
private val InputBg = Color(0x662F2C28)
private val Accent = Color(0xFFE8A13A)
private val Muted = Color(0xFF8E8A82)
private val Amber200 = Color(0xFFF3D08A)

/** Deterministic starfield: same 60 stars every launch, like the web build. */
private class Rand(seed: Int) {
    private var s = seed
    fun next(): Float {
        s = (s + 0x6D2B79F5)
        var t = (s xor (s ushr 15)) * (1 or s)
        t = (t + ((t xor (t ushr 7)) * (61 or t))) xor t
        return ((t xor (t ushr 14)) ushr 0).toFloat() / 4294967296f
    }
}

private data class Star(val x: Float, val y: Float, val size: Float, val base: Float, val phase: Float)

private val Stars: List<Star> = run {
    val r = Rand(42)
    List(60) {
        Star(
            x = r.next(),
            y = r.next(),
            size = 1f + r.next() * 1.5f,
            base = 0.25f + r.next() * 0.5f,
            phase = r.next() * 6.2832f,
        )
    }
}

@Composable
fun AuthScreen(onAuthed: () -> Unit = {}) {
    var step by remember { mutableStateOf("welcome") } // welcome | name
    var guest by remember { mutableStateOf(true) }
    var name by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf<app.cinephile.data.GoogleUser?>(null) }

    val twinkle = rememberInfiniteTransition(label = "twinkle")
    val beat by twinkle.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4000), RepeatMode.Restart),
        label = "beat",
    )

    Box(Modifier.fillMaxSize().background(Bg)) {
        // --- starfield ---
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val w = maxWidth
            val h = maxHeight
            Stars.forEach { s ->
                val a = (s.base + 0.35f * sin(beat * 2f * PI.toFloat() + s.phase)).coerceIn(0.06f, 0.95f)
                Box(
                    Modifier
                        .offset(x = w * s.x, y = h * s.y)
                        .size(s.size.dp)
                        .background(Color.White.copy(alpha = a), CircleShape),
                )
            }
        }

        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars),
        ) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(
                    Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 24.dp)
                        .widthIn(max = 448.dp),
                ) {
                    if (step == "welcome") {
                        WelcomeCard(
                            notice = notice,
                            onGuest = {
                                guest = true
                                step = "name"
                            },
                            onGoogle = {
                                if (busy) return@WelcomeCard
                                busy = true
                                notice = null
                                scope.launch {
                                    val g = app.cinephile.data.signInWithGoogle()
                                    busy = false
                                    if (g == null) {
                                        notice = "Google sign-in was cancelled."
                                    } else {
                                        pending = g
                                        guest = false
                                        step = "name"
                                    }
                                }
                            },
                        )
                    } else {
                        NameCard(
                            name = name,
                            isGuest = guest,
                            busy = busy,
                            onName = { name = it },
                            onBack = { step = "welcome" },
                            onContinue = {
                                val typed = name.trim()
                                val g = pending
                                if (g == null) {
                                    println("[CinephileAuth] guest continue, name='$typed'")
                                    SessionManager.set(Session(method = "guest", displayName = typed))
                                    onAuthed()
                                } else {
                                    busy = true
                                    notice = null
                                    scope.launch {
                                        val api = app.cinephile.core.network.servicesOrNull?.beamApi
                                        val res = runCatching { api?.authGoogleToken(g.idToken, typed) }.getOrNull()
                                        busy = false
                                        if (res == null) {
                                            notice = "Could not finish sign-in. Try again in a moment."
                                        } else {
                                            println("[CinephileAuth] google session created for '${res.user?.email}'")
                                            SessionManager.set(
                                                Session(
                                                    method = "google",
                                                    token = res.token,
                                                    email = res.user?.email,
                                                    role = res.user?.role,
                                                    displayName = typed,
                                                ),
                                            )
                                            onAuthed()
                                        }
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

/* ------------------------------------------------------------------ */

@Composable
private fun Wordmark(size: Int = 26, centred: Boolean = true) {
    Row(
        modifier = if (centred) Modifier.fillMaxWidth() else Modifier,
        horizontalArrangement = if (centred) Arrangement.Center else Arrangement.Start,
        verticalAlignment = Alignment.Bottom,
    ) {
        Text("Cine", color = Fg, fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = size.sp, letterSpacing = (-0.5).sp)
        Text("phile", color = Accent, fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontStyle = FontStyle.Italic, fontSize = size.sp, letterSpacing = (-0.5).sp)
    }
}



@Composable
private fun CardShellReal(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Card)
            .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
            .padding(28.dp),
    ) { content() }
}

@Composable
private fun WelcomeCard(notice: String?, onGuest: () -> Unit, onGoogle: () -> Unit) {
    CardShellReal {
        Wordmark(size = 26)
        Spacer(Modifier.height(18.dp))
        Text(
            "Your Go-To",
            color = Fg,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.SemiBold,
            fontSize = 34.sp,
            lineHeight = 36.sp,
            letterSpacing = (-0.8).sp,
        )
        Text(
            "Movie Site.",
            color = Fg,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.SemiBold,
            fontSize = 34.sp,
            lineHeight = 36.sp,
            letterSpacing = (-0.8).sp,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Discover, Access, Track.",
            color = Amber200,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.4.sp,
        )
        Spacer(Modifier.height(22.dp))

        Text("Sign in", color = Fg, fontSize = 20.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(4.dp))
        Text(
            "Sync your library across devices, or continue as a guest.",
            color = Muted,
            fontSize = 14.sp,
        )
        Spacer(Modifier.height(20.dp))

        PrimaryButton("Continue with Google", onGoogle)
        Spacer(Modifier.height(12.dp))
        SecondaryButton("Continue as guest", onGuest)

        notice?.let {
            Spacer(Modifier.height(14.dp))
            Text(it, color = Amber200, fontSize = 12.sp)
        }

        Spacer(Modifier.height(18.dp))
        Text(
            "Guest mode keeps nothing on our servers.",
            color = Muted.copy(alpha = 0.8f),
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun NameCard(
    name: String,
    isGuest: Boolean,
    busy: Boolean,
    onName: (String) -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit,
) {
    CardShellReal {
        Text(
            if (isGuest) "What should we call you?" else "Your display name",
            color = Fg,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            if (isGuest) "Only used on this device. Guests are not stored anywhere."
            else "Saved to your profile and shown on your lists.",
            color = Muted,
            fontSize = 14.sp,
        )
        Spacer(Modifier.height(20.dp))

        PillField(name, "Enter your name", onName)
        Spacer(Modifier.height(16.dp))
        PrimaryButton(
            if (busy) "Please wait\u2026" else "Continue",
            { if (name.isNotBlank()) onContinue() },
            enabled = name.isNotBlank() && !busy,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Back",
            color = Muted,
            fontSize = 13.sp,
            modifier = Modifier.clickable(onClick = onBack).padding(vertical = 6.dp),
        )
    }
}

@Composable
private fun PillField(value: String, placeholder: String, onChange: (String) -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(CircleShape)
            .background(InputBg)
            .border(1.dp, CardBorder, CircleShape)
            .padding(horizontal = 20.dp, vertical = 15.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (value.isEmpty()) {
            Text(placeholder, color = Muted.copy(alpha = 0.6f), fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = TextStyle(color = Fg, fontSize = 16.sp, fontWeight = FontWeight.Medium),
            cursorBrush = SolidColor(Accent),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun PrimaryButton(label: String, onClick: () -> Unit, enabled: Boolean = true) {
    Box(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(CircleShape)
            .background(Accent.copy(alpha = if (enabled) 1f else 0.5f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Text(
                label,
                color = Color(0xFF1A1305),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.3.sp,
            )
            Spacer(Modifier.width(8.dp))
            androidx.compose.material3.Icon(
                Icons.Filled.ArrowForward,
                contentDescription = null,
                tint = Color(0xFF1A1305),
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun SecondaryButton(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(CircleShape)
            .background(Color.Transparent)
            .border(1.dp, CardBorder, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Fg, fontSize = 16.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.3.sp)
    }
}