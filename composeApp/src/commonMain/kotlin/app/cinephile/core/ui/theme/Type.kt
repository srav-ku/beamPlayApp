package app.cinephile.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import beamplay.composeapp.generated.resources.Res
import beamplay.composeapp.generated.resources.fraunces
import beamplay.composeapp.generated.resources.fraunces_italic
import beamplay.composeapp.generated.resources.geist_mono_medium
import beamplay.composeapp.generated.resources.geist_mono_regular
import beamplay.composeapp.generated.resources.geist_mono_semibold
import beamplay.composeapp.generated.resources.inter_bold
import beamplay.composeapp.generated.resources.inter_extrabold
import beamplay.composeapp.generated.resources.inter_semibold
import org.jetbrains.compose.resources.Font

/**
 * Geist Mono — body / UI text (website `--font-main`, `--font-ui`).
 * Static instances so each weight renders exactly, no variable-axis guessing.
 */
val GeistMono: FontFamily
    @Composable get() = FontFamily(
        Font(Res.font.geist_mono_regular, FontWeight.Normal),
        Font(Res.font.geist_mono_medium, FontWeight.Medium),
        Font(Res.font.geist_mono_semibold, FontWeight.SemiBold),
    )

/** Fraunces - display serif for titles, from Google Fonts (variable: opsz/wght/SOFT/WONK). */
val Fraunces: FontFamily
    @Composable get() = FontFamily(
        Font(Res.font.fraunces, FontWeight.Normal),
        Font(Res.font.fraunces, FontWeight.Medium),
        Font(Res.font.fraunces, FontWeight.SemiBold),
        Font(Res.font.fraunces, FontWeight.Bold),
        Font(Res.font.fraunces_italic, FontWeight.Normal, FontStyle.Italic),
    )

/** Inter — headings (website `--font-heading`). */
val Inter: FontFamily
    @Composable get() = FontFamily(
        Font(Res.font.inter_semibold, FontWeight.SemiBold),
        Font(Res.font.inter_bold, FontWeight.Bold),
        Font(Res.font.inter_extrabold, FontWeight.ExtraBold),
    )

val BeamTypography: Typography
    @Composable get() = Typography(
        displayLarge = TextStyle(fontFamily = Inter, fontWeight = FontWeight.ExtraBold, fontSize = 32.sp, lineHeight = 40.sp),
        displayMedium = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 36.sp),
        headlineLarge = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 32.sp),
        headlineMedium = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 28.sp),
        titleLarge = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 24.sp),
        titleMedium = TextStyle(fontFamily = GeistMono, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
        titleSmall = TextStyle(fontFamily = GeistMono, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
        bodyLarge = TextStyle(fontFamily = GeistMono, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp),
        bodyMedium = TextStyle(fontFamily = GeistMono, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp),
        bodySmall = TextStyle(fontFamily = GeistMono, fontWeight = FontWeight.Normal, fontSize = 11.sp, lineHeight = 16.sp),
        labelLarge = TextStyle(fontFamily = GeistMono, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 18.sp),
        labelMedium = TextStyle(fontFamily = GeistMono, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp),
        labelSmall = TextStyle(fontFamily = GeistMono, fontWeight = FontWeight.Medium, fontSize = 10.sp, lineHeight = 14.sp),
    )

/** Trending-rank display size (website uses 100sp on desktop, 80sp on phone). */
const val TrendingRankSizeSp = 80
