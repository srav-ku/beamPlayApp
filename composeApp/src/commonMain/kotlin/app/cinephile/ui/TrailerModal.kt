package app.cinephile.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.cinephile.core.ui.theme.Beam
import app.cinephile.core.ui.theme.Fraunces
import app.cinephile.core.ui.theme.PillShape

/** The YouTube embed itself. Android renders it in a WebView. */
@Composable
expect fun PlatformYouTubeEmbed(videoKey: String, modifier: Modifier)

/**
 * Full-screen trailer overlay, per the Cinephile spec: 90% black backdrop, a
 * 20dp-rounded 16:9 player, close button top-right, and the trailer name above
 * the title underneath. Closes on back.
 */
@Composable
fun TrailerModal(
    videoTitle: String,
    videoKey: String,
    title: String,
    onClose: () -> Unit,
) {
    val colors = Beam.colors
    PlatformBackHandler(enabled = true) { onClose() }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xE6000000))
            .clickable(onClick = onClose),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black)
                    .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(20.dp)),
            ) {
                PlatformYouTubeEmbed(videoKey, Modifier.fillMaxSize())
            }

            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = videoTitle.uppercase(),
                        color = colors.mutedForeground,
                        fontSize = 11.sp,
                        letterSpacing = 0.8.sp,
                        maxLines = 1,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = title,
                        color = colors.foreground,
                        fontFamily = Fraunces,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                    )
                }
            }
        }

        Box(
            Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(16.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0x1AFFFFFF))
                .border(1.dp, Color(0x26FFFFFF), CircleShape)
                .clickable(onClick = onClose),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Close trailer",
                tint = Color.White.copy(alpha = 0.75f),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/** Small outline chip used for the Trailer / export-style actions. */
@Composable
internal fun GhostChip(label: String, onClick: () -> Unit) {
    val colors = Beam.colors
    Row(
        Modifier
            .clip(PillShape)
            .border(1.dp, colors.border, PillShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = label,
            color = colors.foreground,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}
