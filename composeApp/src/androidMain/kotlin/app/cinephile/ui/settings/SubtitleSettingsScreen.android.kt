package app.cinephile.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import beamplay.composeapp.generated.resources.Res
import beamplay.composeapp.generated.resources.preview_frame
import org.jetbrains.compose.resources.painterResource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.cinephile.ui.player.SubtitlePrefs
import app.cinephile.ui.player.SubtitleStyle

@Composable
actual fun BeamSubtitleSettingsScreen() {
    val ctx = LocalContext.current.applicationContext
    var style by remember { mutableStateOf(SubtitlePrefs.load(ctx)) }
    LaunchedEffect(style) { SubtitlePrefs.save(ctx, style) }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0B0D))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(10.dp))
        Text("Subtitle settings", color = Color.White, fontSize = 20.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            "Applies to every video until you change it again.",
            color = Color(0xFF8A8A90),
            fontSize = 12.sp,
        )
        Spacer(Modifier.height(14.dp))

        // Live preview: a real 16:9 frame with the current style applied.
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(12.dp)),
        ) {
            Image(
                painter = painterResource(Res.drawable.preview_frame),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp)
                    .padding(bottom = (style.bottomOffsetPx / 3).dp),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "This is exactly how your subtitles will look on screen.",
                    color = Color(style.textColor),
                    fontSize = style.fontPx.sp,
                    fontFamily = when (style.fontFamily) {
                        "Serif" -> FontFamily.Serif
                        "Monospace" -> FontFamily.Monospace
                        "Sans-serif" -> FontFamily.SansSerif
                        else -> FontFamily.Default
                    },
                    textAlign = TextAlign.Center,
                    style = TextStyle(
                        shadow = when (style.bgStyle) {
                            "Shadow" -> Shadow(Color.Black, Offset(3f, 3f), 6f)
                            "Outline" -> Shadow(Color.Black, Offset(0f, 0f), 6f)
                            else -> null
                        },
                    ),
                    modifier = Modifier
                        .background(
                            if (style.bgStyle == "Box") {
                                Color(style.bgColor).copy(alpha = style.bgOpacity / 100f)
                            } else {
                                Color.Transparent
                            },
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
        Spacer(Modifier.height(18.dp))

        SettingSlider("FONT SIZE", style.fontPx.toFloat(), 14f, 48f, style.fontPx.toString() + " px") {
            style = style.copy(fontPx = it.toInt())
        }
        SettingSlider("BACKGROUND OPACITY", style.bgOpacity.toFloat(), 0f, 100f, style.bgOpacity.toString() + "%") {
            style = style.copy(bgOpacity = it.toInt())
        }
        SettingSlider("VERTICAL POSITION", style.bottomOffsetPx.toFloat(), 0f, 400f, style.bottomOffsetPx.toString() + " px") {
            style = style.copy(bottomOffsetPx = it.toInt())
        }

        SettingChips("BACKGROUND STYLE", listOf("None", "Outline", "Box", "Shadow"), style.bgStyle) {
            style = style.copy(bgStyle = it)
        }
        SettingChips("FONT FAMILY", listOf("Default", "Serif", "Monospace", "Sans-serif"), style.fontFamily) {
            style = style.copy(fontFamily = it)
        }
        SettingColorRow("TEXT COLOR", listOf(-0x1, -0x100, -0x10000, -0xFF0100), style.textColor) {
            style = style.copy(textColor = it)
        }
        SettingColorRow("BACKGROUND COLOR", listOf(-0x1000000, -0xCCCCCD, -0x7F7F80, -0xFFFF80), style.bgColor) {
            style = style.copy(bgColor = it)
        }

        Spacer(Modifier.height(10.dp))
        Box(
            Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0x1FFFFFFF))
                .clickable { style = SubtitleStyle() }
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Text("Reset to defaults", color = Color(0xFFEDEDED), fontSize = 13.sp)
        }
        Spacer(Modifier.height(30.dp))
    }
}

@Composable
private fun SettingLabel(text: String, trailing: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 14.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, color = Color(0xFFEDEDED), fontSize = 12.sp)
        Text(trailing, color = Color(0xFF8A8A90), fontSize = 12.sp)
    }
}

@Composable
private fun SettingSlider(label: String, value: Float, from: Float, to: Float, trailing: String, onChange: (Float) -> Unit) {
    SettingLabel(label, trailing)
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

@Composable
private fun SettingChips(label: String, options: List<String>, selected: String, onPick: (String) -> Unit) {
    SettingLabel(label, selected)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            val on = option == selected
            Box(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (on) Color(0xFFF5A623) else Color(0x1FFFFFFF))
                    .clickable { onPick(option) }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text(
                    option,
                    color = if (on) Color(0xFF101014) else Color(0xFFEDEDED),
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@Composable
private fun SettingColorRow(label: String, colors: List<Int>, selected: Int, onPick: (Int) -> Unit) {
    SettingLabel(label, "")
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        colors.forEach { value ->
            val on = value == selected
            Box(
                Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(value))
                    .clickable { onPick(value) }
                    .padding(3.dp),
            ) {
                if (on) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onPick(value) },
                    )
                }
            }
            Spacer(Modifier.width(0.dp))
        }
    }
}