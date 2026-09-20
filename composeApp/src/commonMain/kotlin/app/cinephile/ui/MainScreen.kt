package app.cinephile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import app.cinephile.core.ui.components.BeamTrendingCard
import app.cinephile.core.ui.components.CatalogFilterBar
import app.cinephile.core.ui.components.ContentCard
import app.cinephile.core.ui.components.FilterOptionsUi
import app.cinephile.core.ui.components.LoadingSkeleton
import app.cinephile.core.ui.components.PosterSkeleton
import app.cinephile.core.ui.components.SectionHeader
import app.cinephile.core.ui.theme.Beam
import app.cinephile.core.ui.theme.Fraunces
import app.cinephile.core.ui.theme.Inter
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
import app.cinephile.core.ui.components.BeamBottomNav
import app.cinephile.core.ui.components.BeamNavItem
import app.cinephile.core.ui.theme.GeistMono
import app.cinephile.data.Api
import app.cinephile.data.MediaItem
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import app.cinephile.core.ui.theme.PillShape
import kotlin.math.roundToInt
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.RangeSliderState
import androidx.compose.material3.SliderDefaults
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.snapshotFlow
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset

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
    // True while a full-screen sheet (e.g. browse filters) owns the screen.
    var overlayOpen by remember { mutableStateOf(false) }
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
            // Bottom padding leaves room for the floating bar so content is never hidden.
            // Clearance for the floating bar only when no sheet is covering the screen.
            Box(Modifier.weight(1f).padding(bottom = if (overlayOpen) 0.dp else 78.dp)) {
                when (tab) {
                    Tab.Home -> HomeTab(handleCardClick, onOpenSearch = { tab = Tab.Search }, onResumeContinue = onResumeContinue)
                    Tab.Browse -> BrowseTab(handleCardClick, onOverlayChange = { overlayOpen = it })
                    Tab.Movies -> CatalogTab(kind = "movies", handleCardClick)
                    Tab.Series -> CatalogTab(kind = "series", handleCardClick)
                    Tab.Search -> SearchTab(handleCardClick)
                    Tab.Library -> LibraryTab(handleCardClick)
                    Tab.Profile -> ProfileTab(subtitleSettings)
                }
            }

        }

        // Hidden while a full-screen sheet is open, so the sheet owns the screen.
        val navTabs = remember { listOf(Tab.Home, Tab.Browse, Tab.Library, Tab.Profile) }
        if (!overlayOpen) {
            BeamBottomNav(
                items = navTabs.map { BeamNavItem(it.label, it.icon) },
                selectedIndex = navTabs.indexOf(tab),
                onSelect = { index -> tab = navTabs[index] },
                modifier = Modifier.align(Alignment.BottomCenter),
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
private fun HomeTab(
    onOpenMedia: (MediaItem) -> Unit,
    onOpenSearch: () -> Unit = {},
    onResumeContinue: (ContinueItem) -> Unit = {},
) {
    var railTick by remember { mutableStateOf(0) }
    var discovery by remember { mutableStateOf("Trending") }
    var kind by remember { mutableStateOf("Movies") }
    var pickTick by remember { mutableStateOf(0) }
    var showAllContinue by remember { mutableStateOf(false) }
    var railGenre by remember { mutableStateOf<String?>(null) }
    // Session-scoped Watch Later: real behaviour, no backend needed yet.
    var watchLater by remember { mutableStateOf<List<MediaItem>>(emptyList()) }

    var trendingMovies by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var trendingSeries by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var nowPlayingMovies by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var airingTodaySeries by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var topRatedMovies by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var topRatedSeries by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var popularMovies by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var popularSeries by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var genreName by remember { mutableStateOf<String?>(null) }
    var genreItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }

    var loading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var retryTrigger by remember { mutableStateOf(0) }
    val resumeItems = remember(railTick) { localContinueWatching() }

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
                val fOpts = async { runCatching { Api.getFilterOptions().genres }.getOrDefault(emptyList()) }

                val tm = tM.await(); val ts = tS.await()
                val npm = npM.await(); val ats = atS.await()
                val trm = trM.await(); val trs = trS.await()
                val pm = pM.await(); val ps = pS.await()

                trendingMovies = tm; trendingSeries = ts
                nowPlayingMovies = npm; airingTodaySeries = ats
                topRatedMovies = trm; topRatedSeries = trs
                popularMovies = pm; popularSeries = ps

                // "Because you like {Genre}" - first genre from the catalog filters.
                val g = fOpts.await().firstOrNull()
                if (g != null) {
                    genreName = g
                    genreItems = runCatching { Api.movies(genre = g, limit = 20).items }.getOrDefault(emptyList())
                }

                if (tm.isEmpty() && ts.isEmpty()) {
                    errorMessage = "No data returned. Tap to retry."
                }
            }
        } catch (e: Exception) {
            println("TMDB Fetch Error: ${e::class.simpleName}: ${e.message}")
            errorMessage = "${e::class.simpleName}: ${e.message ?: "Network error"}"
        } finally {
            loading = false
        }
    }

    // The genre rail is only personalised from a real signal - the user's own
    // list. Without one it stays a plain "Popular in X" row, never a fake
    // "Because you like X".
    LaunchedEffect(watchLater) {
        val liked = watchLater.flatMap { it.genreList() }
            .groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
        if (liked != null && liked != railGenre) {
            railGenre = liked
            genreItems = runCatching { Api.movies(genre = liked, limit = 20).items }
                .getOrDefault(emptyList())
        }
    }

    LazyColumn(
        Modifier.fillMaxSize().background(Beam.colors.background),
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(40.dp),
    ) {
        item { CinephileTopBar(onSearch = onOpenSearch) }

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
                    PillButton("Retry", primary = true) { retryTrigger++ }
                }
            }

            else -> {

                // ---- 1. Continue Watching. Rendered only when something is in
                // progress: finishing a title removes it on its own, so there is
                // never an empty state to show here.
                if (resumeItems.isNotEmpty()) {
                    item {
                        HomeSection(
                            title = "Continue Watching",
                            action = if (resumeItems.size > 4) {
                                { PillButton("View all", primary = false) { showAllContinue = true } }
                            } else {
                                null
                            },
                        ) {
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

                // ---- 2. Discovery: one rail driven by the filter pills ----
                val discoveryItems = when (discovery) {
                    "Popular" -> if (kind == "Movies") popularMovies else popularSeries
                    "Top Rated" -> if (kind == "Movies") topRatedMovies else topRatedSeries
                    "Latest" -> if (kind == "Movies") nowPlayingMovies else airingTodaySeries
                    else -> if (kind == "Movies") trendingMovies else trendingSeries
                }
                item {
                    Column(Modifier.fillMaxWidth()) {
                        // Section pills get their own full-width row: sharing a row with
                        // the media toggle was clipping "Top Rated".
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            item {
                                PillGroup(
                                    options = listOf("Trending", "Popular", "Top Rated", "Latest"),
                                    selected = discovery,
                                    onSelect = { discovery = it },
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                            PillGroup(
                                options = listOf("Movies", "TV"),
                                selected = kind,
                                onSelect = { kind = it },
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        if (discoveryItems.isEmpty()) {
                            Box(Modifier.padding(horizontal = 16.dp)) {
                                EmptyStateCard(
                                    icon = "\u2726",
                                    title = "Nothing here yet",
                                    body = "Try another filter, or search the catalog directly.",
                                    cta = "Search",
                                    onCta = onOpenSearch,
                                )
                            }
                        } else {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                itemsIndexed(
                                    items = discoveryItems,
                                    key = { idx, item -> "disc_" + discovery + "_" + kind + "_" + item.tmdb_id + "_" + idx },
                                ) { _, item ->
                                    DiscoveryCard(
                                        modifier = Modifier.fillParentMaxWidth(0.82f).widthIn(max = 360.dp),
                                        item = item,
                                        added = watchLater.any { it.tmdb_id == item.tmdb_id },
                                        onToggleAdd = {
                                            watchLater = if (watchLater.any { it.tmdb_id == item.tmdb_id }) {
                                                watchLater.filterNot { it.tmdb_id == item.tmdb_id }
                                            } else {
                                                watchLater + item
                                            }
                                        },
                                        onClick = { onOpenMedia(item) },
                                    )
                                }
                            }
                        }
                    }
                }

                // ---- 3. Pick for Me: personal only. It draws from the user's own
                // list, never from trending, so it can never look like a random
                // content dump or repeat the rail above it.
                val pickPool = watchLater.filterNot { w ->
                    w.tmdb_id != null && discoveryItems.any { it.tmdb_id == w.tmdb_id }
                }
                val pick = if (pickPool.isEmpty()) null else pickPool[pickTick % pickPool.size]
                item {
                    Column(Modifier.fillMaxWidth()) {
                        SectionTitleRow(
                            title = "Pick for Me",
                            subtitle = if (pick != null) "from your list" else null,
                            action = { PillButton("Shuffle", primary = false) { pickTick++ } },
                        )
                        Spacer(Modifier.height(12.dp))
                        Box(Modifier.padding(horizontal = 16.dp)) {
                            if (pick == null) {
                                EmptyStateCard(
                                    icon = "\u2726",
                                    title = "Add titles to Watch Later",
                                    body = "Tap the + on any card and we will pick one for you when you cannot decide what to watch.",
                                    cta = "Search the catalog",
                                    onCta = onOpenSearch,
                                )
                            } else {
                                PickForMeCard(
                                    item = pick,
                                    source = "My List",
                                    onView = { onOpenMedia(pick) },
                                    onAdd = {
                                        watchLater = if (watchLater.any { it.tmdb_id == pick.tmdb_id }) {
                                            watchLater
                                        } else {
                                            watchLater + pick
                                        }
                                    },
                                )
                            }
                        }
                    }
                }

                // ---- 4. Because you like {Genre} ----
                if (genreItems.isNotEmpty()) {
                    item {
                        HomeSection(
                            title = railGenre?.let { "Because you like " + it }
                                ?: (genreName?.let { "Popular in " + it } ?: "Recommended for You"),
                        ) {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                itemsIndexed(
                                    items = genreItems.filterNot { g -> discoveryItems.any { it.tmdb_id == g.tmdb_id } },
                                    key = { idx, item -> "genre_" + (genreName ?: "rec") + "_" + item.tmdb_id + "_" + idx },
                                ) { _, item ->
                                    DiscoveryCard(
                                        modifier = Modifier.fillParentMaxWidth(0.82f).widthIn(max = 360.dp),
                                        item = item,
                                        added = watchLater.any { it.tmdb_id == item.tmdb_id },
                                        onToggleAdd = {
                                            watchLater = if (watchLater.any { it.tmdb_id == item.tmdb_id }) {
                                                watchLater.filterNot { it.tmdb_id == item.tmdb_id }
                                            } else {
                                                watchLater + item
                                            }
                                        },
                                        onClick = { onOpenMedia(item) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Full-screen list of everything in progress (reached via "View all").
        item {
            if (showAllContinue) {
                ContinueWatchingAllScreen(
                    items = resumeItems,
                    onPick = { ci -> showAllContinue = false; onResumeContinue(ci) },
                    onRemove = { ci -> removeContinueWatching(ci.sourceUrl); railTick++ },
                    onWatched = { ci -> markContinueWatchingWatched(ci.sourceUrl, ci.title, ci.durationMs); railTick++ },
                    onClose = { showAllContinue = false },
                )
            }
        }
    }
}

/**
 * Mobile navbar per the Cinephile spec: logo left, search + avatar as 40dp
 * bordered circles on the right. Desktop nav pills are intentionally absent.
 */
@Composable
private fun CinephileTopBar(onSearch: () -> Unit) {
    val colors = Beam.colors
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                "Cine",
                color = colors.foreground,
                fontFamily = Fraunces,
                fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp,
                letterSpacing = (-0.6).sp,
                maxLines = 1,
            )
            Text(
                "phile",
                color = colors.amber500,
                fontFamily = Fraunces,
                fontWeight = FontWeight.SemiBold,
                fontStyle = FontStyle.Italic,
                fontSize = 22.sp,
                letterSpacing = (-0.6).sp,
                maxLines = 1,
            )
        }
        Spacer(Modifier.weight(1f))
        CircleIconButton(Icons.Filled.Search, "Search", onSearch)
        Spacer(Modifier.width(10.dp))
        CircleIconButton(Icons.Filled.Person, "Profile") {}
    }
}

@Composable
private fun CircleIconButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    val colors = Beam.colors
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(colors.card)
            .border(1.dp, colors.foreground.copy(alpha = 0.12f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = colors.foreground.copy(alpha = 0.72f),
            modifier = Modifier.size(18.dp),
        )
    }
}

/** Segmented pill switcher: `bg-ring` active, muted inactive, 2dp inset. */
@Composable
private fun PillGroup(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    val colors = Beam.colors
    Row(
        Modifier
            .clip(PillShape)
            .background(colors.card)
            .border(1.dp, colors.border, PillShape)
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEach { option ->
            val active = option == selected
            Box(
                Modifier
                    .height(28.dp)
                    .clip(PillShape)
                    .background(if (active) colors.amber500 else Color.Transparent)
                    .clickable { onSelect(option) }
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    option,
                    color = if (active) Color(0xFF161310) else colors.mutedForeground,
                    fontFamily = GeistMono,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun PillButton(label: String, primary: Boolean, onClick: () -> Unit) {
    val colors = Beam.colors
    Box(
        Modifier
            .clip(PillShape)
            .background(if (primary) colors.amber500 else Color.Transparent)
            .border(1.dp, if (primary) Color.Transparent else colors.border, PillShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (primary) Color(0xFF161310) else colors.foreground,
            fontFamily = GeistMono,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

/** Section header: serif title, optional chevron, optional muted subtitle. */
@Composable
private fun SectionTitleRow(title: String, subtitle: String? = null, onSeeAll: (() -> Unit)? = null, action: (@Composable () -> Unit)? = null) {
    val colors = Beam.colors
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = if (onSeeAll != null) Modifier.clickable(onClick = onSeeAll) else Modifier,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                title,
                color = colors.foreground,
                fontFamily = Fraunces,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.3).sp,
                maxLines = 1,
            )
            if (onSeeAll != null) {
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = colors.mutedForeground,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        if (subtitle != null) {
            Spacer(Modifier.width(10.dp))
            Text(
                subtitle,
                color = colors.mutedForeground,
                fontFamily = GeistMono,
                fontSize = 11.sp,
                maxLines = 1,
            )
        }
        Spacer(Modifier.weight(1f))
        action?.invoke()
    }
}

/** One dashboard section: header + 16dp gap + content. Sections are 40dp apart. */
@Composable
private fun HomeSection(
    title: String,
    subtitle: String? = null,
    onSeeAll: (() -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        SectionTitleRow(title = title, subtitle = subtitle, onSeeAll = onSeeAll, action = action)
        Spacer(Modifier.height(16.dp))
        content()
    }
}

private fun formatRating(value: Double): String {
    val tenths = (value * 10).roundToInt()
    return (tenths / 10).toString() + "." + (tenths % 10).toString()
}

/**
 * The dashboard card: 26dp chrome, 8dp padding, 16:10 backdrop, bottom-up
 * gradient, title + rating/year over the image, Watch Later chip top-right.
 */
@Composable
private fun DiscoveryCard(
    modifier: Modifier = Modifier,
    item: MediaItem,
    added: Boolean,
    onToggleAdd: () -> Unit,
    onClick: () -> Unit,
) {
    val colors = Beam.colors
    Box(
        modifier
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
            val art = item.backdrop_path ?: item.poster_path
            if (art != null) {
                AsyncImage(
                    model = Api.backdropUrl(art, "w780"),
                    contentDescription = item.title,
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
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(if (added) colors.amber500.copy(alpha = 0.30f) else Color.Black.copy(alpha = 0.50f))
                    .border(
                        1.dp,
                        if (added) colors.amber500.copy(alpha = 0.60f) else Color(0x26FFFFFF),
                        CircleShape,
                    )
                    .clickable(onClick = onToggleAdd),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (added) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                    contentDescription = if (added) "Remove from Watch Later" else "Add to Watch Later",
                    tint = if (added) colors.amber500 else Color(0xD9FFFFFF),
                    modifier = Modifier.size(16.dp),
                )
            }
            Column(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 14.dp, end = 14.dp, bottom = 10.dp),
            ) {
                Text(
                    item.title,
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
                    item.tmdb_rating?.let { rating ->
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = Color(0xFFF5C77E),
                            modifier = Modifier.size(12.dp),
                        )
                        Text(
                            formatRating(rating),
                            color = Color.White.copy(alpha = 0.85f),
                            fontFamily = GeistMono,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            "\u00B7",
                            color = Color.White.copy(alpha = 0.45f),
                            fontFamily = GeistMono,
                            fontSize = 13.sp,
                        )
                    }
                    Text(
                        item.year?.toString() ?: "\u2014",
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

/** Pick for Me: image left, content right, badge + meta + 2-line overview + actions. */
@Composable
private fun PickForMeCard(
    item: MediaItem,
    source: String,
    onView: () -> Unit,
    onAdd: () -> Unit,
) {
    val colors = Beam.colors
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(colors.card)
            .border(1.dp, colors.foreground.copy(alpha = 0.12f), RoundedCornerShape(26.dp))
            .clickable(onClick = onView)
            .padding(12.dp),
    ) {
        Column {
            Row {
                val art = item.backdrop_path ?: item.poster_path
                Box(
                    Modifier
                        .width(128.dp)
                        .height(96.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(colors.muted),
                ) {
                    if (art != null) {
                        AsyncImage(
                            model = Api.backdropUrl(art, "w500"),
                            contentDescription = item.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(
                        Modifier
                            .clip(PillShape)
                            .background(colors.amber500.copy(alpha = 0.05f))
                            .border(1.dp, colors.amber500.copy(alpha = 0.20f), PillShape)
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "\u2726 Tonight's pick",
                            color = colors.amber500,
                            fontFamily = GeistMono,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        item.title,
                        color = colors.foreground,
                        fontFamily = GeistMono,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 21.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(
                        Modifier.padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        item.year?.let {
                            Text(
                                it.toString(),
                                color = colors.mutedForeground,
                                fontFamily = GeistMono,
                                fontSize = 13.sp,
                            )
                        }
                        item.tmdb_rating?.let { rating ->
                            Text("\u00B7", color = colors.mutedForeground, fontFamily = GeistMono, fontSize = 13.sp)
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                                tint = Color(0xFFF0B457),
                                modifier = Modifier.size(12.dp),
                            )
                            Text(
                                formatRating(rating),
                                color = colors.mutedForeground,
                                fontFamily = GeistMono,
                                fontSize = 13.sp,
                            )
                        }
                        Text(
                            "\u00B7 " + source,
                            color = colors.mutedForeground,
                            fontFamily = GeistMono,
                            fontSize = 13.sp,
                            maxLines = 1,
                        )
                    }
                    item.overview?.takeIf { it.isNotBlank() }?.let { text ->
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text,
                            color = colors.mutedForeground,
                            fontFamily = GeistMono,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PillButton("View Details", primary = true, onClick = onView)
                PillButton("Add to List", primary = false, onClick = onAdd)
            }
        }
    }
}

/** Empty card: amber icon, title, body, outline CTA. */
@Composable
private fun EmptyStateCard(
    icon: String,
    title: String,
    body: String,
    cta: String,
    onCta: () -> Unit,
) {
    val colors = Beam.colors
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(colors.card)
            .border(1.dp, colors.border, RoundedCornerShape(26.dp))
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(icon, color = colors.amber500.copy(alpha = 0.40f), fontSize = 26.sp)
        Spacer(Modifier.height(10.dp))
        Text(
            title,
            color = colors.foreground,
            fontFamily = GeistMono,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            body,
            color = colors.mutedForeground,
            fontFamily = GeistMono,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        PillButton(cta, primary = false, onClick = onCta)
    }
}

/** Loading: four 192dp rounded blocks, matching the spec's skeleton rhythm. */
@Composable
private fun HomeLoadingSkeleton() {
    Column(Modifier.fillMaxWidth()) {
        repeat(4) {
            LoadingSkeleton(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(192.dp)
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(26.dp),
            )
            Spacer(Modifier.height(32.dp))
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
private fun BrowseTab(
    onOpenMedia: (MediaItem) -> Unit,
    onOverlayChange: (Boolean) -> Unit = {},
) {
    var kind by remember { mutableStateOf("movies") }
    var items by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var page by remember { mutableStateOf(1) }
    var loading by remember { mutableStateOf(true) }
    var loadingMore by remember { mutableStateOf(false) }
    var endReached by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    var genre by remember { mutableStateOf("") }
    var language by remember { mutableStateOf("") }
    var yearStart by remember { mutableStateOf<Int?>(null) }
    var yearEnd by remember { mutableStateOf<Int?>(null) }
    var options by remember { mutableStateOf(FilterOptionsUi()) }
    var showFilters by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf<List<Long>>(emptyList()) }
    var retry by remember { mutableStateOf(0) }

    val colors = Beam.colors
    val gridState = rememberLazyGridState()

    LaunchedEffect(Unit) {
        runCatching { Api.getFilterOptions() }.getOrNull()?.let { f ->
            options = FilterOptionsUi(f.genres, f.years, f.languages)
        }
    }

    // Single year goes through the exact-match parameter; a real range uses the
    // two range parameters the worker gained alongside this screen.
    val exactYear = if (yearStart != null && yearStart == yearEnd) yearStart.toString() else ""
    val fromYear = if (yearStart != null && yearStart != yearEnd) yearStart.toString() else null
    val toYear = if (yearEnd != null && yearStart != yearEnd) yearEnd.toString() else null

    suspend fun loadPage(target: Int, replace: Boolean) {
        if (replace) loading = true else loadingMore = true
        error = null
        try {
            val res = if (kind == "movies") {
                Api.movies(
                    page = target, limit = 30, genre = genre, year = exactYear,
                    yearFrom = fromYear, yearTo = toYear, language = language,
                )
            } else {
                Api.seriesList(
                    page = target, limit = 30, genre = genre, year = exactYear,
                    yearFrom = fromYear, yearTo = toYear, language = language,
                )
            }
            val incoming = res.items
            items = if (replace) {
                incoming
            } else {
                items + incoming.filter { fresh -> items.none { it.id == fresh.id && it.type == fresh.type } }
            }
            page = target
            endReached = incoming.size < 30
        } catch (e: Exception) {
            error = "Could not load content. Check your connection."
        } finally {
            loading = false
            loadingMore = false
        }
    }

    LaunchedEffect(kind, genre, language, exactYear, fromYear, toYear, retry) {
        if (retry > 0) kotlinx.coroutines.delay(200)
        loadPage(1, replace = true)
    }

    // Hide the bottom bar while the filter sheet is up.
    LaunchedEffect(showFilters) { onOverlayChange(showFilters) }


    LaunchedEffect(gridState) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .collect { last ->
                if (!endReached && !loading && !loadingMore && items.isNotEmpty() && last >= items.size - 6) {
                    loadPage(page + 1, replace = false)
                }
            }
    }

    val rangeActive = yearStart != null && yearEnd != null && yearStart != yearEnd
    val activeCount = listOf(genre.isNotBlank(), language.isNotBlank(), rangeActive || exactYear.isNotBlank())
        .count { it }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Browse",
                color = Color.White,
                fontFamily = Fraunces,
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.5).sp,
            )
            Spacer(Modifier.weight(1f))
            Row(
                Modifier
                    .height(36.dp)
                    .clip(RoundedCornerShape(50))
                    .background(colors.card)
                    .border(1.dp, colors.border, RoundedCornerShape(50))
                    .clickable { showFilters = true }
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(Icons.Filled.Tune, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Text("Filter", color = Color.White, fontFamily = GeistMono, fontSize = 13.sp)
                if (activeCount > 0) {
                    Box(
                        Modifier.size(18.dp).clip(CircleShape).background(colors.amber500),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = activeCount.toString(),
                            color = Color(0xFF101014),
                            fontFamily = GeistMono,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            BoxWithConstraints(
                Modifier
                    .width(228.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(50))
                    .background(colors.card)
                    .border(1.dp, colors.border, RoundedCornerShape(50))
                    .padding(3.dp),
            ) {
                val segment = (maxWidth - 6.dp) / 2
                val target = if (kind == "movies") 0.dp else segment
                // The amber pill glides between the two labels.
                val pillX by androidx.compose.animation.core.animateDpAsState(
                    targetValue = target,
                    animationSpec = androidx.compose.animation.core.spring(
                        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioLowBouncy,
                        stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow,
                    ),
                    label = "browse-segment",
                )
                Box(
                    Modifier
                        .offset(x = pillX)
                        .width(segment)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(50))
                        .background(colors.amber500),
                )
                Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                    listOf("movies" to "Movies", "series" to "TV Shows").forEach { pair ->
                        val on = kind == pair.first
                        Box(
                            Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable { kind = pair.first },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = pair.second,
                                color = if (on) Color(0xFF101014) else colors.mutedForeground,
                                fontFamily = GeistMono,
                                fontSize = 14.sp,
                                fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        when {
            loading && items.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colors.amber500, strokeWidth = 2.dp)
            }

            error != null && items.isEmpty() -> Column(
                Modifier.fillMaxWidth().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = error ?: "",
                    color = colors.mutedForeground,
                    fontFamily = GeistMono,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(12.dp))
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(colors.amber500)
                        .clickable { retry++ }
                        .padding(horizontal = 16.dp, vertical = 9.dp),
                ) {
                    Text("Retry", color = Color(0xFF101014), fontFamily = GeistMono, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            items.isEmpty() -> Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = "Nothing here yet.",
                    color = colors.mutedForeground,
                    fontFamily = GeistMono,
                    fontSize = 13.sp,
                )
            }

            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                state = gridState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(
                    items = items,
                    key = { it.id.toString() + "_" + (it.type ?: "") },
                ) { item ->
                    BrowsePosterCard(
                        item = item,
                        saved = saved.contains(item.id),
                        onToggleSave = {
                            saved = if (saved.contains(item.id)) saved - item.id else saved + item.id
                        },
                        onClick = { onOpenMedia(item) },
                    )
                }

                if (loadingMore) {
                    item(span = { GridItemSpan(3) }) {
                        Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = colors.amber500, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }

    if (showFilters) {
        BrowseFilterSheet(
            kind = kind,
            genres = options.genres,
            languages = options.languages,
            years = options.years,
            genre = genre,
            language = language,
            yearStart = yearStart,
            yearEnd = yearEnd,
            onApply = { g, l, ys, ye ->
                genre = g
                language = l
                yearStart = ys
                yearEnd = ye
                showFilters = false
            },
            onReset = {
                genre = ""
                language = ""
                yearStart = null
                yearEnd = null
            },
            onDismiss = { showFilters = false },
        )
    }
}

/** Compact 3-column grid card: poster, bookmark chip, title, rating + year. */
@Composable
private fun BrowsePosterCard(
    item: MediaItem,
    saved: Boolean,
    onToggleSave: () -> Unit,
    onClick: () -> Unit,
) {
    val colors = Beam.colors
    Column(Modifier.clickable(onClick = onClick)) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(6.dp))
                .background(colors.card),
        ) {
            Api.backdropUrl(item.poster_path, "w342")?.let { url ->
                AsyncImage(
                    model = url,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (saved) colors.amber500.copy(alpha = 0.30f) else Color.Black.copy(alpha = 0.40f))
                    .clickable(onClick = onToggleSave),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (saved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                    contentDescription = if (saved) "Remove bookmark" else "Bookmark",
                    tint = if (saved) colors.amber500 else Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(14.dp),
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        Text(
            text = item.title,
            color = Color.White,
            fontFamily = GeistMono,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            item.tmdb_rating?.takeIf { it > 0 }?.let { rating ->
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = colors.amber500,
                    modifier = Modifier.size(10.dp),
                )
                Text(formatRating(rating), color = colors.mutedForeground, fontFamily = GeistMono, fontSize = 11.sp)
                Text("\u00B7", color = colors.mutedForeground, fontFamily = GeistMono, fontSize = 11.sp)
            }
            Text(
                text = item.year?.toString() ?: "\u2014",
                color = colors.mutedForeground,
                fontFamily = GeistMono,
                fontSize = 11.sp,
                maxLines = 1,
            )
        }
    }
}


/**
 * One collapsible filter row: label left, current value + chevron right. Tapping
 * expands an inline list instead of pushing a separate picker screen.
 */
@Composable
private fun ExpandableFilterRow(
    title: String,
    selected: String,
    options: List<String>,
    onSelect: (String) -> Unit,
) {
    val colors = Beam.colors
    var open by remember { mutableStateOf(false) }
    val label = selected.ifBlank { "Any" }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.card)
            .border(1.dp, colors.border, RoundedCornerShape(12.dp)),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { open = !open }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, color = Color.White, fontFamily = GeistMono, fontSize = 14.sp)
            Spacer(Modifier.weight(1f))
            Text(
                text = label,
                color = if (selected.isBlank()) colors.mutedForeground else colors.amber500,
                fontFamily = GeistMono,
                fontSize = 14.sp,
                maxLines = 1,
            )
            Spacer(Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = colors.mutedForeground,
                modifier = Modifier.size(20.dp),
            )
        }

        if (open) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(colors.border))
            Column(Modifier.heightIn(max = 220.dp).verticalScroll(rememberScrollState())) {
                (listOf("" to "Any") + options.map { it to it }).forEach { pair ->
                    val value = pair.first
                    val text = pair.second
                    val isSelected = selected == value
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(if (isSelected) colors.amber500.copy(alpha = 0.10f) else Color.Transparent)
                            .clickable {
                                onSelect(value)
                                open = false
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = text,
                            color = if (isSelected) colors.amber500 else Color.White,
                            fontFamily = GeistMono,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Filter sheet: two collapsible dropdowns and a year range slider - short enough
 * to fit one screen, so there is no chip wall to scroll through.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun BrowseFilterSheet(
    kind: String,
    genres: List<String>,
    languages: List<String>,
    years: List<Int>,
    genre: String,
    language: String,
    yearStart: Int?,
    yearEnd: Int?,
    onApply: (String, String, Int?, Int?) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = Beam.colors
    var g by remember { mutableStateOf(genre) }
    var l by remember { mutableStateOf(language) }

    // Slider bounds come from the catalogue, with a sane fallback.
    val lo = (years.minOrNull() ?: 1988).toFloat()
    val hi = (years.maxOrNull() ?: 2026).toFloat()
    val sliderState = remember {
        androidx.compose.material3.RangeSliderState(
            activeRangeStart = (yearStart ?: lo.toInt()).toFloat(),
            activeRangeEnd = (yearEnd ?: hi.toInt()).toFloat(),
            valueRange = lo..hi,
        )
    }

    var count by remember { mutableStateOf<Int?>(null) }
    var countKey by remember { mutableStateOf(0) }

    // Live "Show N Results": one cheap paged call reads the new `total` field.
    LaunchedEffect(sliderState.activeRangeStart, sliderState.activeRangeEnd, g, l, countKey) {
        kotlinx.coroutines.delay(250)
        val start = sliderState.activeRangeStart.toInt()
        val end = sliderState.activeRangeEnd.toInt()
        val exact = if (start == end) start.toString() else ""
        count = runCatching {
            if (kind == "movies") {
                Api.movies(
                    page = 1, limit = 1, genre = g, year = exact, language = l,
                    yearFrom = if (start != end) start.toString() else null,
                    yearTo = if (start != end) end.toString() else null,
                ).total
            } else {
                Api.seriesList(
                    page = 1, limit = 1, genre = g, year = exact, language = l,
                    yearFrom = if (start != end) start.toString() else null,
                    yearTo = if (start != end) end.toString() else null,
                ).total
            }
        }.getOrNull()
    }

    PlatformBackHandler(enabled = true) { onDismiss() }

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable(onClick = onDismiss),
        )

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                .background(colors.card)
                // Swallow taps on the sheet's own empty space so they cannot
                // reach the scrim behind it and dismiss the sheet.
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null,
                ) {}
                .navigationBarsPadding()
                .padding(24.dp),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Filters",
                    color = Color.White,
                    fontFamily = Fraunces,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "Reset",
                    color = colors.amber500,
                    fontFamily = GeistMono,
                    fontSize = 14.sp,
                    modifier = Modifier.clickable {
                        g = ""
                        l = ""
                        sliderState.activeRangeStart = lo
                        sliderState.activeRangeEnd = hi
                        onReset()
                        countKey++
                    },
                )
                Spacer(Modifier.width(12.dp))
                // Close affordance instead of a drag line: same chip language as
                // the header buttons elsewhere in the app.
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(colors.muted)
                        .border(1.dp, colors.border, CircleShape)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close filters",
                        tint = colors.foreground.copy(alpha = 0.85f),
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            ExpandableFilterRow(
                title = "Genre",
                selected = g,
                options = genres,
                onSelect = { g = it },
            )

            Spacer(Modifier.height(12.dp))

            ExpandableFilterRow(
                title = "Language",
                selected = l,
                options = languages,
                onSelect = { l = it },
            )

            Spacer(Modifier.height(24.dp))

            Text(
                text = "YEAR",
                color = colors.mutedForeground,
                fontFamily = GeistMono,
                fontSize = 12.sp,
                letterSpacing = 1.sp,
            )

            Spacer(Modifier.height(8.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = sliderState.activeRangeStart.toInt().toString(),
                    color = Color.White,
                    fontFamily = GeistMono,
                    fontSize = 12.sp,
                )
                Text(
                    text = sliderState.activeRangeEnd.toInt().toString(),
                    color = Color.White,
                    fontFamily = GeistMono,
                    fontSize = 12.sp,
                )
            }

            androidx.compose.material3.RangeSlider(
                state = sliderState,
                colors = androidx.compose.material3.SliderDefaults.colors(
                    thumbColor = colors.amber500,
                    activeTrackColor = colors.amber500,
                    inactiveTrackColor = colors.border,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(20.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Box(
                    Modifier
                        .width(220.dp)
                        .height(44.dp)
                        .clip(RoundedCornerShape(50))
                        .background(colors.amber500)
                        .clickable {
                            val s = sliderState.activeRangeStart.toInt()
                            val e = sliderState.activeRangeEnd.toInt()
                            onApply(
                                g,
                                l,
                                if (s > lo.toInt()) s else null,
                                if (e < hi.toInt()) e else null,
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = count?.let { "Show $it Results" } ?: "Show Results",
                        color = Color(0xFF101014),
                        fontFamily = GeistMono,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                    )
                }
            }

            // Keeps the apply button clear of the phone's navigation buttons.
            Spacer(Modifier.height(30.dp))

        }
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
    val session by app.cinephile.data.SessionManager.session.collectAsState()
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

/**
 * Full-screen "Continue Watching" list: everything in progress, picked from here.
 * A Dialog is used so it sits above the floating navigation bar, and the system
 * back gesture closes it.
 */
@Composable
private fun ContinueWatchingAllScreen(
    items: List<ContinueItem>,
    onPick: (ContinueItem) -> Unit,
    onRemove: (ContinueItem) -> Unit,
    onWatched: (ContinueItem) -> Unit,
    onClose: () -> Unit,
) {
    val colors = Beam.colors
    Dialog(
        onDismissRequest = onClose,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .background(colors.background)
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Continue Watching",
                    color = colors.foreground,
                    fontFamily = Fraunces,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(colors.muted)
                        .border(1.dp, colors.border, CircleShape)
                        .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = colors.foreground.copy(alpha = 0.85f),
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            if (items.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Nothing in progress.",
                        color = colors.mutedForeground,
                        fontFamily = GeistMono,
                        fontSize = 13.sp,
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    itemsIndexed(
                        items = items,
                        key = { idx, ci -> "cwall_" + ci.sourceUrl + "_" + idx },
                    ) { _, ci ->
                        ContinueWatchingResumeCard(
                            item = ci,
                            onClick = { onPick(ci) },
                            onRemove = { onRemove(ci) },
                            onWatched = { onWatched(ci) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}
