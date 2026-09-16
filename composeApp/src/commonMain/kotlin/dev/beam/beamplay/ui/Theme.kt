package dev.beam.beamplay.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import dev.beam.beamplay.core.ui.theme.BeamAmber300
import dev.beam.beamplay.core.ui.theme.BeamBackground
import dev.beam.beamplay.core.ui.theme.BeamBorder
import dev.beam.beamplay.core.ui.theme.BeamBorderLight
import dev.beam.beamplay.core.ui.theme.BeamCard
import dev.beam.beamplay.core.ui.theme.BeamColorScheme
import dev.beam.beamplay.core.ui.theme.BeamForeground
import dev.beam.beamplay.core.ui.theme.BeamGreen
import dev.beam.beamplay.core.ui.theme.BeamMuted
import dev.beam.beamplay.core.ui.theme.BeamMutedForeground
import dev.beam.beamplay.core.ui.theme.BeamShapes
import dev.beam.beamplay.core.ui.theme.BeamTypography
import dev.beam.beamplay.core.ui.theme.LocalBeamColors
import dev.beam.beamplay.core.ui.theme.beamColors

/**
 * Legacy palette names kept so the existing screens keep compiling, now backed
 * by the exact Phase 2 tokens (`core/ui/theme/Colors.kt`) instead of the old
 * approximations.
 */
object BeamColors {
    val bg = BeamBackground
    val surface = BeamCard
    val card = BeamCard
    val cardElevated = Color(0xFF222226)
    val muted = BeamMuted
    val border = BeamBorder
    val borderLight = BeamBorderLight

    val primary = BeamForeground
    val onPrimary = BeamBackground

    val textPrimary = BeamForeground
    val textSecondary = BeamMutedForeground
    val textMuted = Color(0xFF71717A)

    val amber = BeamAmber300
    val green = BeamGreen
}

/**
 * Root theme. Delegates to the shared Phase 2 design system so the whole app
 * (old screens included) renders with Geist Mono / Inter, the exact dark
 * palette and the 26dp card radius.
 */
@Composable
fun BeamTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalBeamColors provides beamColors) {
        MaterialTheme(
            colorScheme = BeamColorScheme,
            typography = BeamTypography,
            shapes = BeamShapes,
            content = content,
        )
    }
}
