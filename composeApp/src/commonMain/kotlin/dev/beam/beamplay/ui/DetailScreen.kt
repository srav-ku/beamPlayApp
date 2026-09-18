package dev.beam.beamplay.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import dev.beam.beamplay.core.model.TmdbCast
import dev.beam.beamplay.core.model.TmdbCredits
import dev.beam.beamplay.core.model.TmdbItem
import dev.beam.beamplay.core.network.servicesOrNull
import dev.beam.beamplay.core.ui.components.ContentCard
import dev.beam.beamplay.ui.player.PlayerSubtitle
import kotlinx.coroutines.launch
import dev.beam.beamplay.core.ui.theme.Beam
import dev.beam.beamplay.core.ui.theme.GeistMono
import dev.beam.beamplay.core.ui.theme.Inter
import dev.beam.beamplay.data.Api
import dev.beam.beamplay.data.MediaItem

/**
 * Title detail screen.
 *
 * Content mirrors the website's detail page (hero, meta row, actions,
 * storyline, Director, Actors, Studios, More like this, Seasons) but arranged
 * for a phone: the metadata and actions sit under the hero instead of on top
 * of it, and every section uses the app's own visual language.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BeamDetailScreen(
    item: MediaItem,
    onBack: () -> Unit,
    onOpenMedia: (MediaItem) -> Unit,
    onPlay: (String, List<PlayerSubtitle>, String) -> Unit,
) {
    val colors = Beam.colors
    val tmdbId = item.tmdb_id ?: 0L
    val isSeries = item.type == "series"

    var credits by remember { mutableStateOf<TmdbCredits?>(null) }
    var similar by remember { mutableStateOf<List<TmdbItem>>(emptyList()) }
    var studios by remember { mutableStateOf<List<String>>(emptyList()) }
    var videos by remember { mutableStateOf<List<dev.beam.beamplay.core.model.TmdbVideo>>(emptyList()) }
    var sources by remember { mutableStateOf<List<dev.beam.beamplay.core.model.Link>>(emptyList()) }
    var watchLater by remember { mutableStateOf(false) }
    var favorite by remember { mutableStateOf(false) }
    var watched by remember { mutableStateOf(false) }
    var resolving by remember { mutableStateOf(false) }
    var showSources by remember { mutableStateOf(false) }
    var showPremium by remember { mutableStateOf(false) }
    var showDownloads by remember { mutableStateOf(false) }

    // Back closes an open sheet first - never the whole screen underneath it.
    PlatformBackHandler(enabled = showSources || showDownloads || showPremium) {
        showSources = false
        showDownloads = false
        showPremium = false
    }
    var resolvingUrl by remember { mutableStateOf<String?>(null) }
    var downloadFiles by remember { mutableStateOf<List<dev.beam.beamplay.core.model.DownloadFile>>(emptyList()) }
    var downloadsLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(item.id) {
        val svc = servicesOrNull
        if (svc != null && tmdbId > 0) {
            runCatching {
                if (isSeries) {
                    val d = svc.tmdbApi.tvDetails(tmdbId)
                    credits = d.credits
                    similar = d.similar?.results ?: emptyList()
                    videos = d.videos?.results ?: emptyList()
                    studios = d.production_companies.map { it.name }
                } else {
                    val d = svc.tmdbApi.movieDetails(tmdbId)
                    credits = d.credits
                    similar = d.similar?.results ?: emptyList()
                    videos = d.videos?.results ?: emptyList()
                    studios = d.production_companies.map { it.name }
                }
            }
        }
    }

    // Stream sources for this title (real data from the worker).
    LaunchedEffect(item.id) {
        runCatching { servicesOrNull?.beamApi?.getMovieLinks(item.id) }.getOrNull()?.let { sources = it.items }
    }

    val title = item.title
    val year = item.year
    val runtime = item.runtime


    val directors = credits?.crew?.filter { it.job == "Director" }?.take(2) ?: emptyList()
    val cast: List<TmdbCast> = credits?.cast?.take(20) ?: emptyList()
    Box(Modifier.fillMaxSize()) {
    LazyColumn(
        Modifier
            .fillMaxSize()
            .background(colors.background),
        contentPadding = PaddingValues(bottom = 28.dp),
    ) {
        item {
            Box(Modifier.fillMaxWidth().aspectRatio(16f / 11f)) {
                val heroPath = item.backdrop_path ?: item.poster_path
                Api.backdropUrl(heroPath, "w780")?.let { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } ?: Box(Modifier.fillMaxSize().background(colors.card))

                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0.00f to Color(0x99000000),
                                0.45f to Color(0x33000000),
                                0.78f to Color(0xCC0A0A0A),
                                1.00f to colors.background,
                            ),
                        ),
                )

                Box(
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(14.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0x8C000000))
                        .border(1.dp, Color(0x1FFFFFFF), CircleShape)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }

        item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = title,
                    color = colors.foreground,
                    fontFamily = Inter,
                    fontSize = 26.sp,
                    lineHeight = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp,
                )

                Spacer(Modifier.height(10.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PillText(if (isSeries) "Series" else "Movie")
                    item.tmdb_rating?.takeIf { it > 0 }?.let { r ->
                        RatingPill(r)
                    }
                    year?.let { PillText(it.toString()) }
                    runtime?.takeIf { it > 0 }?.let { PillText("${it}m") }
                }

                item.genreList().takeIf { it.isNotEmpty() }?.let { genres ->
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = genres.joinToString(", "),
                        color = colors.mutedForeground,
                        fontFamily = GeistMono,
                        fontSize = 12.sp,
                    )
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.foreground)
                            .clickable {
                    if (dev.beam.beamplay.data.EntitlementsState.streamLockedForThisUser) showPremium = true else showSources = true
                },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = colors.background,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (resolving) "Loading\u2026" else "Play",
                            color = colors.background,
                            fontFamily = GeistMono,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.card)
                            .border(1.dp, colors.border, RoundedCornerShape(14.dp))
                            .clickable {
                                showDownloads = true
                                if (downloadFiles.isEmpty() && !downloadsLoading) {
                                    downloadsLoading = true
                                    scope.launch {
                                        downloadFiles = if (isSeries) emptyList() else runCatching {
                                            dev.beam.beamplay.core.network.servicesOrNull?.beamApi
                                                ?.getDownloadOptions("movie", item.id)
                                        }.getOrNull()?.items ?: emptyList()
                                        downloadsLoading = false
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Download,
                            contentDescription = "Download",
                            tint = colors.foreground,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ActionChip(
                        label = "Mark Watched",
                        icon = Icons.Filled.CheckCircle,
                        active = watched,
                        activeTint = colors.green,
                    ) { watched = !watched }
                    ActionChip(
                        label = "Watch Later",
                        icon = if (watchLater) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                        active = watchLater,
                        activeTint = colors.amber500,
                    ) { watchLater = !watchLater }
                    ActionChip(
                        label = "Favorite",
                        icon = if (favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        active = favorite,
                        activeTint = colors.red,
                    ) { favorite = !favorite }
                    ActionChip(
                        label = "Report",
                        icon = Icons.Filled.Flag,
                        active = false,
                        activeTint = colors.red,
                    ) { }
                }
            }
        }

        item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                Spacer(Modifier.height(22.dp))
                DetailSection("Storyline") {
                    Text(
                        text = item.overview?.takeIf { it.isNotBlank() } ?: "No storyline provided yet.",
                        color = colors.mutedForeground,
                        fontFamily = GeistMono,
                        fontSize = 13.sp,
                        lineHeight = 21.sp,
                    )
                }
            }
        }

        if (directors.isNotEmpty()) {
            item {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    DetailSection("Director") {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            directors.forEach { d ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AvatarCircle(d.profile_path, d.name, 44.dp)
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        text = d.name,
                                        color = colors.foreground,
                                        fontFamily = GeistMono,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (cast.isNotEmpty()) {
            item {
                DetailSection("Actors") {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        items(cast) { person ->
                            Column(
                                modifier = Modifier.width(78.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                AvatarCircle(person.profile_path, person.name, 66.dp)
                                Spacer(Modifier.height(7.dp))
                                Text(
                                    text = person.name,
                                    color = colors.foreground,
                                    fontFamily = GeistMono,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                person.character?.takeIf { it.isNotBlank() }?.let { role ->
                                    Text(
                                        text = role,
                                        color = colors.mutedForeground,
                                        fontFamily = GeistMono,
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (studios.isNotEmpty()) {
            item {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    DetailSection("Studios") {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            studios.take(8).forEach { name -> PillText(name) }
                        }
                    }
                }
            }
        }

        if (similar.isNotEmpty()) {
            item {
                DetailSection("More like this") {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        itemsIndexed(similar.take(14)) { index, rec ->
                            ContentCard(
                                title = rec.displayTitle,
                                posterPath = rec.poster_path,
                                rating = rec.vote_average,
                                year = rec.displayYear.take(4).toIntOrNull(),
                                width = 124.dp,
                                onClick = {
                                    onOpenMedia(
                                        MediaItem(
                                            id = 0,
                                            tmdb_id = rec.id,
                                            title = rec.displayTitle,
                                            overview = rec.overview,
                                            poster_path = rec.poster_path,
                                            backdrop_path = rec.backdrop_path,
                                            release_year = rec.displayYear.take(4).toIntOrNull(),
                                            first_release_year = rec.displayYear.take(4).toIntOrNull(),
                                            tmdb_rating = rec.vote_average,
                                            type = if (rec.isMovie) "movie" else "series",
                                        ),
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.navigationBarsPadding().height(8.dp)) }
    }
        if (showSources) {
            SourcesSheet(
                title = title,
                links = sources,
                resolvingUrl = resolvingUrl,
                onPick = { link ->
                    resolvingUrl = link.url
                    scope.launch {
                        val meta = runCatching {
                            dev.beam.beamplay.core.network.servicesOrNull?.vidaraApi?.getStreamMetadata(link.url)
                        }.getOrNull()
                        resolvingUrl = null
                        if (meta != null && meta.streaming_url.isNotBlank()) {
                            showSources = false
                            onPlay(
                                            meta.streaming_url,
                                            meta.subtitles.map { sub ->
                                                val abs = if (sub.file_path.startsWith("http")) sub.file_path
                                                else (dev.beam.beamplay.core.network.servicesOrNull?.vidaraApi?.originOf(link.url) ?: "") + sub.file_path
                                                PlayerSubtitle(sub.language, abs)
                                            },
                                        link.url,
                                        )
                        }
                    }
                },
                onDismiss = { showSources = false },
            )
        }

        if (showPremium) {
            PremiumSheet(onDismiss = { showPremium = false })
        }

        if (showDownloads) {
            DownloadsSheet(
                title = title,
                files = downloadFiles,
                loading = downloadsLoading,
                onDismiss = { showDownloads = false },
            )
        }
    }
}

@Composable
private fun DetailSection(
    title: String,
    horizontalPadding: androidx.compose.ui.unit.Dp = 0.dp,
    content: @Composable () -> Unit,
) {
    val colors = Beam.colors

    Column(Modifier.fillMaxWidth().padding(top = 18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .width(3.dp)
                    .height(17.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.foreground),
            )
            Spacer(Modifier.width(9.dp))
            Text(
                text = title,
                color = colors.foreground,
                fontFamily = Inter,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun AvatarCircle(path: String?, name: String, size: androidx.compose.ui.unit.Dp) {
    val colors = Beam.colors
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(colors.muted),
        contentAlignment = Alignment.Center,
    ) {
        val url = dev.beam.beamplay.core.util.tmdbImageUrl(path, "w185")
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                text = name.take(1).uppercase(),
                color = colors.mutedForeground,
                fontFamily = GeistMono,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun PillText(text: String) {
    val colors = Beam.colors
    Text(
        text = text,
        color = colors.foreground,
        fontFamily = GeistMono,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(7.dp))
            .background(colors.muted)
            .border(1.dp, colors.borderLight, RoundedCornerShape(7.dp))
            .padding(horizontal = 9.dp, vertical = 4.dp),
    )
}

@Composable
private fun RatingPill(rating: Double) {
    val colors = Beam.colors
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(7.dp))
            .background(colors.muted)
            .border(1.dp, colors.borderLight, RoundedCornerShape(7.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.Star,
            contentDescription = null,
            tint = colors.amber300,
            modifier = Modifier.size(12.dp),
        )
        Text(
            text = (kotlin.math.round(rating * 10.0) / 10.0).toString(),
            color = colors.foreground,
            fontFamily = GeistMono,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun ActionChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean,
    activeTint: Color,
    onClick: () -> Unit,
) {
    val colors = Beam.colors
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (active) colors.muted else Color.Transparent)
            .border(1.dp, if (active) colors.border else colors.borderLight, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (active) activeTint else colors.mutedForeground,
            modifier = Modifier.size(15.dp),
        )
        Text(
            text = label,
            color = if (active) colors.foreground else colors.mutedForeground,
            fontFamily = GeistMono,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}
