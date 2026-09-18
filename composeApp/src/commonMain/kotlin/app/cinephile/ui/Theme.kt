package app.cinephile.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import app.cinephile.core.ui.theme.BeamAmber300
import app.cinephile.core.ui.theme.BeamBackground
import app.cinephile.core.ui.theme.BeamBorder
import app.cinephile.core.ui.theme.BeamBorderLight
import app.cinephile.core.ui.theme.BeamCard
import app.cinephile.core.ui.theme.BeamColorScheme
import app.cinephile.core.ui.theme.BeamForeground
import app.cinephile.core.ui.theme.BeamGreen
import app.cinephile.core.ui.theme.BeamMuted
import app.cinephile.core.ui.theme.BeamMutedForeground
import app.cinephile.core.ui.theme.BeamShapes
import app.cinephile.core.ui.theme.BeamTypography
import app.cinephile.core.ui.theme.LocalBeamColors
import app.cinephile.core.ui.theme.beamColors

/**
 * Legacy palette names kept so the existing screens keep compiling, now backed
 * by the exact Phase 2 tokens (`core/ui/theme/Colors.kt`) instead of the old
 * approximations.
 */
object BeamColors {
    val bg = BeamBackground
    val surface = BeamCard
    val card = BeamCard
    val cardElevated = Color(0xFF201E1B)   // warm raised surface
    val muted = BeamMuted
    val border = BeamBorder
    val borderLight = BeamBorderLight

    val primary = Color(0xFFE8A13A)        // Cinephile amber; was near-white
    val onPrimary = Color(0xFF161310)

    val textPrimary = BeamForeground
    val textSecondary = BeamMutedForeground
    val textMuted = Color(0xFF8A8378)

    val amber = Color(0xFFE8A13A)
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
