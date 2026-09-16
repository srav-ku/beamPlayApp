package dev.beam.beamplay.core.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.beam.beamplay.core.ui.theme.Beam

/**
 * Shimmering placeholder matching the website's `.skeleton` class
 * (animated linear gradient between [Beam]'s skeleton tokens).
 */
@Composable
fun LoadingSkeleton(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(26.dp),
) {
    val colors = Beam.colors

    val transition = rememberInfiniteTransition(label = "beam-skeleton")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "beam-skeleton-shimmer",
    )

    val sweep = -320f + progress * 960f
    val brush = Brush.linearGradient(
        colors = listOf(colors.skeletonBase, colors.skeletonHighlight, colors.skeletonBase),
        start = Offset(x = sweep, y = 0f),
        end = Offset(x = sweep + 320f, y = 0f),
    )

    Box(modifier = modifier.background(brush = brush, shape = shape))
}

/** Poster-shaped skeleton (2:3), the most common placeholder on the Home screen. */
@Composable
fun PosterSkeleton(
    width: Dp = 132.dp,
    modifier: Modifier = Modifier,
) {
    LoadingSkeleton(
        modifier = modifier
            .width(width)
            .height(width * 1.5f),
        shape = RoundedCornerShape(12.dp),
    )
}

/** Full-width text-line skeleton. */
@Composable
fun LineSkeleton(
    height: Dp = 14.dp,
    modifier: Modifier = Modifier,
) {
    LoadingSkeleton(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        shape = RoundedCornerShape(6.dp),
    )
}
