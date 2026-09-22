package app.cinephile.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import app.cinephile.core.ui.theme.Beam
import app.cinephile.core.ui.theme.GeistMono
import app.cinephile.data.Api
import app.cinephile.data.MediaItem
import app.cinephile.data.SearchHistory
import app.cinephile.data.TmdbSearchItem

private val SearchTypes = listOf("All", "Movie", "TV", "Anime", "Asian")
private val SortOptions = listOf("Relevance", "Year", "Rating", "Title")
private val RatingSteps = listOf(0.0, 5.0, 6.0, 7.0, 8.0)
private val YearWindows = listOf("Any", "2024+", "2020s", "2010s", "Older")

/** TMDB genre ids are stable reference data, so a small map beats another round trip. */
private val GenreNames = mapOf(
    28L to "Action", 12L to "Adventure", 16L to "Animation", 35L to "Comedy",
    80L to "Crime", 99L to "Documentary", 18L to "Drama", 10751L to "Family",
    14L to "Fantasy", 36L to "History", 27L to "Horror", 10402L to "Music",
    9648L to "Mystery", 10749L to "Romance", 878L to "Sci-Fi", 53L to "Thriller",
    10752L to "War", 37L to "Western", 10759L to "Action & Adventure",
    10765L to "Sci-Fi & Fantasy",
)

/**
 * Search as an overlay, not a destination: whatever is behind stays behind, and
 * closing it drops you back exactly where you were.
 *
 * Typing shows suggestions (titles and people from one TMDB call); pressing
 * search turns the same surface into the results page. The type pills drive
 * both, so what you filtered while typing is what you get in the results.
 */
