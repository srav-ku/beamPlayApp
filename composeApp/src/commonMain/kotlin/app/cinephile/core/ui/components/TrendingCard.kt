package app.cinephile.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import app.cinephile.core.ui.theme.Beam
import app.cinephile.core.ui.theme.GeistMono
import app.cinephile.core.util.tmdbImageUrl
import kotlin.math.sqrt

/**
 * Trending card — mirrors the website's `.trending-card` / `TrendingCard`:
 * 16:10 backdrop, 14dp radius, bottom-up gradient plus a radial darkening in
 * the bottom-right corner, a 100sp Geist Mono Black rank numeral anchored at
 * the bottom-right, and the title/meta block at the bottom-left.
 *
 * Width is supplied by the caller via [modifier] (the site uses `82vw` capped
 * at 360px on phones).
 */
@Composable
fun BeamTrendingCard(
    title: String,
    backdropPath: String?,
    posterPath: String?,
    rating: Double?,
    year: Int?,
    rank: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Beam.colors
    val imageUrl = tmdbImageUrl(backdropPath ?: posterPath, "w500")

    Box(modifier = modifier.clickable(onClick = onClick)) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 10f)
                .clip(RoundedCornerShape(14.dp))
                .background(colors.card),
        ) {
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Text(
                    text = title,
                    color = colors.mutedForeground,
                    fontFamily = GeistMono,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.Center).padding(16.dp),
                )
            }

            // Linear gradient for legibility.
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.00f to colors.overlay92,
                            0.22f to colors.overlay55,
                            0.50f to colors.overlay12,
                            0.70f to Color.Transparent,
                        ),
                    ),
            )

            // Localized radial darkening anchored at the bottom-right corner so
            // the rank numeral stays readable over bright artwork.
            Box(
                Modifier
                    .fillMaxSize()
                    .drawBehind {
                        val radius = sqrt(size.width * size.width + size.height * size.height)
                        drawRect(
                            brush = Brush.radialGradient(
                                colorStops = arrayOf(
                                    0.00f to Color(0xBF000000),
                                    0.25f to Color(0x66000000),
                                    0.50f to Color(0x00000000),
                                ),
                                center = Offset(size.width, size.height),
                                radius = radius,
                            ),
                        )
                    },
            )

            // Rank numeral: Geist Mono Black 100sp, white, bottom-right.
            Text(
                text = rank.toString(),
                color = Color.White,
                fontFamily = GeistMono,
                fontSize = 100.sp,
                lineHeight = 100.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-5).sp,
                maxLines = 1,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 4.dp, y = 12.dp),
            )

            BookmarkButton(
                size = 32.dp,
                iconSize = 15.dp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 10.dp, end = 10.dp),
            )

            Column(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 90.dp, bottom = 12.dp),
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontFamily = GeistMono,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Box(Modifier.padding(top = 3.dp)) {
                    CardMetaRow(rating = rating, year = year, starSize = 12.dp, fontSize = 11.5.sp)
                }
            }
        }
    }
}
