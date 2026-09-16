package dev.beam.beamplay.core.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

/* ------------------------------------------------------------------ */
/*  Exact values from beamPlay-web/src/app/globals.css -> html.dark    */
/* ------------------------------------------------------------------ */

val BeamBackground = Color(0xFF0A0A0A)
val BeamForeground = Color(0xFFFCFCFC)
val BeamCard = Color(0xFF1C1C1E)
val BeamPopover = Color(0xFF1C1C1E)
val BeamMuted = Color(0xFF2A2A2E)
val BeamMutedForeground = Color(0xFFA1A1AA)
val BeamBorder = Color(0xFF48484F)
val BeamBorderLight = Color(0x14FFFFFF) // 8% white
val BeamInput = Color(0xFF48484F)

val BeamAmber300 = Color(0xFFFCD34D) // TMDB star
val BeamAmber400 = Color(0xFFFBBF24) // accent hover
val BeamAmber500 = Color(0xFFF59E0B) // active bookmark
val BeamAmber600 = Color(0xFFD97706)

val BeamRed = Color(0xFFEF4444) // destructive / report
val BeamGreen = Color(0xFF22C55E) // watched check
val BeamBlue = Color(0xFF3B82F6) // info badges

// Skeleton shimmer base + highlight
val SkeletonBase = Color(0xFF1A1A1F)
val SkeletonHighlight = Color(0xFF232328)

// Translucent overlays matching the website's gradient stops
val Overlay92 = Color(0xEB000000) // rgba(0,0,0,0.92)
val Overlay55 = Color(0x8C000000) // rgba(0,0,0,0.55)
val Overlay12 = Color(0x1F000000) // rgba(0,0,0,0.12)
val Overlay75 = Color(0xBF000000) // rgba(0,0,0,0.75)
val Overlay40 = Color(0x66000000) // rgba(0,0,0,0.40)

val BeamColorScheme = darkColorScheme(
    primary = BeamForeground,
    onPrimary = BeamBackground,
    secondary = BeamCard,
    onSecondary = BeamForeground,
    tertiary = BeamAmber400,
    onTertiary = Color.Black,
    background = BeamBackground,
    onBackground = BeamForeground,
    surface = BeamCard,
    onSurface = BeamForeground,
    surfaceVariant = BeamMuted,
    onSurfaceVariant = BeamMutedForeground,
    surfaceTint = BeamForeground,
    inverseSurface = BeamMutedForeground,
    inverseOnSurface = BeamBackground,
    error = BeamRed,
    onError = Color.White,
    outline = BeamBorder,
    outlineVariant = BeamBorderLight,
    scrim = Color.Black,
)
