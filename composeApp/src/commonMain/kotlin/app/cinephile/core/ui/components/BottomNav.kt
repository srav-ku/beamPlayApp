package app.cinephile.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.cinephile.core.ui.theme.Beam
import app.cinephile.core.ui.theme.GeistMono

/** One entry in [BeamBottomNav]. */
data class BeamNavItem(
    val label: String,
    val icon: ImageVector,
)

/**
 * Floating bottom navigation - a dark glass panel hovering above the content,
 * not a full-width bar attached to the edge.
 *
 * Style: nearly full-width rounded panel (18dp side margins, 10dp above the
 * system navigation area, 24dp corners), near-black translucent fill, a single
 * hairline border and a soft exterior shadow.
 *
 * Only the ACTIVE item gets a small pill behind its ICON - the label always sits
 * outside the pill. Colours come from the app theme, so the active state is the
 * Cinephile amber rather than the reference screenshot's navy/periwinkle.
 */
@Composable
fun BeamBottomNav(
    items: List<BeamNavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Beam.colors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 18.dp, end = 18.dp, bottom = 10.dp)
            .shadow(
                elevation = 18.dp,
                shape = RoundedCornerShape(24.dp),
                clip = false,
                ambientColor = Color(0xB3000000),
                spotColor = Color(0xB3000000),
            )
            .clip(RoundedCornerShape(24.dp))
            .background(colors.background.copy(alpha = 0.94f))
            .border(1.dp, colors.border, RoundedCornerShape(24.dp))
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEachIndexed { index, item ->
            val active = index == selectedIndex
            val interactionSource = remember { MutableInteractionSource() }
            val tint = if (active) colors.amber500 else colors.mutedForeground

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                    ) { onSelect(index) }
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Pill behind the icon only - never behind the label.
                Box(
                    Modifier
                        .height(28.dp)
                        .width(if (active) 54.dp else 28.dp)
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (active) colors.amber500.copy(alpha = 0.18f) else Color.Transparent,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = tint,
                        modifier = Modifier.size(21.dp),
                    )
                }

                Spacer(Modifier.height(3.dp))

                Text(
                    text = item.label,
                    color = tint,
                    fontFamily = GeistMono,
                    fontSize = 9.sp,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1,
                )
            }
        }
    }
}