@Composable
fun SearchOverlay(
    onDismiss: () -> Unit,
    onOpenMedia: (MediaItem) -> Unit,
    onOpenPerson: (Long) -> Unit,
) {
    val colors = Beam.colors

    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("All") }
    var suggestions by remember { mutableStateOf<List<TmdbSearchItem>>(emptyList()) }
    var results by remember { mutableStateOf<List<TmdbSearchItem>>(emptyList()) }
    var showingResults by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var sort by remember { mutableStateOf("Relevance") }
    var listView by remember { mutableStateOf(false) }
    var showFilters by remember { mutableStateOf(false) }
    var minRating by remember { mutableStateOf(0.0) }
    var genre by remember { mutableStateOf("") }
    var yearWindow by remember { mutableStateOf("Any") }
    var history by remember { mutableStateOf<List<String>>(emptyList()) }
    val focus = remember { FocusRequester() }

    SearchHistory.ensureLoaded()
    LaunchedEffect(Unit) {
        history = SearchHistory.entries
        runCatching { focus.requestFocus() }
    }
    PlatformBackHandler(enabled = true) { onDismiss() }

    // Suggestions: 500ms of quiet before asking, and never while results are up.
    LaunchedEffect(query, showingResults) {
        val q = query.trim()
        if (showingResults || q.length < 2) {
            suggestions = emptyList()
            return@LaunchedEffect
        }
        kotlinx.coroutines.delay(500)
        loading = true
        suggestions = runCatching { Api.searchMulti(q) }.getOrDefault(emptyList())
        loading = false
    }

    val pool = if (showingResults) results else suggestions
    val kept = remember(pool, filter) { pool.filter { matchesFilter(it, filter) } }
    val visible = remember(kept, sort, minRating, genre, yearWindow) {
        kept
            .filter { minRating == 0.0 || (it.vote_average ?: 0.0) >= minRating }
            .filter { genre.isBlank() || it.genre_ids.any { id -> GenreNames[id] == genre } }
            .filter { inYearWindow(it.year, yearWindow) }
            .let { list ->
                when (sort) {
                    "Year" -> list.sortedWith(compareByDescending { it.year ?: 0 })
                    "Rating" -> list.sortedWith(compareByDescending { it.vote_average ?: 0.0 })
                    "Title" -> list.sortedBy { it.displayTitle.lowercase() }
                    else -> list
                }
            }
    }
    val genresPresent = remember(kept) {
        kept.flatMap { it.genre_ids }.mapNotNull { GenreNames[it] }.distinct().sorted()
    }
    val people = visible.filter { it.isPerson }
    val titles = visible.filterNot { it.isPerson }

    fun runSearch(raw: String) {
        val q = raw.trim()
        if (q.length < 2) return
        query = q
        SearchHistory.remember(q)
        history = SearchHistory.entries
        showingResults = true
        loading = true
    }

    LaunchedEffect(showingResults, query) {
        if (!showingResults) return@LaunchedEffect
        val q = query.trim()
        if (q.length < 2) return@LaunchedEffect
        loading = true
        results = runCatching { Api.searchMulti(q) }.getOrDefault(emptyList())
        loading = false
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .fillMaxSize()
                .background(colors.background),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .weight(1f)
                        .height(56.dp)
                        .clip(RoundedCornerShape(50))
                        .background(colors.card)
                        .border(1.dp, colors.border, RoundedCornerShape(50))
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            tint = colors.mutedForeground,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Box(Modifier.weight(1f)) {
                            if (query.isEmpty()) {
                                Text(
                                    text = "Search movies, TV shows, or people",
                                    color = colors.mutedForeground.copy(alpha = 0.6f),
                                    fontFamily = GeistMono,
                                    fontSize = 15.sp,
                                )
                            }
                            BasicTextField(
                                value = query,
                                onValueChange = {
                                    query = it
                                    showingResults = false
                                },
                                singleLine = true,
                                textStyle = TextStyle(color = colors.foreground, fontFamily = GeistMono, fontSize = 16.sp),
                                cursorBrush = SolidColor(colors.amber500),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = { runSearch(query) }),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focus),
                            )
                        }
                    }
                }
                Spacer(Modifier.width(10.dp))
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(colors.card)
                        .border(1.dp, colors.border, CircleShape)
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close search",
                        tint = colors.mutedForeground,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                SearchTypes.forEach { label ->
                    val on = label == filter
                    Text(
                        text = label,
                        color = if (on) colors.background else colors.mutedForeground,
                        fontFamily = GeistMono,
                        fontSize = 12.sp,
                        fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (on) colors.amber500 else colors.card)
                            .border(1.dp, if (on) colors.amber500 else colors.border, RoundedCornerShape(50))
                            .clickable { filter = label }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }

            if (!showingResults) {
                if (query.isEmpty() && history.isNotEmpty()) {
                    Row(
                        Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "RECENT SEARCHES",
                            color = colors.mutedForeground,
                            fontFamily = GeistMono,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.weight(1f))
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Clear recent searches",
                            tint = colors.mutedForeground,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable {
                                    SearchHistory.clear()
                                    history = emptyList()
                                },
                        )
                    }
                    LazyColumn(Modifier.fillMaxWidth().heightIn(max = 260.dp)) {
                        items(history) { entry ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { runSearch(entry) }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Search,
                                    contentDescription = null,
                                    tint = colors.mutedForeground,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = entry,
                                    color = colors.foreground,
                                    fontFamily = GeistMono,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }

                if (query.length >= 2 && suggestions.isNotEmpty()) {
                    SuggestionList(
                        titles = kept.filterNot { it.isPerson }.take(8),
                        people = kept.filter { it.isPerson }.take(6),
                        onTitle = { item ->
                            SearchHistory.remember(query)
                            onOpenMedia(item.toMediaItem())
                            onDismiss()
                        },
                        onPerson = { person ->
                            SearchHistory.remember(query)
                            onOpenPerson(person.id)
                            onDismiss()
                        },
                    )
                }
            } else {
                ResultsView(
                    count = visible.size,
                    loading = loading,
                    listView = listView,
                    onToggleView = { listView = !listView },
                    sort = sort,
                    onSort = { sort = it },
                    showFilters = showFilters,
                    onToggleFilters = { showFilters = !showFilters },
                    people = people,
                    titles = titles,
                    onOpenMedia = { onOpenMedia(it.toMediaItem()); onDismiss() },
                    onOpenPerson = { onOpenPerson(it.id); onDismiss() },
                    minRating = minRating,
                    onMinRating = { minRating = it },
                    genre = genre,
                    onGenre = { genre = it },
                    genres = genresPresent,
                    yearWindow = yearWindow,
                    onYearWindow = { yearWindow = it },
                    onClearFilters = {
                        minRating = 0.0
                        genre = ""
                        yearWindow = "Any"
                    },
                )
            }

            Spacer(Modifier.weight(1f))

            Text(
                text = if (showingResults) "Pick a result, or close to go back" else "Press search to see all results",
                color = colors.mutedForeground,
                fontFamily = GeistMono,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp, horizontal = 16.dp),
            )
        }
    }
}

