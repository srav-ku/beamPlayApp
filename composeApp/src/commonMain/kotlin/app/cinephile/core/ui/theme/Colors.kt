package app.cinephile.core.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

/* ------------------------------------------------------------------ */
/*  Exact values from beamPlay-web/src/app/globals.css -> html.dark    */
/* ------------------------------------------------------------------ */

val BeamBackground = Color(0xFF0E0E0D)
val BeamForeground = Color(0xFFF5F2EA)
val BeamCard = Color(0xFF161513)
val BeamPopover = Color(0xFF1A1815)
val BeamMuted = Color(0xFF201E1B)
val BeamMutedForeground = Color(0xFFB8AFA4)
val BeamBorder = Color(0xFF2E2A26)
val BeamBorderLight = Color(0x1AFFFFFF) // 8% white
val BeamInput = Color(0xFF2E2A26)

val BeamAmber300 = Color(0xFFF5C77E) // TMDB star
val BeamAmber400 = Color(0xFFF0B457) // accent hover
val BeamAmber500 = Color(0xFFE8A13A) // active bookmark
val BeamAmber600 = Color(0xFFC9871F)

val BeamRed = Color(0xFFC4503A) // destructive / report
val BeamGreen = Color(0xFF6B8E7F) // watched check
val BeamBlue = Color(0xFF7D93A8) // info badges

// Skeleton shimmer base + highlight
val SkeletonBase = Color(0xFF161513)
val SkeletonHighlight = Color(0xFF201E1B)

// Translucent overlays matching the website's gradient stops
val Overlay92 = Color(0xEB000000) // rgba(0,0,0,0.92)
val Overlay55 = Color(0x8C000000) // rgba(0,0,0,0.55)
val Overlay12 = Color(0x1F000000) // rgba(0,0,0,0.12)
val Overlay75 = Color(0xBF000000) // rgba(0,0,0,0.75)
val Overlay40 = Color(0x66000000) // rgba(0,0,0,0.40)

val BeamColorScheme = darkColorScheme(
    // Amber is the brand. This was mapped to near-white (Cinephile's own --primary),
    // which is why every primary control rendered white instead of the accent.
    primary = BeamAmber500,
    onPrimary = Color(0xFF161310),
    secondary = BeamMuted,
    onSecondary = BeamForeground,
    tertiary = BeamAmber400,
    onTertiary = Color(0xFF161310),
    background = BeamBackground,
    onBackground = BeamForeground,
    surface = BeamCard,
    onSurface = BeamForeground,
    surfaceVariant = BeamMuted,
    onSurfaceVariant = BeamMutedForeground,
    surfaceTint = BeamAmber500,
    inverseSurface = BeamMutedForeground,
    inverseOnSurface = BeamBackground,
    error = BeamRed,
    onError = Color.White,
    outline = BeamBorder,
    outlineVariant = BeamBorderLight,
    scrim = Color.Black,
)

// --- Cinephile-style accent aliases (phonofilm.net tokens) ---
val BeamAccent = Color(0xFFE8A13A)
val BeamAccentPressed = Color(0xFFC9871F)
val BeamAccentSoft = Color(0x1FE8A13A)
val BeamSurfaceRaised = Color(0xFF1A1815)
val BeamSurfaceSunken = Color(0xFF121110)
