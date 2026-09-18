package dev.beam.beamplay.core.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/** One entry in [BeamBottomNav]. */
data class BeamNavItem(
    val label: String,
    val icon: ImageVector,
)

/**
 * Icon-first floating dock.
 *
 * Deliberately has **no text labels**: the icon set is unambiguous (house /
 * film / tv / bookshelf), the active tab is communicated by a gliding pill
 * highlight plus a brighter, slightly larger icon, and every item still carries
 * a `contentDescription` for screen readers.
 */
@Composable
fun BeamBottomNav(
    items: List<BeamNavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pillShape = RoundedCornerShape(30.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .shadow(
                    elevation = 22.dp,
                    shape = pillShape,
                    clip = false,
                    ambientColor = Color(0xCC000000),
                    spotColor = Color(0xCC000000),
                )
                .clip(pillShape)
                .background(Color(0xFF121216))
                .border(1.dp, Color(0x14FFFFFF), pillShape),
        ) {
            val itemWidth = maxWidth / items.size

            if (selectedIndex in items.indices) {
                val highlightX by animateDpAsState(
                    targetValue = itemWidth * selectedIndex,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                    label = "beam-nav-highlight",
                )


            }

            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items.forEachIndexed { index, item ->
                    val active = index == selectedIndex
                    val interactionSource = remember { MutableInteractionSource() }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null,
                            ) { onSelect(index) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = if (active) Color(0xFFFCFCFC) else Color(0xFF7C7C86),
                            modifier = Modifier.size(if (active) 25.dp else 22.dp),
                        )
                    }
                }
            }
        }
    }
}
