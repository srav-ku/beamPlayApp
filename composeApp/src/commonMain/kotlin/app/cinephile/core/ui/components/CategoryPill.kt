package app.cinephile.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.cinephile.core.ui.theme.Beam
import app.cinephile.core.ui.theme.GeistMono
import app.cinephile.core.ui.theme.PillShape

/**
 * Pill-shaped filter chip, matching `LinkSelector.tsx` (fully rounded,
 * foreground-on-active).
 */
@Composable
fun CategoryPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Beam.colors

    Text(
        text = if (label == "All") "Any" else label,
        color = if (selected) colors.background else colors.foreground,
        fontFamily = GeistMono,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        modifier = modifier
            .clip(PillShape)
            .background(if (selected) colors.foreground else Color.Transparent)
            .border(1.dp, if (selected) colors.foreground else colors.border, PillShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
    )
}

/** Non-interactive version for static labels / "no filter" chips. */
@Composable
fun CategoryPillStatic(
    label: String,
    modifier: Modifier = Modifier,
) {
    val colors = Beam.colors

    Text(
        text = label,
        color = colors.mutedForeground,
        fontFamily = GeistMono,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        modifier = modifier
            .clip(PillShape)
            .border(1.dp, colors.borderLight, PillShape)
            .padding(horizontal = 14.dp, vertical = 6.dp),
    )
}
