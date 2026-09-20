package app.cinephile.ui

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
import app.cinephile.core.model.TmdbCast
import app.cinephile.core.model.TmdbCredits
import app.cinephile.core.model.TmdbItem
import app.cinephile.core.network.servicesOrNull
import app.cinephile.core.ui.components.ContentCard
import app.cinephile.ui.player.PlayerSubtitle
import kotlinx.coroutines.launch
import app.cinephile.core.ui.theme.Beam
import app.cinephile.core.ui.theme.Fraunces
import app.cinephile.core.ui.theme.GeistMono
import app.cinephile.core.ui.theme.Inter
import app.cinephile.data.Api
import app.cinephile.data.MediaItem
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.text.style.TextAlign
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.layout.fillMaxHeight

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
    trailer: app.cinephile.core.model.TmdbVideo? = null,
    onTrailer: () -> Unit = {},
    onOpenMedia: (MediaItem) -> Unit,
    onPlay: (String, List<PlayerSubtitle>, String) -> Unit,
) {
    val colors = Beam.colors
    val tmdbId = item.tmdb_id ?: 0L
    val isSeries = item.type == "series"

    var credits by remember { mutableStateOf<TmdbCredits?>(null) }
    var similar by remember { mutableStateOf<List<TmdbItem>>(emptyList()) }
    var studios by remember { mutableStateOf<List<String>>(emptyList()) }
    var videos by remember { mutableStateOf<List<app.cinephile.core.model.TmdbVideo>>(emptyList()) }
    var sources by remember { mutableStateOf<List<app.cinephile.core.model.Link>>(emptyList()) }
    var watchLater by remember { mutableStateOf(false) }
    var favorite by remember { mutableStateOf(false) }
    var watched by remember { mutableStateOf(false) }
    var myRating by remember { mutableStateOf(0) }
    var showTrailer by remember { mutableStateOf(false) }
    var resolving by remember { mutableStateOf(false) }
    var showSources by remember { mutableStateOf(false) }
    var showPremium by remember { mutableStateOf(false) }
    // True when the worker refused the links because of the premium gate - the UI must be
    // able to say "locked", never show an empty list and let the user guess.
    var premiumRequired by remember { mutableStateOf(false) }
    val premiumLocked = premiumRequired || app.cinephile.data.EntitlementsState.streamLockedForThisUser
    var showDownloads by remember { mutableStateOf(false) }

    // Back closes an open sheet first - never the whole screen underneath it.
    PlatformBackHandler(enabled = showSources || showDownloads || showPremium || showTrailer) {
        showSources = false
        showDownloads = false
        showPremium = false
        showTrailer = false
    }
    var resolvingUrl by remember { mutableStateOf<String?>(null) }
    var downloadFiles by remember { mutableStateOf<List<app.cinephile.core.model.DownloadFile>>(emptyList()) }
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
        runCatching { servicesOrNull?.beamApi?.getMovieLinks(item.id) }
            .onSuccess { result -> sources = result?.items ?: emptyList(); premiumRequired = false }
            .onFailure {
                val locked = app.cinephile.data.EntitlementsState.streamLockedForThisUser
                premiumRequired = locked
                if (!locked) sources = emptyList()
            }
    }

    val title = item.title
    val year = item.year
    val runtime = item.runtime


    val directors = credits?.crew?.filter { it.job == "Director" }?.take(2) ?: emptyList()
    val trailer = videos.firstOrNull { it.isYouTubeTrailer }
        ?: item.trailer_key?.takeIf { it.isNotBlank() }?.let { key ->
            app.cinephile.core.model.TmdbVideo(key = key, site = "YouTube", type = "Trailer", name = "Trailer")
        }
    // Hero resume bar: match this title against the local playback memory.
    val resumeItem = remember(item.id) { localContinueWatching().firstOrNull { it.title == item.title } }
    val cast: List<TmdbCast> = credits?.cast?.take(20) ?: emptyList()
    Box(Modifier.fillMaxSize()) {
    LazyColumn(
        Modifier
            .fillMaxSize()
            .background(colors.background),
        contentPadding = PaddingValues(bottom = 28.dp),
    ) {
        item {
            HeroCard(
                item = item,
                directors = directors.map { it.name },
                premiumLocked = premiumLocked,
                resolving = resolving,
                watched = watched,
                favorite = favorite,
                watchLater = watchLater,
                onBack = onBack,
                resume = resumeItem,
                trailer = trailer,
                onTrailer = { showTrailer = true },
                onPlay = {
                    if (premiumLocked) {
                        premiumRequired = true
                        showPremium = true
                    } else {
                        showSources = true
                    }
                },
                onWatchLater = { watchLater = !watchLater },
                onWatched = { watched = !watched },
                onFavorite = { favorite = !favorite },
                onDownload = {
                    showDownloads = true
                    if (downloadFiles.isEmpty() && !downloadsLoading) {
                        downloadsLoading = true
                        scope.launch {
                            downloadFiles = if (isSeries) emptyList() else runCatching {
                                app.cinephile.core.network.servicesOrNull?.beamApi
                                    ?.getDownloadOptions("movie", item.id)
                            }.getOrNull()?.items ?: emptyList()
                            downloadsLoading = false
                        }
                    }
                },
            )
        }

        // ---- Overview ----
        item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                Spacer(Modifier.height(24.dp))
                SectionLabel("Overview")
                Spacer(Modifier.height(6.dp))
                Text(
                    text = item.overview?.takeIf { it.isNotBlank() } ?: "No overview available.",
                    color = colors.foreground,
                    fontFamily = GeistMono,
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                )
            }
        }

        // ---- Ratings grid: only the sources we actually have data for ----
        val tmdbRating = item.tmdb_rating?.takeIf { it > 0 }
        val imdbRating = item.imdb_rating?.takeIf { it > 0 }
        val rtRating = item.rt_rating?.takeIf { it.isNotBlank() }
        val metacritic = item.metacritic?.takeIf { it > 0 }
        if (tmdbRating != null || imdbRating != null || rtRating != null || metacritic != null) {
            item {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    Spacer(Modifier.height(24.dp))
                    // Always two columns, so a card is the same size whether one,
                    // two, three or four ratings exist. A missing slot is filled with
                    // an invisible spacer - never by stretching its neighbour.
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (tmdbRating != null) {
                            RatingCard(label = "TMDB", value = fmt1(tmdbRating), star = true, modifier = Modifier.weight(1f))
                        } else {
                            Spacer(Modifier.weight(1f))
                        }
                        if (imdbRating != null) {
                            RatingCard(label = "IMDb", value = fmt1(imdbRating), star = false, modifier = Modifier.weight(1f))
                        } else {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                    if (rtRating != null || metacritic != null) {
                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (rtRating != null) {
                                RatingCard(
                                    label = "Rotten Tomatoes", value = rtRating, star = false,
                                    tint = Color(0xFFEF4444), modifier = Modifier.weight(1f),
                                )
                            } else {
                                Spacer(Modifier.weight(1f))
                            }
                            if (metacritic != null) {
                                RatingCard(label = "Metacritic", value = metacritic.toString(), star = false, modifier = Modifier.weight(1f))
                            } else {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        // ---- Box office: only when the enrichment job has filled it in ----
        val budget = item.budget?.takeIf { it > 0L }
        val revenue = item.revenue?.takeIf { it > 0L }
        if (budget != null || revenue != null) {
            item {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    Spacer(Modifier.height(24.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (budget != null) {
                            RatingCard("Budget", "$" + money(budget), star = false, compact = true, modifier = Modifier.weight(1f))
                        } else {
                            Spacer(Modifier.weight(1f))
                        }
                        if (revenue != null) {
                            RatingCard("Revenue", "$" + money(revenue), star = false, compact = true, modifier = Modifier.weight(1f))
                        } else {
                            Spacer(Modifier.weight(1f))
                        }
                        if (budget != null && revenue != null) {
                            val profit = revenue - budget
                            RatingCard(
                                label = "Profit",
                                value = (if (profit >= 0) "+$" else "-$") + money(if (profit >= 0) profit else -profit),
                                star = false,
                                compact = true,
                                tint = if (profit >= 0) Color(0xFF10B981) else Color(0xFFEF4444),
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }

        // ---- Your Tracking: rating only; watched/favourite live in the hero ----
        item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                Spacer(Modifier.height(24.dp))
                TrackingCard(
                    watched = watched,
                    favorite = favorite,
                    rating = myRating,
                    onWatched = { watched = !watched },
                    onFavorite = { favorite = !favorite },
                    onRating = { myRating = it },
                )
            }
        }

        // ---- Cast: 3 columns, circular portraits, name + character ----
        if (cast.isNotEmpty()) {
            item {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    Spacer(Modifier.height(24.dp))
                    SectionLabel("Cast")
                    Spacer(Modifier.height(12.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(cast.take(10)) { person ->
                            Column(
                                modifier = Modifier.width(72.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                AvatarCircle(person.profile_path, person.name, 56.dp)
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = person.name,
                                    color = colors.foreground,
                                    fontFamily = GeistMono,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center,
                                )
                                person.character?.takeIf { it.isNotBlank() }?.let { role ->
                                    Text(
                                        text = role,
                                        color = colors.mutedForeground,
                                        fontFamily = GeistMono,
                                        fontSize = 9.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ---- More Like This: TMDB's own similar titles for this movie ----
        if (similar.isNotEmpty()) {
            item {
                Column(Modifier.fillMaxWidth()) {
                    Spacer(Modifier.height(24.dp))
                    Column(Modifier.padding(horizontal = 16.dp)) { SectionLabel("More Like This") }
                    Spacer(Modifier.height(12.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        itemsIndexed(similar.take(14)) { _, rec ->
                            RailCard(
                                title = rec.displayTitle,
                                backdropPath = rec.backdrop_path,
                                posterPath = rec.poster_path,
                                rating = rec.vote_average,
                                year = rec.displayYear.take(4).toIntOrNull(),
                                modifier = Modifier.fillParentMaxWidth(0.82f),
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

        item { Spacer(Modifier.navigationBarsPadding().height(16.dp)) }
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
                            app.cinephile.core.network.servicesOrNull?.vidaraApi?.getStreamMetadata(link.url)
                        }.getOrNull()
                        resolvingUrl = null
                        if (meta != null && meta.streaming_url.isNotBlank()) {
                            showSources = false
                            onPlay(
                                            meta.streaming_url,
                                            meta.subtitles.map { sub ->
                                                val abs = if (sub.file_path.startsWith("http")) sub.file_path
                                                else (app.cinephile.core.network.servicesOrNull?.vidaraApi?.originOf(link.url) ?: "") + sub.file_path
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

        if (showTrailer && trailer != null) {
            TrailerModal(
                videoTitle = trailer.name.ifBlank { "Trailer" },
                videoKey = trailer.key,
                title = title,
                onClose = { showTrailer = false },
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
        val url = app.cinephile.core.util.tmdbImageUrl(path, "w185")
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

private fun money(value: Long): String {
    val millions = value / 1_000_000.0
    val tenths = (millions * 10).roundToInt()
    return (tenths / 10).toString() + "." + (tenths % 10).toString() + "M"
}

private fun fmt1(value: Double): String {
    val tenths = (value * 10).roundToInt()
    return (tenths / 10).toString() + "." + (tenths % 10).toString()
}

/** Uppercase serif label used for every section heading on this screen. */
@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        color = Beam.colors.mutedForeground,
        fontFamily = Fraunces,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp,
        modifier = modifier,
    )
}

/** Thin pill used for genres, studios and metadata. */
@Composable
private fun MetaPill(text: String) {
    val colors = Beam.colors
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(colors.muted)
            .border(1.dp, colors.border, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = text,
            color = colors.foreground.copy(alpha = 0.85f),
            fontFamily = GeistMono,
            fontSize = 11.sp,
            maxLines = 1,
        )
    }
}

/** Solid amber / outline pill button, the screen's only two button styles. */
@Composable
private fun DetailButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    primary: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val colors = Beam.colors
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(if (primary) colors.amber500 else Color.Transparent)
            .border(1.dp, if (primary) Color.Transparent else colors.border, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (primary) Color(0xFF161310) else colors.foreground,
            modifier = Modifier.size(15.dp),
        )
        Text(
            text = label,
            color = if (primary) Color(0xFF161310) else colors.foreground,
            fontFamily = GeistMono,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * The hero: backdrop with a gradient into the page colour, poster, serif title,
 * meta row, genre pills, director credit and the action row.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun HeroCard(
    item: MediaItem,
    directors: List<String>,
    premiumLocked: Boolean,
    resolving: Boolean,
    watched: Boolean,
    favorite: Boolean,
    watchLater: Boolean,
    resume: ContinueItem?,
    onBack: () -> Unit,
    onPlay: () -> Unit,
    onWatchLater: () -> Unit,
    onWatched: () -> Unit,
    onFavorite: () -> Unit,
    onDownload: () -> Unit,
    trailer: app.cinephile.core.model.TmdbVideo? = null,
    onTrailer: () -> Unit = {},
) {
    val colors = Beam.colors
    val isSeries = item.type == "series"

    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(colors.card)
            .border(1.dp, colors.border, RoundedCornerShape(26.dp)),
    ) {
        Column {
            // ---- backdrop: melts into the page colour, carries the back button
            // and (when partially watched) the resume bar ----
            Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
                val heroPath = item.backdrop_path ?: item.poster_path
                Api.backdropUrl(heroPath, "w780")?.let { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } ?: Box(Modifier.fillMaxSize().background(colors.card))

                // Status-bar legibility band, then the melt-into-page gradient.
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0x99000000), Color(0x00000000)),
                            ),
                        ),
                )
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0.00f to Color(0x33000000),
                                0.55f to Color(0xB30E0E0D),
                                1.00f to colors.background,
                            ),
                        ),
                )

                Box(
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(12.dp)
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0x99000000))
                        .border(1.dp, Color(0x1FFFFFFF), CircleShape)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(19.dp),
                    )
                }

                resume?.let { r ->
                    val fraction = if (r.durationMs > 0L) {
                        (r.positionMs.toFloat() / r.durationMs.toFloat()).coerceIn(0f, 1f)
                    } else {
                        0f
                    }
                    if (fraction > 0f || r.positionMs > 0L) {
                        Column(Modifier.align(Alignment.BottomStart).padding(start = 14.dp, bottom = 10.dp)) {
                            Text(
                                text = "Resume from " + stampOf(r.positionMs),
                                color = Color.White.copy(alpha = 0.85f),
                                fontFamily = GeistMono,
                                fontSize = 12.sp,
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
                                    .background(colors.amber500),
                            )
                        }
                    }
                }
            }

            // ---- poster straddles the boundary between image and content ----
            Row(Modifier.padding(start = 14.dp, end = 14.dp)) {
                Box(
                    Modifier
                        .offset(y = (-38).dp)
                        .width(96.dp)
                        .aspectRatio(2f / 3f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(colors.muted)
                        .border(1.dp, colors.border, RoundedCornerShape(18.dp)),
                ) {
                    Api.backdropUrl(item.poster_path, "w342")?.let { url ->
                        AsyncImage(
                            model = url,
                            contentDescription = item.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column(Modifier.padding(top = 10.dp)) {
                    Text(
                        text = item.title,
                        color = colors.foreground,
                        fontFamily = Fraunces,
                        fontSize = 28.sp,
                        lineHeight = 34.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.4).sp,
                    )

                    Spacer(Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        item.tmdb_rating?.takeIf { it > 0 }?.let { rating ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = null,
                                    tint = Color(0xFFF0B457),
                                    modifier = Modifier.size(14.dp),
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = fmt1(rating),
                                    color = Color(0xFFF0B457),
                                    fontFamily = GeistMono,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                            Text("\u00B7", color = colors.mutedForeground, fontFamily = GeistMono, fontSize = 14.sp)
                        }
                        item.year?.let {
                            Text(
                                text = it.toString(),
                                color = colors.mutedForeground,
                                fontFamily = GeistMono,
                                fontSize = 14.sp,
                            )
                            Text("\u00B7", color = colors.mutedForeground, fontFamily = GeistMono, fontSize = 14.sp)
                        }
                        Text(
                            text = if (isSeries) {
                                val seasons = item.total_seasons ?: 0
                                if (seasons > 0) "$seasons season" + (if (seasons > 1) "s" else "") else "\u2014"
                            } else {
                                val runtime = item.runtime ?: 0
                                if (runtime > 0) (runtime / 60).toString() + "h " + (runtime % 60).toString() + "m" else "\u2014"
                            },
                            color = colors.mutedForeground,
                            fontFamily = GeistMono,
                            fontSize = 14.sp,
                        )
                        Text("\u00B7", color = colors.mutedForeground, fontFamily = GeistMono, fontSize = 14.sp)
                        Text(
                            text = if (isSeries) "TV" else "MOVIE",
                            color = colors.mutedForeground,
                            fontFamily = GeistMono,
                            fontSize = 12.sp,
                            letterSpacing = 0.6.sp,
                        )
                    }

                    val genres = item.genreList().take(4)
                    if (genres.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        androidx.compose.foundation.layout.FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            genres.forEach { genre -> MetaPill(genre) }
                        }
                    }

                    if (directors.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Row {
                            Text(
                                text = "Directed by ",
                                color = colors.mutedForeground,
                                fontFamily = GeistMono,
                                fontSize = 13.sp,
                            )
                            Text(
                                text = directors.joinToString(", "),
                                color = colors.amber500,
                                fontFamily = GeistMono,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }

            // ---- one wrapping action row: three labelled primaries, then the
            // two tracking toggles as icon-only buttons ----
            // Two per row: four buttons on one line clipped their labels.
            Row(
                Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DetailButton(
                    label = if (resolving) "Loading\u2026" else "Play",
                    icon = if (premiumLocked) PremiumCrown else Icons.Filled.PlayArrow,
                    primary = true,
                    modifier = Modifier.weight(1f),
                    onClick = onPlay,
                )
                DetailButton(
                    label = "My List",
                    icon = if (watchLater) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                    primary = false,
                    modifier = Modifier.weight(1f),
                    onClick = onWatchLater,
                )
            }

            Row(
                Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DetailButton(
                    label = "Download",
                    icon = Icons.Filled.Download,
                    primary = false,
                    modifier = Modifier.weight(1f),
                    onClick = onDownload,
                )
                if (trailer != null) {
                    DetailButton(
                        label = "Trailer",
                        icon = Icons.Filled.PlayArrow,
                        primary = false,
                        modifier = Modifier.weight(1f),
                        onClick = onTrailer,
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                }
            }

            // Row 2: tracking toggles, icon-only.
            Row(
                Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                IconToggleButton(
                    icon = Icons.Filled.CheckCircle,
                    contentDescription = if (watched) "Watched" else "Mark watched",
                    active = watched,
                    onClick = onWatched,
                )
                IconToggleButton(
                    icon = if (favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = if (favorite) "Favorite" else "Add to favorites",
                    active = favorite,
                    onClick = onFavorite,
                )
            }
        }
    }
}

/** Square-ish icon toggle for the hero: amber when active, outline otherwise. */
@Composable
private fun IconToggleButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    active: Boolean,
    onClick: () -> Unit,
) {
    val colors = Beam.colors
    Box(
        Modifier
            .height(36.dp)
            .size(36.dp)
            .clip(RoundedCornerShape(50))
            .background(if (active) colors.amber500.copy(alpha = 0.18f) else Color.Transparent)
            .border(
                1.dp,
                if (active) colors.amber500.copy(alpha = 0.55f) else colors.border,
                RoundedCornerShape(50),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (active) colors.amber500 else colors.foreground.copy(alpha = 0.9f),
            modifier = Modifier.size(17.dp),
        )
    }
}

private fun stampOf(positionMs: Long): String {
    val minutes = positionMs / 60000L
    val seconds = (positionMs / 1000L) % 60L
    return minutes.toString() + ":" + seconds.toString().padStart(2, '0')
}

/**
 * Personal rating only. Watched / favourite live in the hero as icon toggles -
 * repeating them here was duplication, so this card does one job.
 */
@Composable
private fun TrackingCard(
    watched: Boolean,
    favorite: Boolean,
    rating: Int,
    onWatched: () -> Unit,
    onFavorite: () -> Unit,
    onRating: (Int) -> Unit,
) {
    val colors = Beam.colors
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colors.card)
            .border(1.dp, colors.amber500.copy(alpha = 0.20f), RoundedCornerShape(18.dp))
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = colors.amber500,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = "Your Tracking",
                color = colors.foreground,
                fontFamily = Fraunces,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }

        Spacer(Modifier.height(14.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconToggleButton(
                icon = Icons.Filled.CheckCircle,
                contentDescription = if (watched) "Watched" else "Mark watched",
                active = watched,
                onClick = onWatched,
            )
            IconToggleButton(
                icon = if (favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = if (favorite) "Favorite" else "Add to favorites",
                active = favorite,
                onClick = onFavorite,
            )
        }

        Spacer(Modifier.height(14.dp))

        Text(
            text = "Your rating",
            color = colors.foreground,
            fontFamily = GeistMono,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            (1..5).forEach { star ->
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = "$star stars",
                    tint = if (star <= rating) Color(0xFFF0B457) else colors.mutedForeground.copy(alpha = 0.40f),
                    modifier = Modifier.size(24.dp).clickable { onRating(star) },
                )
            }
            if (rating > 0) {
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "$rating/5",
                    color = colors.mutedForeground,
                    fontFamily = GeistMono,
                    fontSize = 13.sp,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Ratings are kept on this device for now.",
            color = colors.mutedForeground,
            fontFamily = GeistMono,
            fontSize = 11.sp,
        )
    }
}
/** Compact data chip: 64dp for ratings, 56dp for box office (visual hierarchy). */
@Composable
private fun RatingCard(
    label: String,
    value: String,
    star: Boolean,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
    compact: Boolean = false,
) {
    val colors = Beam.colors
    val shape = RoundedCornerShape(if (compact) 10.dp else 12.dp)
    Column(
        modifier = modifier
            .height(if (compact) 60.dp else 76.dp)
            .clip(shape)
            .background(colors.card)
            .border(1.dp, colors.border, shape)
            .padding(if (compact) 10.dp else 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = label.uppercase(),
            color = colors.mutedForeground,
            fontFamily = GeistMono,
            fontSize = 10.sp,
            letterSpacing = 0.6.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (star) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = Color(0xFFF0B457),
                    modifier = Modifier.size(if (compact) 14.dp else 16.dp),
                )
            }
            Text(
                text = value,
                color = if (tint == Color.Unspecified) colors.foreground else tint,
                fontFamily = GeistMono,
                fontSize = if (compact) 16.sp else 20.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

/** Carousel card: 26dp chrome, 16:10 backdrop, title + rating/year over the image. */
@Composable
private fun RailCard(
    modifier: Modifier = Modifier,
    title: String,
    backdropPath: String?,
    posterPath: String?,
    rating: Double?,
    year: Int?,
    onClick: () -> Unit,
) {
    val colors = Beam.colors
    Box(
        Modifier
            .then(modifier)
            .widthIn(max = 360.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(colors.card)
            .border(1.dp, colors.foreground.copy(alpha = 0.12f), RoundedCornerShape(26.dp))
            .clickable(onClick = onClick)
            .padding(8.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 10f)
                .clip(RoundedCornerShape(18.dp))
                .background(colors.muted),
        ) {
            val art = backdropPath ?: posterPath
            if (art != null) {
                AsyncImage(
                    model = Api.backdropUrl(art, "w780"),
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0x0D000000), Color(0x40000000), Color(0xE0000000)),
                        ),
                    ),
            )
            Column(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 14.dp, end = 14.dp, bottom = 10.dp),
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontFamily = GeistMono,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 19.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    Modifier.padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    rating?.takeIf { it > 0 }?.let { value ->
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = Color(0xFFF5C77E),
                            modifier = Modifier.size(12.dp),
                        )
                        Text(
                            text = fmt1(value),
                            color = Color.White.copy(alpha = 0.85f),
                            fontFamily = GeistMono,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        Text("\u00B7", color = Color.White.copy(alpha = 0.45f), fontFamily = GeistMono, fontSize = 13.sp)
                    }
                    Text(
                        text = year?.toString() ?: "\u2014",
                        color = Color.White.copy(alpha = 0.85f),
                        fontFamily = GeistMono,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

