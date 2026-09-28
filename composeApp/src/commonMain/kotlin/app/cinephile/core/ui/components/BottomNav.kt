package app.cinephile.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import app.cinephile.core.ui.theme.Beam

/** One entry in [BeamBottomNav]. */
data class BeamNavItem(
    val label: String,
    val icon: ImageVector,
)

/**
 * True Glassmorphism Floating Island (Design 1 Style).
 *
 * Features:
 * - Pure frosted glass capsule floating above content
 * - Extremely clean icon-only layout without heavy oval container background behind active icon
 * - Active icon glows in bright gold/amber with smooth scaling
 * - Inactive icons are translucent white/grey outline vectors
 * - Translucent hairline border highlight with ambient drop shadow
 */
@Composable
fun BeamBottomNav(
    items: List<BeamNavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Beam.colors

    // Dark solid surface layer background so content doesn't bleed/merge through
    val glassBg = Color(0xF7101014)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 28.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .height(60.dp)
                .shadow(
                    elevation = 20.dp,
                    shape = RoundedCornerShape(32.dp),
                    clip = false,
                    ambientColor = Color(0xAA000000),
                    spotColor = Color(0xDD000000),
                )
                .clip(RoundedCornerShape(32.dp))
                .background(glassBg)
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0x38FFFFFF),
                            Color(0x0CFFFFFF),
                        ),
                    ),
                    shape = RoundedCornerShape(32.dp),
                )
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEachIndexed { index, item ->
                val active = index == selectedIndex
                val interactionSource = remember { MutableInteractionSource() }

                // Active icon transitions to pure amber, inactive to muted translucent white
                val iconColor by animateColorAsState(
                    targetValue = if (active) colors.amber500 else Color(0x73FFFFFF),
                    animationSpec = spring(stiffness = Spring.StiffnessLow),
                )

                // Spring animated scale on active icon
                val iconScale by animateFloatAsState(
                    targetValue = if (active) 1.2f else 1.0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow,
                    ),
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                        ) { onSelect(index) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = iconColor,
                        modifier = Modifier
                            .size(24.dp)
                            .scale(iconScale),
                    )
                }
            }
        }
    }
}


