package dev.beam.beamplay.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
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
import dev.beam.beamplay.core.ui.theme.GeistMono
import dev.beam.beamplay.data.Api
import dev.beam.beamplay.data.MediaItem

/**
 * Filter Pill Row (e.g. [Movie | TV Show])
 * Matching website: Rounded container with active white pill and subtle border.
 */
@Composable
fun FilterPillBar(
    tabs: List<String>,
    selectedTab: String,
    onTabSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(9999.dp))
            .background(Color(0xFF1E1E22))
            .border(1.dp, Color(0xFF2E2E34), RoundedCornerShape(9999.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tabs.forEach { tab ->
            val isSelected = tab == selectedTab
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(9999.dp))
                    .background(if (isSelected) Color(0xFFFCFCFC) else Color.Transparent)
                    .clickable { onTabSelected(tab) }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = tab,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontFamily = GeistMono,
                    color = if (isSelected) Color(0xFF0A0A0A) else Color(0xFFA1A1AA),
                )
            }
        }
    }
}

/**
 * Exact Trending Card:
 * - 16:9 widescreen poster
 * - Rounded corners (20.dp)
 * - Dark gradient overlay at bottom
 * - Top-right circular bookmark button
 * - Bottom-left: Title, Star Rating, Year (Monospace)
 * - Bottom-right: Giant rank numeral ("1", "2", "3"...)
 */
@Composable
fun TrendingCard(
    item: MediaItem,
    rank: Int,
    onClick: () -> Unit,
    onBookmark: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val imagePath = item.backdrop_path ?: item.poster_path
    val imageUrl = Api.backdropUrl(imagePath, "w780") ?: Api.posterUrl(imagePath, "w500")

    Box(
        modifier = modifier
            .width(260.dp)
            .aspectRatio(16f / 10f)
            .clip(RoundedCornerShape(20.dp))
            .background(BeamColors.card)
            .clickable(onClick = onClick),
    ) {
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(
                Modifier.fillMaxSize().background(BeamColors.card),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    item.title,
                    fontFamily = GeistMono,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = BeamColors.textSecondary,
                )
            }
        }

        // Multi-stop gradient overlay for readable text
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Transparent,
                        0.35f to Color.Transparent,
                        0.70f to Color(0x99000000),
                        1.0f to Color(0xF5000000),
                    ),
                ),
        )

        // Radial darkening at bottom-right for rank numeral contrast
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color.Transparent, Color(0xAA000000)),
                        radius = 280f,
                    ),
                ),
        )

        // Bookmark pill button at top right
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp)
                .size(34.dp)
                .clip(CircleShape)
                .background(Color(0x73000000))
                .border(1.dp, Color(0x2BFFFFFF), CircleShape)
                .clickable { onBookmark?.invoke() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.BookmarkBorder,
                contentDescription = "Bookmark",
                tint = Color.White,
                modifier = Modifier.size(16.dp),
            )
        }

        // Giant Rank Numeral at Bottom-Right
        Text(
            text = "$rank",
            fontFamily = GeistMono,
            fontSize = 72.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            lineHeight = 72.sp,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 2.dp, y = 6.dp)
                .padding(end = 4.dp),
        )

        // Title + Rating + Year at Bottom-Left
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 14.dp, bottom = 12.dp, end = 74.dp),
        ) {
            Text(
                text = item.title,
                fontFamily = GeistMono,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = null,
                    tint = BeamColors.amber,
                    modifier = Modifier.size(12.dp),
                )
                Text(
                    text = item.tmdb_rating?.takeIf { it > 0 }?.let { "%.1f".format(it) } ?: "N/A",
                    fontFamily = GeistMono,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.9f),
                )
                item.year?.let { y ->
                    Text("•", color = Color.White.copy(alpha = 0.45f), fontSize = 11.sp)
                    Text(
                        text = "$y",
                        fontFamily = GeistMono,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.85f),
                    )
                }
            }
        }
    }
}

/**
 * Exact Vertical Poster Card (Latest / Top Rated / Popular):
 * - 2:3 vertical aspect ratio
 * - Rounded corners (16.dp)
 * - Gradient overlay
 * - Top bookmark button
 * - Overlay text: 2-line title + star + year
 */
