package dev.beam.beamplay.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import dev.beam.beamplay.core.ui.components.BeamTrendingCard
import dev.beam.beamplay.core.ui.components.CatalogFilterBar
import dev.beam.beamplay.core.ui.components.ContentCard
import dev.beam.beamplay.core.ui.components.FilterOptionsUi
import dev.beam.beamplay.core.ui.components.LoadingSkeleton
import dev.beam.beamplay.core.ui.components.PosterSkeleton
import dev.beam.beamplay.core.ui.components.SectionHeader
import dev.beam.beamplay.core.ui.theme.Beam
import dev.beam.beamplay.core.ui.theme.Inter
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil3.compose.AsyncImage
import dev.beam.beamplay.core.ui.components.BeamBottomNav
import dev.beam.beamplay.core.ui.components.BeamNavItem
import dev.beam.beamplay.core.ui.theme.GeistMono
import dev.beam.beamplay.data.Api
import dev.beam.beamplay.data.MediaItem
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

enum class Tab(val label: String, val icon: ImageVector) {
    Home("Home", Icons.Filled.Home),
    Browse("Browse", Icons.Filled.Explore),
    Movies("Movies", Icons.Filled.Movie),
    Series("Series", Icons.Filled.Tv),
    Search("Search", Icons.Filled.Search),
    Library("Library", Icons.Filled.LibraryBooks),
    Profile("Profile", Icons.Filled.Person),
}

/**
 * Request Modal Dialog — identical to web app's RequestModal.tsx
 */
