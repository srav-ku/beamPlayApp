package dev.beam.beamplay.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.beam.beamplay.core.ui.theme.Beam
import dev.beam.beamplay.core.ui.theme.GeistMono
import dev.beam.beamplay.core.ui.theme.PillShape

/**
 * Section header — mirrors the website's current `.section-header` block
 * (the later `BEAM HOME PAGE` rules, which override the older Inter styling):
 *
 * - title: Geist Mono 16sp, weight 400, `--foreground`
 * - tabs: pill container (`--card` fill, 1dp `--border`, 9999px radius, 2dp pad)
 *   with 28dp-tall tabs, Geist Mono 13sp Medium; selected tab inverts to
 *   `--foreground` fill with `--background` text
 * - scroll chevrons: 32dp outlined circles, desktop-only on the web
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    tabs: List<String>? = null,
    activeTab: String? = null,
    onTabChange: ((String) -> Unit)? = null,
    onScrollLeft: (() -> Unit)? = null,
    onScrollRight: (() -> Unit)? = null,
) {
    val colors = Beam.colors

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = title,
            color = colors.foreground,
            fontFamily = GeistMono,
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        if (tabs != null && activeTab != null && onTabChange != null) {
            Row(
                modifier = Modifier
                    .clip(PillShape)
                    .background(colors.card)
                    .border(1.dp, colors.border, PillShape)
                    .padding(2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                tabs.forEach { tab ->
                    val selected = tab == activeTab
                    Box(
                        modifier = Modifier
                            .height(28.dp)
                            .clip(PillShape)
                            .background(if (selected) colors.foreground else androidx.compose.ui.graphics.Color.Transparent)
                            .clickable { onTabChange(tab) }
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = tab,
                            color = if (selected) colors.background else colors.mutedForeground,
                            fontFamily = GeistMono,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                        )
                    }
                }
            }
        }

        if (onScrollLeft != null || onScrollRight != null) {
            Row(
                modifier = Modifier.padding(start = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ScrollCircle(Icons.Filled.ChevronLeft, "Scroll left", onScrollLeft)
                ScrollCircle(Icons.Filled.ChevronRight, "Scroll right", onScrollRight)
            }
        }
    }
}

@Composable
private fun ScrollCircle(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: (() -> Unit)?,
) {
    val colors = Beam.colors

    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .border(1.dp, colors.border, CircleShape)
            .clickable(enabled = onClick != null) { onClick?.invoke() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = colors.foreground,
            modifier = Modifier.size(16.dp),
        )
    }
}