@Composable
fun LatestPosterCard(
    item: MediaItem,
    width: Dp = 138.dp,
    onClick: () -> Unit,
    onBookmark: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val imageUrl = Api.posterUrl(item.poster_path, "w500")

    Box(
        modifier = modifier
            .width(width)
            .aspectRatio(2f / 3f)
            .clip(RoundedCornerShape(16.dp))
            .background(BeamColors.card)
            .clickable(onClick = onClick),
    ) {
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(
                Modifier.fillMaxSize().background(BeamColors.card),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    item.title,
                    fontFamily = GeistMono,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BeamColors.textSecondary,
                    modifier = Modifier.padding(8.dp),
                )
            }
        }

        // Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Transparent,
                        0.40f to Color.Transparent,
                        0.75f to Color(0xA6000000),
                        1.0f to Color(0xF0000000),
                    ),
                ),
        )

        // Bookmark button
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .size(30.dp)
                .clip(CircleShape)
                .background(Color(0x66000000))
                .border(1.dp, Color(0x24FFFFFF), CircleShape)
                .clickable { onBookmark?.invoke() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.BookmarkBorder,
                contentDescription = "Bookmark",
                tint = Color.White,
                modifier = Modifier.size(15.dp),
            )
        }

        // Title + Rating at Bottom
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 10.dp, vertical = 10.dp),
        ) {
            Text(
                text = item.title,
                fontFamily = GeistMono,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp,
            )
            Spacer(Modifier.height(3.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = null,
                    tint = BeamColors.amber,
                    modifier = Modifier.size(11.dp),
                )
                Text(
                    text = item.tmdb_rating?.takeIf { it > 0 }?.let { "%.1f".format(it) } ?: "N/A",
                    fontFamily = GeistMono,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.9f),
                )
                item.year?.let { y ->
                    Text("•", color = Color.White.copy(alpha = 0.45f), fontSize = 10.sp)
                    Text(
                        text = "$y",
                        fontFamily = GeistMono,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.8f),
                    )
                }
            }
        }
    }
}

/**
 * Continue Watching Card:
 * - 16:9 thumbnail
 * - Progress bar at bottom
 * - Title + percent label + Play icon
 * - Dismiss 'X' button
 */
@Composable
fun ContinueWatchingCard(
    item: MediaItem,
    progressFraction: Float,
    subtitle: String,
    onClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val imageUrl = Api.backdropUrl(item.backdrop_path ?: item.poster_path, "w500")

    Box(
        modifier = modifier
            .width(250.dp)
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(16.dp))
            .background(BeamColors.card)
            .clickable(onClick = onClick),
    ) {
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(Modifier.fillMaxSize().background(BeamColors.card))
        }

        // Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Transparent,
                        0.40f to Color(0x33000000),
                        1.0f to Color(0xEE000000),
                    ),
                ),
        )

        // Dismiss X button
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .size(26.dp)
                .clip(CircleShape)
                .background(Color(0x99000000))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Close,
                contentDescription = "Remove",
                tint = Color.White,
                modifier = Modifier.size(14.dp),
            )
        }

        // Title and play icon at bottom
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                    text = item.title,
                    fontFamily = GeistMono,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    fontFamily = GeistMono,
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.7f),
                )
            }
            Icon(
                Icons.Filled.PlayArrow,
                contentDescription = "Play",
                tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
        }

        // Progress bar at bottom edge
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .height(3.dp)
                .background(Color.White.copy(alpha = 0.2f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progressFraction.coerceIn(0f, 1f))
                    .background(Color.White),
            )
        }
    }
}

/**
 * Section Container with header and horizontal list
 */
@Composable
fun SectionContainer(
    title: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                fontFamily = GeistMono,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = BeamColors.textPrimary,
            )
            trailing?.invoke()
        }
        Spacer(Modifier.height(8.dp))
        content()
    }
}

@Composable
fun LoadingBlock(visible: Boolean) {
    if (!visible) return
    Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = BeamColors.primary, strokeWidth = 2.dp)
    }
}

@Composable
fun ErrorBlock(message: String) {
    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(
            message,
            fontFamily = GeistMono,
            color = BeamColors.textSecondary,
            fontSize = 13.sp,
        )
    }
}

/**
 * Continue Watching card.
 *
 * Same footprint and corners as the trending cards, with the bottom gradient
 * kept for the title block. The offline store has no artwork, so the thumbnail
 * is a neutral placeholder rather than a broken image; the accent progress bar
 * shows exactly how far the viewer got.
 */
@Composable
fun ContinueWatchingResumeCard(
    item: ContinueItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fraction = if (item.durationMs > 0L) {
        (item.positionMs.toFloat() / item.durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val remaining = ((item.durationMs - item.positionMs) / 1000L).coerceAtLeast(0L)
    val remainingLabel = when {
        item.durationMs <= 0L -> "Resume"
        remaining >= 3600L -> "${remaining / 3600L}h ${(remaining % 3600L) / 60L}m left"
        remaining >= 60L -> "${remaining / 60L} min left"
        else -> "less than a minute left"
    }

    Box(
        modifier = modifier
            .width(250.dp)
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF17171C))
            .clickable(onClick = onClick),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFF1C1C22), Color(0xFF131318)))),
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Transparent,
                        0.45f to Color(0x33000000),
                        1.0f to Color(0xEE000000),
                    ),
                ),
        )

        Box(
            Modifier
                .align(Alignment.Center)
                .size(42.dp)
                .clip(CircleShape)
                .background(Color(0x99000000)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.PlayArrow,
                contentDescription = "Resume",
                tint = Color.White,
                modifier = Modifier.size(22.dp),
            )
        }

        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(start = 14.dp, end = 14.dp, bottom = 12.dp),
        ) {
            Text(
                text = item.title,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(0.9f),
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = remainingLabel,
                color = Color(0xFFB6B6C0),
                fontFamily = GeistMono,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Box(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .height(3.dp)
                .background(Color(0x33FFFFFF)),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .background(Color(0xFFF5A623)),
            )
        }
    }
}