@Composable
fun RequestModalDialog(
    item: MediaItem,
    onDismiss: () -> Unit,
) {
    var isSubmitting by remember { mutableStateOf(false) }
    var submitted by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF141416))
                .border(1.dp, Color(0xFF2E2E34), RoundedCornerShape(20.dp))
                .padding(20.dp),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Top close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        if (submitted) "Request Sent" else "Not Available Yet",
                        fontFamily = GeistMono,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color(0x33FFFFFF))
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Backdrop image preview if available
                val img = Api.backdropUrl(item.backdrop_path ?: item.poster_path, "w500")
                if (!img.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clip(RoundedCornerShape(12.dp)),
                    ) {
                        AsyncImage(
                            model = img,
                            contentDescription = item.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                }

                Text(
                    text = item.title,
                    fontFamily = GeistMono,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                Spacer(Modifier.height(8.dp))

                Text(
                    text = if (submitted) {
                        "We've received your request for \"${item.title}\". We'll add it as soon as possible!"
                    } else {
                        "\"${item.title}\" isn't in our database yet. Tap Request and we'll add it for you."
                    },
                    fontFamily = GeistMono,
                    fontSize = 12.5.sp,
                    color = Color(0xFFA1A1AA),
                    lineHeight = 18.sp,
                )

                errorMsg?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = Color(0xFFF87171), fontSize = 11.5.sp, fontFamily = GeistMono)
                }

                Spacer(Modifier.height(20.dp))

                if (!submitted) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Button(
                            onClick = {
                                scope.launch {
                                    isSubmitting = true
                                    val ok = Api.submitRequest(item.title, item.tmdb_id, item.type ?: "movie")
                                    isSubmitting = false
                                    if (ok) submitted = true else errorMsg = "Could not submit request."
                                }
                            },
                            enabled = !isSubmitting,
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Text("+ Request to Add", fontFamily = GeistMono, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222226), contentColor = Color.White),
                        ) {
                            Text("Not now", fontFamily = GeistMono, fontSize = 13.sp)
                        }
                    }
                } else {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                    ) {
                        Text("Done", fontFamily = GeistMono, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun MainScreen(initialTab: Tab = Tab.Home, onOpenMedia: (MediaItem) -> Unit, subtitleSettings: @Composable () -> Unit = {}, onResumeContinue: (ContinueItem) -> Unit = {}) {
    var tab by remember { mutableStateOf(initialTab) }
    var requestItem by remember { mutableStateOf<MediaItem?>(null) }
    var isCheckingDb by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Exact card click logic from reference-web-repo/src/lib/useCardClick.ts
    val handleCardClick: (MediaItem) -> Unit = { item ->
        if (item.id > 0) {
            onOpenMedia(item)
        } else if (item.tmdb_id != null) {
            scope.launch {
                isCheckingDb = true
                val inDb = if (item.type == "series") Api.getSeriesByTmdb(item.tmdb_id) else Api.getMovieByTmdb(item.tmdb_id)
                isCheckingDb = false
                if (inDb != null && inDb.id > 0) {
                    onOpenMedia(inDb)
                } else {
                    // Not in DB -> show Request dialog!
                    requestItem = item
                }
            }
        } else {
            onOpenMedia(item)
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .background(BeamColors.bg),
        ) {
            Box(Modifier.weight(1f)) {
                when (tab) {
                    Tab.Home -> HomeTab(handleCardClick, onOpenSearch = { tab = Tab.Search }, onResumeContinue = onResumeContinue)
                    Tab.Browse -> BrowseTab(handleCardClick)
                    Tab.Movies -> CatalogTab(kind = "movies", handleCardClick)
                    Tab.Series -> CatalogTab(kind = "series", handleCardClick)
                    Tab.Search -> SearchTab(handleCardClick)
                    Tab.Library -> LibraryTab(handleCardClick)
                    Tab.Profile -> ProfileTab(subtitleSettings)
                }
            }

            // Bottom navigation - matches beamPlay-web `.bottom-nav`
            val navTabs = remember { listOf(Tab.Home, Tab.Browse, Tab.Library, Tab.Profile) }
            BeamBottomNav(
                items = navTabs.map { BeamNavItem(it.label, it.icon) },
                selectedIndex = navTabs.indexOf(tab),
                onSelect = { index -> tab = navTabs[index] },
            )
        }

        // Checking indicator overlay
        if (isCheckingDb) {
            Box(
                Modifier.fillMaxSize().background(Color(0x44000000)),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp)
            }
        }

        // Request Modal popup
        requestItem?.let { item ->
            RequestModalDialog(item = item, onDismiss = { requestItem = null })
        }
    }
}

@Composable
private fun HomeTab(onOpenMedia: (MediaItem) -> Unit, onOpenSearch: () -> Unit = {}, onResumeContinue: (ContinueItem) -> Unit = {}) {
    var railTick by remember { mutableStateOf(0) }
    var trendingTab by remember { mutableStateOf("Movie") }
    var latestTab by remember { mutableStateOf("Movie") }
    var topRatedTab by remember { mutableStateOf("Movie") }
    var popularTab by remember { mutableStateOf("Movie") }

    var trendingMovies by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var trendingSeries by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var nowPlayingMovies by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var airingTodaySeries by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var topRatedMovies by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var topRatedSeries by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var popularMovies by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var popularSeries by remember { mutableStateOf<List<MediaItem>>(emptyList()) }

    var loading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var retryTrigger by remember { mutableStateOf(0) }

    LaunchedEffect(retryTrigger) {
        loading = true
        errorMessage = null
        try {
            coroutineScope {
                val tM = async { Api.getTmdbTrendingMovies() }
                val tS = async { Api.getTmdbTrendingSeries() }
                val npM = async { Api.getTmdbNowPlayingMovies() }
                val atS = async { Api.getTmdbAiringTodaySeries() }
                val trM = async { Api.getTmdbTopRatedMovies() }
                val trS = async { Api.getTmdbTopRatedSeries() }
                val pM = async { Api.getTmdbPopularMovies() }
                val pS = async { Api.getTmdbPopularSeries() }

                val tm = tM.await()
                val ts = tS.await()
                val npm = npM.await()
                val ats = atS.await()
                val trm = trM.await()
                val trs = trS.await()
                val pm = pM.await()
                val ps = pS.await()

                trendingMovies = tm
                trendingSeries = ts
                nowPlayingMovies = npm
                airingTodaySeries = ats
                topRatedMovies = trm
                topRatedSeries = trs
                popularMovies = pm
                popularSeries = ps

                if (tm.isEmpty() && ts.isEmpty()) {
                    errorMessage = "No TMDB data returned. Tap to retry."
                }
            }
        } catch (e: Exception) {
            println("TMDB Fetch Error: ${e::class.simpleName}: ${e.message}")
            errorMessage = "${e::class.simpleName}: ${e.message ?: "Network error"}"
        } finally {
            loading = false
        }
    }

    LazyColumn(
        Modifier.fillMaxSize().background(Beam.colors.background),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item { BeamTopBar(onSearch = onOpenSearch) }

        when {
            loading -> item { HomeLoadingSkeleton() }

            errorMessage != null -> item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = Color(0xFFF87171),
                        fontFamily = GeistMono,
                        fontSize = 12.sp,
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { retryTrigger++ },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Text("Retry", fontFamily = GeistMono, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            else -> {
                val trendingItems = if (trendingTab == "Movie") trendingMovies else trendingSeries
                val latestItems = if (latestTab == "Movie") nowPlayingMovies else airingTodaySeries
                val topRatedItems = if (topRatedTab == "Movie") topRatedMovies else topRatedSeries
                val popularItems = if (popularTab == "Movie") popularMovies else popularSeries

                // Continue Watching - locally remembered streams, no network.
                item {
                    val resumeItems = remember(railTick) { localContinueWatching() }
                    if (resumeItems.isNotEmpty()) {
                        HomeSection(title = "Continue Watching") {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                itemsIndexed(
                                    items = resumeItems,
                                    key = { idx, ci -> "cw_" + ci.sourceUrl + "_" + idx },
                                ) { _, ci ->
                                    ContinueWatchingResumeCard(
                                        item = ci,
                                        onClick = { onResumeContinue(ci) },
                                        onRemove = { removeContinueWatching(ci.sourceUrl); railTick++ },
                                        onWatched = { markContinueWatchingWatched(ci.sourceUrl, ci.title, ci.durationMs); railTick++ },
                                    )
                                }
                            }
                        }
                    }
                }

                // Trending â€” rank-numbered backdrop cards, Movie / TV Show tabs.
                if (trendingItems.isNotEmpty()) {
                    item {
                        HomeSection(
                            title = "Trending",
                            tabs = listOf("Movie", "TV Show"),
                            activeTab = trendingTab,
                            onTabChange = { trendingTab = it },
                        ) {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                itemsIndexed(
                                    items = trendingItems,
                                    key = { idx, item -> "tr_${trendingTab}_${item.tmdb_id}_$idx" },
                                ) { index, item ->
                                    BeamTrendingCard(
                                        title = item.title,
                                        backdropPath = item.backdrop_path,
                                        posterPath = item.poster_path,
                                        rating = item.tmdb_rating,
                                        year = item.year,
                                        rank = index + 1,
                                        onClick = { onOpenMedia(item) },
                                        modifier = Modifier
                                            .fillParentMaxWidth(0.82f)
                                            .widthIn(max = 360.dp),
                                    )
                                }
                            }
                        }
                    }
                }

                // Latest â€” Now Playing / Airing Today.
                if (latestItems.isNotEmpty()) {
                    item {
                        HomeSection(
                            title = "Latest",
                            tabs = listOf("Movie", "TV Show"),
                            activeTab = latestTab,
                            onTabChange = { latestTab = it },
                        ) {
                            PosterRow(
                                keyPrefix = "l_${latestTab}",
                                items = latestItems,
                                onOpenMedia = onOpenMedia,
                            )
                        }
                    }
                }

                // Top Rated.
                if (topRatedItems.isNotEmpty()) {
                    item {
                        HomeSection(
                            title = "Top Rated",
                            tabs = listOf("Movie", "TV Show"),
                            activeTab = topRatedTab,
                            onTabChange = { topRatedTab = it },
                        ) {
                            PosterRow(
                                keyPrefix = "tr_${topRatedTab}",
                                items = topRatedItems,
                                onOpenMedia = onOpenMedia,
                            )
                        }
                    }
                }

                // Popular.
                if (popularItems.isNotEmpty()) {
                    item {
                        HomeSection(
                            title = "Popular",
                            tabs = listOf("Movie", "TV Show"),
                            activeTab = popularTab,
                            onTabChange = { popularTab = it },
                        ) {
                            PosterRow(
                                keyPrefix = "pop_${popularTab}",
                                items = popularItems,
                                onOpenMedia = onOpenMedia,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Mobile navbar, matching the site's `t4tsa-nav` layout at phone width:
 * row 1 = the `BEAM` wordmark (Inter 800, 24sp, -0.04em tracking),
 * row 2 = the full-width "Search Movies" pill (`t4tsa-search-btn`:
 * 44dp tall, 16dp radius, `--card` fill, 1dp light border).
 */
@Composable
private fun BeamTopBar(onSearch: () -> Unit) {
    val colors = Beam.colors

    Column(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "BEAM",
                color = colors.foreground,
                fontFamily = Inter,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.96).sp,
                maxLines = 1,
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(colors.card)
                .border(1.dp, Color(0xB3CACACA), RoundedCornerShape(16.dp))
                .clickable(onClick = onSearch)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = colors.foreground,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = "Search Movies",
                color = colors.mutedForeground,
                fontFamily = GeistMono,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.325).sp,
                maxLines = 1,
            )
        }
    }
}


/** `.section` â€” 48dp bottom rhythm, 16dp side padding on phones. */
@Composable
private fun HomeSection(
    title: String,
    tabs: List<String>? = null,
    activeTab: String? = null,
    onTabChange: ((String) -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 48.dp),
    ) {
        SectionHeader(
            title = title,
            modifier = Modifier.padding(horizontal = 16.dp),
            tabs = tabs,
            activeTab = activeTab,
            onTabChange = onTabChange,
        )
        Spacer(Modifier.height(16.dp))
        content()
    }
}

/** A carousel of poster cards (`.carousel-track` â€” 12dp gaps, 24dp end pad). */
@Composable
private fun PosterRow(
    keyPrefix: String,
    items: List<MediaItem>,
    onOpenMedia: (MediaItem) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(start = 16.dp, end = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        itemsIndexed(
            items = items,
            key = { idx, item -> "${keyPrefix}_${item.tmdb_id}_$idx" },
        ) { _, item ->
            ContentCard(
                title = item.title,
                posterPath = item.poster_path,
                rating = item.tmdb_rating,
                year = item.year,
                width = 128.dp,
                onClick = { onOpenMedia(item) },
            )
        }
    }
}

/** Loading state â€” the web renders one section of 8 dimmed poster blocks. */
@Composable
private fun HomeLoadingSkeleton() {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 48.dp),
    ) {
        Box(Modifier.padding(horizontal = 16.dp)) {
            LoadingSkeleton(
                modifier = Modifier.width(96.dp).height(16.dp),
                shape = RoundedCornerShape(6.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            userScrollEnabled = false,
        ) {
            items(count = 8) {
                PosterSkeleton(width = 128.dp, modifier = Modifier.alpha(0.5f))
            }
        }
    }
}

@Composable
private fun CatalogTab(kind: String, onOpenMedia: (MediaItem) -> Unit) {
    var items by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var page by remember { mutableStateOf(1) }
    var loading by remember { mutableStateOf(true) }
    var loadingMore by remember { mutableStateOf(false) }
    var endReached by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    var genre by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var language by remember { mutableStateOf("") }
    var options by remember { mutableStateOf(FilterOptionsUi()) }
    var retry by remember { mutableStateOf(0) }

    val scope = rememberCoroutineScope()

    // Filter values come from the database, never hardcoded.
    LaunchedEffect(Unit) {
        runCatching { Api.getFilterOptions() }.getOrNull()?.let { f ->
            options = FilterOptionsUi(f.genres, f.years, f.languages)
        }
    }

    suspend fun loadPage(target: Int, replace: Boolean) {
        if (replace) loading = true else loadingMore = true
        error = null
        try {
            val res = if (kind == "movies") {
                Api.movies(page = target, limit = 24, genre = genre, year = year, language = language)
            } else {
                Api.seriesList(page = target, limit = 24, genre = genre, year = year, language = language)
            }
            val incoming = res.items
            items = if (replace) {
                incoming
            } else {
                items + incoming.filter { fresh -> items.none { it.id == fresh.id && it.type == fresh.type } }
            }
            page = target
            endReached = incoming.size < 24
        } catch (e: Exception) {
            error = "Could not load content. Check your connection."
        } finally {
            loading = false
            loadingMore = false
        }
    }

    // Debounced reload whenever a filter changes (matches the website).
    LaunchedEffect(kind, genre, year, language, retry) {
        if (retry > 0 || genre.isNotBlank() || year.isNotBlank() || language.isNotBlank()) {
            kotlinx.coroutines.delay(300)
        }
        loadPage(1, replace = true)
    }

    val gridState = rememberLazyGridState()
    val shouldLoadMore by remember {
        derivedStateOf {
            val last = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            !loading && !loadingMore && !endReached && last >= items.size - 5 && items.isNotEmpty()
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) loadPage(page + 1, replace = false)
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Beam.colors.background)
            .statusBarsPadding(),
    ) {
        Text(
            text = if (kind == "movies") "Movies" else "Series",
            color = Beam.colors.foreground,
            fontFamily = Inter,
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.5).sp,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
        )

        Box(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            CatalogFilterBar(
                options = options,
                activeGenre = genre,
                activeYear = year,
                activeLanguage = language,
                onGenreChange = { genre = it },
                onYearChange = { year = it },
                onLanguageChange = { language = it },
                onClear = { genre = ""; year = ""; language = "" },
            )
        }

        when {
            loading -> LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                userScrollEnabled = false,
                modifier = Modifier.fillMaxSize(),
            ) {
                items(count = 8) {
                    Column {
                        PosterSkeleton(modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(10.dp))
                        LoadingSkeleton(
                            modifier = Modifier.fillMaxWidth().height(12.dp),
                            shape = RoundedCornerShape(6.dp),
                        )
                    }
                }
            }

            error != null -> Column(
                Modifier.fillMaxWidth().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = error ?: "",
                    color = Color(0xFFF87171),
                    fontFamily = GeistMono,
                    fontSize = 12.sp,
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { retry++ },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Text("Retry", fontFamily = GeistMono, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            items.isEmpty() -> Column(
                Modifier.fillMaxWidth().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = if (kind == "movies") "No movies found" else "No series found",
                    color = Beam.colors.foreground,
                    fontFamily = Inter,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (genre.isNotBlank() || year.isNotBlank() || language.isNotBlank()) {
                        "Try adjusting your filters."
                    } else {
                        "Content will appear here once imported."
                    },
                    color = Beam.colors.mutedForeground,
                    fontFamily = GeistMono,
                    fontSize = 12.sp,
                )
            }

            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                state = gridState,
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(count = items.size, key = { index -> "${kind}_${items[index].tmdb_id}_$index" }) { index ->
                    val item = items[index]
                    ContentCard(
                        title = item.title,
                        posterPath = item.poster_path,
                        rating = item.tmdb_rating,
                        year = item.year,
                        width = null,
                        onClick = { onOpenMedia(item) },
                    )
                }

                if (!endReached) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(
                            Modifier.fillMaxWidth().padding(vertical = 18.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (loadingMore) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(22.dp),
                                )
                            } else {
                                Box(
                                    Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(Beam.colors.card)
                                        .border(1.dp, Beam.colors.border, RoundedCornerShape(999.dp))
                                        .clickable { scope.launch { loadPage(page + 1, replace = false) } }
                                        .padding(horizontal = 26.dp, vertical = 11.dp),
                                ) {
                                    Text(
                                        text = "Load More",
                                        color = Beam.colors.foreground,
                                        fontFamily = GeistMono,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BrowseTab(onOpenMedia: (MediaItem) -> Unit) {
    var kind by remember { mutableStateOf("movies") }
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf("movies" to "Movies", "series" to "TV Shows").forEach { pair ->
                val on = kind == pair.first
                Box(
                    Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (on) Color(0xFFF5A623) else Color(0x1FFFFFFF))
                        .clickable { kind = pair.first }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = pair.second,
                        color = if (on) Color(0xFF101014) else Color(0xFFEDEDED),
                        fontFamily = GeistMono,
                        fontSize = 13.sp,
                    )
                }
            }
        }
        CatalogTab(kind = kind, onOpenMedia = onOpenMedia)
    }
}

@Composable
private fun SearchTab(onOpenMedia: (MediaItem) -> Unit) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var searched by remember { mutableStateOf(false) }

    val trendingTags = listOf("obsession", "The Furious", "From", "Avatar", "Obsession", "Love Island", "Alpha")

    LaunchedEffect(query) {
        val q = query.trim()
        if (q.length < 2) { results = emptyList(); searched = false; return@LaunchedEffect }
        kotlinx.coroutines.delay(300)
        loading = true
        try {
            results = Api.search(q).items
            searched = true
        } catch (_: Exception) { }
        loading = false
    }

    Column(Modifier.fillMaxSize().background(BeamColors.bg).statusBarsPadding()) {
        Text(
            "Search",
            fontFamily = GeistMono,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search all", fontFamily = GeistMono, color = Color(0xFF71717A)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            shape = RoundedCornerShape(9999.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions.Default,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.White,
                unfocusedBorderColor = Color(0xFF2E2E34),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = Color.White,
            ),
        )

        if (query.isEmpty()) {
            Spacer(Modifier.height(20.dp))
            Text(
                "Everyone searching",
                fontFamily = GeistMono,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFA1A1AA),
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(Modifier.height(10.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(trendingTags) { tag ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(9999.dp))
                            .background(Color(0xFF1E1E22))
                            .border(1.dp, Color(0xFF2E2E34), RoundedCornerShape(9999.dp))
                            .clickable { query = tag }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    ) {
                        Text(
                            tag,
                            fontFamily = GeistMono,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        if (loading) {
            Box(Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp)
            }
        } else if (searched && results.isEmpty()) {
            ErrorBlock("No results for \"$query\"")
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(results, key = { "${it.type ?: "movie"}_${it.id}" }) {
                    LatestPosterCard(it, width = 110.dp, onClick = { onOpenMedia(it) })
                }
            }
        }
    }
}

@Composable
private fun LibraryTab(onOpenMedia: (MediaItem) -> Unit) {
    val session by dev.beam.beamplay.data.SessionManager.session.collectAsState()
    val s = session

    Column(Modifier.fillMaxSize().background(BeamColors.bg).statusBarsPadding()) {
        Text(
            "Library",
            fontFamily = GeistMono,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )

        if (s == null || s.method == "guest") {
            Box(Modifier.fillMaxWidth().padding(top = 100.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🍿", fontSize = 44.sp)
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "Your Library",
                        fontFamily = GeistMono,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Watchlist, Liked & Continue Watching sync will be fully enabled here",
                        fontFamily = GeistMono,
                        fontSize = 12.5.sp,
                        color = Color(0xFFA1A1AA),
                        modifier = Modifier.padding(horizontal = 40.dp),
                        lineHeight = 18.sp,
                    )
                }
            }
        } else {
            var items by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
            var loading by remember { mutableStateOf(true) }
            LaunchedEffect(Unit) {
                try {
                    items = Api.library("favorites").items
                } catch (_: Exception) { }
                loading = false
            }
            if (loading) {
                Box(Modifier.fillMaxWidth().padding(top = 100.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp)
                }
            } else if (items.isEmpty()) {
                ErrorBlock("Your library is empty — bookmark titles to see them here")
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(items, key = { "${it.type ?: "movie"}_${it.id}" }) {
                        LatestPosterCard(it, width = 110.dp, onClick = { onOpenMedia(it) })
                    }
                }
            }
        }
    }
}

/** Profile -> Settings -> Subtitle settings. */
@Composable
private fun ProfileTab(subtitleSettings: @Composable () -> Unit) {
    var showSettings by remember { mutableStateOf(false) }

    Box(
        Modifier
            .fillMaxSize()
            .background(BeamColors.bg),
    ) {
        if (showSettings) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier
                            .size(22.dp)
                            .clickable { showSettings = false },
                    )
                    Spacer(Modifier.width(14.dp))
                    Text("Settings", color = Color.White, fontSize = 18.sp)
                }
                subtitleSettings()
            }
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp),
            ) {
                Spacer(Modifier.height(12.dp))
                Text("Profile", color = Color.White, fontSize = 24.sp)
                Spacer(Modifier.height(20.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF141419))
                        .clickable { showSettings = true }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Filled.Settings,
                        contentDescription = "Settings",
                        tint = Color(0xFFEDEDED),
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(14.dp))
                    Text("Settings", color = Color(0xFFEDEDED), fontSize = 15.sp)
                }
            }
        }
    }
}