@Composable
private fun SuggestionList(
    titles: List<TmdbSearchItem>,
    people: List<TmdbSearchItem>,
    onTitle: (TmdbSearchItem) -> Unit,
    onPerson: (TmdbSearchItem) -> Unit,
) {
    val colors = Beam.colors
    LazyColumn(Modifier.fillMaxWidth()) {
        if (titles.isNotEmpty()) {
            item { SectionHead("Movies & TV") }
            items(titles) { item ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onTitle(item) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .width(34.dp)
                            .height(50.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(colors.card),
                    ) {
                        Api.posterUrl(item.poster_path, "w185")?.let { url ->
                            AsyncImage(model = url, contentDescription = item.displayTitle, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = item.displayTitle,
                            color = colors.foreground,
                            fontFamily = GeistMono,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = (item.year?.toString() ?: "") + (if (item.isTv) "  ·  TV" else "  ·  Film"),
                            color = colors.mutedForeground,
                            fontFamily = GeistMono,
                            fontSize = 11.sp,
                        )
                    }
                }
            }
        }
        if (people.isNotEmpty()) {
            item { SectionHead("People") }
            items(people) { person ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onPerson(person) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(colors.card),
                    ) {
                        Api.posterUrl(person.profile_path, "w185")?.let { url ->
                            AsyncImage(model = url, contentDescription = person.displayTitle, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = person.displayTitle,
                            color = colors.foreground,
                            fontFamily = GeistMono,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = person.known_for_department ?: "Acting",
                            color = colors.mutedForeground,
                            fontFamily = GeistMono,
                            fontSize = 11.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultsView(
    count: Int,
    loading: Boolean,
    listView: Boolean,
    onToggleView: () -> Unit,
    sort: String,
    onSort: (String) -> Unit,
    showFilters: Boolean,
    onToggleFilters: () -> Unit,
    people: List<TmdbSearchItem>,
    titles: List<TmdbSearchItem>,
    onOpenMedia: (TmdbSearchItem) -> Unit,
    onOpenPerson: (TmdbSearchItem) -> Unit,
    minRating: Double,
    onMinRating: (Double) -> Unit,
    genre: String,
    onGenre: (String) -> Unit,
    genres: List<String>,
    yearWindow: String,
    onYearWindow: (String) -> Unit,
    onClearFilters: () -> Unit,
) {
    val colors = Beam.colors
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (loading) "Searching\u2026" else count.toString() + " result" + (if (count == 1) "" else "s"),
                color = colors.mutedForeground,
                fontFamily = GeistMono,
                fontSize = 12.sp,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = if (listView) "Grid" else "List",
                color = colors.amber500,
                fontFamily = GeistMono,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { onToggleView() }.padding(6.dp),
            )
            Text(
                text = "Filters",
                color = if (showFilters) colors.amber500 else colors.mutedForeground,
                fontFamily = GeistMono,
                fontSize = 12.sp,
                modifier = Modifier.clickable { onToggleFilters() }.padding(6.dp),
            )
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(SortOptions) { option ->
                val on = option == sort
                Text(
                    text = option,
                    color = if (on) colors.background else colors.mutedForeground,
                    fontFamily = GeistMono,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (on) colors.amber500 else colors.card)
                        .clickable { onSort(option) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }

        if (showFilters) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                FilterLabel("MIN RATING")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    RatingSteps.forEach { step ->
                        val on = step == minRating
                        Text(
                            text = if (step == 0.0) "Any" else step.toInt().toString() + "+",
                            color = if (on) colors.background else colors.mutedForeground,
                            fontFamily = GeistMono,
                            fontSize = 11.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (on) colors.amber500 else colors.card)
                                .clickable { onMinRating(step) }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                FilterLabel("YEAR")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    YearWindows.forEach { window ->
                        val on = window == yearWindow
                        Text(
                            text = window,
                            color = if (on) colors.background else colors.mutedForeground,
                            fontFamily = GeistMono,
                            fontSize = 11.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (on) colors.amber500 else colors.card)
                                .clickable { onYearWindow(window) }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                        )
                    }
                }

                if (genres.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    FilterLabel("GENRE")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(listOf("") + genres) { name ->
                            val on = name == genre
                            Text(
                                text = if (name.isBlank()) "Any" else name,
                                color = if (on) colors.background else colors.mutedForeground,
                                fontFamily = GeistMono,
                                fontSize = 11.sp,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(if (on) colors.amber500 else colors.card)
                                    .clickable { onGenre(name) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Clear all filters",
                    color = colors.amber500,
                    fontFamily = GeistMono,
                    fontSize = 12.sp,
                    modifier = Modifier.clickable { onClearFilters() }.padding(vertical = 4.dp),
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        if (loading && titles.isEmpty() && people.isEmpty()) {
            Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colors.amber500)
            }
        } else if (titles.isEmpty() && people.isEmpty()) {
            Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = "No results found.",
                    color = colors.mutedForeground,
                    fontFamily = GeistMono,
                    fontSize = 13.sp,
                )
            }
        } else {
            LazyVerticalGrid(
                columns = if (listView) GridCells.Fixed(1) else GridCells.Fixed(3),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth().weight(1f),
            ) {
                if (people.isNotEmpty()) {
                    item {
                        Column {
                            SectionHead("People")
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(people) { person ->
                                    Column(
                                        Modifier
                                            .width(72.dp)
                                            .clickable { onOpenPerson(person) },
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                    ) {
                                        Box(
                                            Modifier
                                                .size(56.dp)
                                                .clip(CircleShape)
                                                .background(colors.card),
                                        ) {
                                            Api.posterUrl(person.profile_path, "w185")?.let { url ->
                                                AsyncImage(model = url, contentDescription = person.displayTitle, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                            }
                                        }
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            text = person.displayTitle,
                                            color = colors.foreground,
                                            fontFamily = GeistMono,
                                            fontSize = 11.sp,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                items(titles) { item ->
                    Column(Modifier.clickable { onOpenMedia(item) }) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(2f / 3f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.card),
                        ) {
                            Api.posterUrl(item.poster_path, "w342")?.let { url ->
                                AsyncImage(model = url, contentDescription = item.displayTitle, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = item.displayTitle,
                            color = colors.foreground,
                            fontFamily = GeistMono,
                            fontSize = 11.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = buildString {
                                item.vote_average?.takeIf { it > 0 }?.let { append("\u2605 ").append(fmtOne(it)) }
                                item.year?.let {
                                    if (isNotEmpty()) append("  ·  ")
                                    append(it)
                                }
                            },
                            color = colors.mutedForeground,
                            fontFamily = GeistMono,
                            fontSize = 10.sp,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHead(text: String) {
    Text(
        text = text.uppercase(),
        color = Beam.colors.mutedForeground,
        fontFamily = GeistMono,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 6.dp),
    )
}

@Composable
private fun FilterLabel(text: String) {
    Text(
        text = text,
        color = Beam.colors.mutedForeground,
        fontFamily = GeistMono,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 6.dp),
    )
}

private fun fmtOne(value: Double): String {
    val rounded = kotlin.math.round(value * 10.0) / 10.0
    return if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()
}

private fun matchesFilter(item: TmdbSearchItem, filter: String): Boolean = when (filter) {
    "Movie" -> item.media_type == "movie"
    "TV" -> item.media_type == "tv"
    "Anime" -> !item.isPerson && item.isAnimation
    "Asian" -> !item.isPerson && item.isAsian
    else -> true
}

private fun inYearWindow(year: Int?, window: String): Boolean {
    if (window == "Any") return true
    val y = year ?: return false
    return when (window) {
        "2024+" -> y >= 2024
        "2020s" -> y in 2020..2029
        "2010s" -> y in 2010..2019
        else -> y < 2010
    }
}
