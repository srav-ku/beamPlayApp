package dev.beam.beamplay.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Always dark — the website is dark-only, so there is no light branch. */
val LocalBeamColors = staticCompositionLocalOf { BeamColors() }

data class BeamColors(
    val background: Color = BeamBackground,
    val foreground: Color = BeamForeground,
    val card: Color = BeamCard,
    val popover: Color = BeamPopover,
    val muted: Color = BeamMuted,
    val mutedForeground: Color = BeamMutedForeground,
    val border: Color = BeamBorder,
    val borderLight: Color = BeamBorderLight,
    val input: Color = BeamInput,
    val amber300: Color = BeamAmber300,
    val amber400: Color = BeamAmber400,
    val amber500: Color = BeamAmber500,
    val amber600: Color = BeamAmber600,
    val red: Color = BeamRed,
    val green: Color = BeamGreen,
    val blue: Color = BeamBlue,
    val skeletonBase: Color = SkeletonBase,
    val skeletonHighlight: Color = SkeletonHighlight,
    val overlay92: Color = Overlay92,
    val overlay55: Color = Overlay55,
    val overlay12: Color = Overlay12,
    val overlay75: Color = Overlay75,
    val overlay40: Color = Overlay40,
)

/** Single shared instance — the palette never changes at runtime. */
val beamColors = BeamColors()

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

/** Ergonomic accessor: `Beam.colors.foreground`. */
object Beam {
    val colors: BeamColors
        @Composable get() = LocalBeamColors.current
}
