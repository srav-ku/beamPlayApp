package app.cinephile.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import app.cinephile.core.ui.theme.Beam
import app.cinephile.core.ui.theme.GeistMono
import app.cinephile.core.util.tmdbImageUrl

/**
 * Poster card — mirrors the website's `.latest-card` / `LatestCard`:
 * 2:3-ish poster (210/308), 12dp radius, bottom-up gradient, title overlaid
 * at the bottom in Geist Mono 14sp SemiBold and a star/rating/year meta line
 * in Geist Mono 11sp Medium, plus the 28dp glass bookmark button.
 */
@Composable
fun ContentCard(
    title: String,
    posterPath: String?,
    rating: Double?,
    year: Int?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp? = 128.dp,
    showBookmark: Boolean = true,
    onBookmarkClick: (() -> Unit)? = null,
) {
    val colors = Beam.colors
    val imageUrl = tmdbImageUrl(posterPath, "w342")

    Box(
        modifier = modifier
            .then(if (width != null) Modifier.width(width) else Modifier.fillMaxWidth())
            .clickable(onClick = onClick),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(210f / 308f)
                .clip(RoundedCornerShape(12.dp))
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
                    modifier = Modifier.align(Alignment.Center).padding(12.dp),
                )
            }

            // Gradient for text legibility: rgba(0,0,0,.92) -> .55 -> .12 -> 0
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

            if (showBookmark) {
                BookmarkButton(
                    size = 28.dp,
                    iconSize = 14.dp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 6.dp, end = 6.dp),
                    onClick = onBookmarkClick,
                )
            }

            Column(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, bottom = 10.dp),
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontFamily = GeistMono,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 17.5.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.size(4.dp))
                CardMetaRow(rating = rating, year = year, starSize = 11.dp, fontSize = 11.sp)
            }
        }
    }
}

/** The `★ 7.8 • 2026` line shared by both card types. */
@Composable
internal fun CardMetaRow(
    rating: Double?,
    year: Int?,
    starSize: Dp,
    fontSize: androidx.compose.ui.unit.TextUnit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.Star,
            contentDescription = null,
            tint = Beam.colors.amber300,
            modifier = Modifier.size(starSize),
        )
        Text(
            text = if (rating != null && rating > 0) rating.oneDecimal() else "N/A",
            color = Color(0xE6FFFFFF),
            fontFamily = GeistMono,
            fontSize = fontSize,
            fontWeight = FontWeight.Medium,
        )
        if (year != null && year > 0) {
            Text(
                text = "\u2022",
                color = Color(0x73FFFFFF),
                fontFamily = GeistMono,
                fontSize = fontSize,
            )
            Text(
                text = year.toString(),
                color = Color(0xE6FFFFFF),
                fontFamily = GeistMono,
                fontSize = fontSize,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

/** Circular glass bookmark button (`rgba(0,0,0,.5)` + 15% white border). */
@Composable
internal fun BookmarkButton(
    size: Dp,
    iconSize: Dp,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(0x80000000))
            .border(1.dp, Color(0x26FFFFFF), CircleShape)
            .clickable(enabled = onClick != null) { onClick?.invoke() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.BookmarkBorder,
            contentDescription = "Bookmark",
            tint = Color(0xD9FFFFFF),
            modifier = Modifier.size(iconSize),
        )
    }
}

/** Kotlin/common replacement for `toFixed(1)`. */
internal fun Double.oneDecimal(): String {
    val rounded = kotlin.math.round(this * 10.0) / 10.0
    val whole = rounded.toLong()
    val frac = kotlin.math.round((rounded - whole) * 10).toLong().let {
        if (it < 0) -it else it
    }
    return "$whole.$frac"
}
