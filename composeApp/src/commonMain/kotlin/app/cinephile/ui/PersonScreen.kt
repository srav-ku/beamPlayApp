package app.cinephile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import app.cinephile.core.ui.theme.Beam
import app.cinephile.core.ui.theme.Fraunces
import app.cinephile.core.ui.theme.GeistMono
import app.cinephile.core.ui.theme.Overlay75
import app.cinephile.data.Api
import app.cinephile.data.MediaItem
import app.cinephile.data.PersonCredit
import app.cinephile.data.TmdbPerson

/**
 * A person's page: who they are, then everything they have worked on.
 *
 * Reached by tapping a cast portrait or the "Directed by" credit on a title, and
 * deliberately fed the TMDB id directly - no search step in between.
 *
 * Every credit carries its own reason: a character for acting work, a job for
 * crew work. Section-by-section, anything TMDB does not have is simply not drawn.
 */
@Composable
fun PersonScreen(
    personId: Long,
    onClose: () -> Unit,
    onOpenMedia: (MediaItem) -> Unit,
) {
    val colors = Beam.colors

    var person by remember(personId) { mutableStateOf<TmdbPerson?>(null) }
    var loading by remember(personId) { mutableStateOf(true) }
    var failed by remember(personId) { mutableStateOf(false) }
    var retry by remember(personId) { mutableStateOf(0) }
    var expandedBio by remember(personId) { mutableStateOf(false) }
    var filter by remember(personId) { mutableStateOf("All") }

    PlatformBackHandler(enabled = true) { onClose() }

    LaunchedEffect(personId, retry) {
        loading = true
        failed = false
        val loaded = runCatching { Api.getPerson(personId) }.getOrNull()
        person = loaded
        failed = loaded == null
        loading = false
    }

    // Acting entries first: the role you recognise someone from is usually the
    // acting one, and a crew credit for the same title would just duplicate it.
    val credits: List<Pair<PersonCredit, Boolean>> = remember(person) {
        val loaded = person ?: return@remember emptyList()
        val seen = HashSet<Long>()
        val out = ArrayList<Pair<PersonCredit, Boolean>>()
        loaded.combined_credits?.cast.orEmpty()
            .sortedByDescending { it.popularity ?: 0.0 }
            .forEach { if (it.id != 0L && seen.add(it.id)) out += it to true }
        loaded.combined_credits?.crew.orEmpty()
            .sortedByDescending { it.popularity ?: 0.0 }
            .forEach { if (it.id != 0L && seen.add(it.id)) out += it to false }
        out
    }

    // Only the departments this person actually appears in get a chip.
    val departments: List<String> = remember(credits) {
        val found = LinkedHashSet<String>()
        credits.forEach { (credit, isActing) ->
            if (isActing) found += "Acting"
            else credit.department?.takeIf { it.isNotBlank() }?.let { found += it }
        }
        listOf("All") + found.toList()
    }

    val visible: List<Pair<PersonCredit, Boolean>> = remember(credits, filter) {
        when {
            filter == "All" -> credits
            filter == "Acting" -> credits.filter { it.second }
            else -> credits.filter { !it.second && it.first.department == filter }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 40.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    PersonCloseChip(onClose)
                }
            }

            item { PersonHero(person, loading) }

            val bio = person?.biography?.takeIf { it.isNotBlank() }
            if (bio != null) {
                item {
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        Spacer(Modifier.height(26.dp))
                        PersonLabel("Biography")
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = bio,
                            color = colors.foreground,
                            fontFamily = GeistMono,
                            fontSize = 14.sp,
                            lineHeight = 21.sp,
                            maxLines = if (expandedBio) Int.MAX_VALUE else 4,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (bio.length > 260) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = if (expandedBio) "Show less" else "Show more",
                                color = colors.amber500,
                                fontFamily = GeistMono,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.clickable { expandedBio = !expandedBio },
                            )
                        }
                    }
                }
            }

            if (credits.isNotEmpty()) {
                item {
                    Column {
                        Spacer(Modifier.height(26.dp))
                        Column(Modifier.padding(horizontal = 16.dp)) { PersonLabel("Known For") }
                        Spacer(Modifier.height(12.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(credits.take(20)) { entry ->
                                KnownForCard(entry.first) { onOpenMedia(entry.first.toMediaItem()) }
                            }
                        }
                    }
                }

                item {
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        Spacer(Modifier.height(28.dp))
                        PersonLabel("Filmography")
                        Spacer(Modifier.height(10.dp))
                        if (departments.size > 2) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(departments) { name ->
                                    RoleChip(name, name == filter) { filter = name }
                                }
                            }
                            Spacer(Modifier.height(14.dp))
                        }
                    }
                }

                items(visible.take(200)) { entry ->
                    FilmographyRow(entry.first) { onOpenMedia(entry.first.toMediaItem()) }
                }
            }
        }

        if (loading) {
            CircularProgressIndicator(
                color = colors.amber500,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        if (failed && !loading) {
            Column(
                Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Could not load this person.",
                    color = colors.mutedForeground,
                    fontFamily = GeistMono,
                    fontSize = 13.sp,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Try again",
                    color = colors.amber500,
                    fontFamily = GeistMono,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { retry += 1 },
                )
            }
        }
    }
}

@Composable
private fun PersonCloseChip(onClose: () -> Unit) {
    val colors = Beam.colors
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Overlay75)
            .clickable { onClose() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = "Close",
            tint = colors.foreground,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** 120dp portrait, name in serif, then only the facts TMDB actually has. */
@Composable
private fun PersonHero(person: TmdbPerson?, loading: Boolean) {
    val colors = Beam.colors
    val name = person?.name.orEmpty()
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(colors.card),
            contentAlignment = Alignment.Center,
        ) {
            val portrait = Api.posterUrl(person?.profile_path, "w342")
            if (portrait != null) {
                AsyncImage(
                    model = portrait,
                    contentDescription = name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Text(
                    text = name.take(1).uppercase(),
                    color = colors.mutedForeground,
                    fontFamily = Fraunces,
                    fontSize = 40.sp,
                )
            }
        }

        if (name.isNotBlank()) {
            Spacer(Modifier.height(14.dp))
            Text(
                text = name,
                color = colors.foreground,
                fontFamily = Fraunces,
                fontSize = 28.sp,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
            )
        }

        person?.known_for_department?.takeIf { it.isNotBlank() }?.let { department ->
            Spacer(Modifier.height(4.dp))
            Text(
                text = department.uppercase(),
                color = colors.mutedForeground,
                fontFamily = GeistMono,
                fontSize = 11.sp,
                letterSpacing = 1.sp,
            )
        }

        val born = person?.birthday?.takeIf { it.isNotBlank() }
        val died = person?.deathday?.takeIf { it.isNotBlank() }
        if (born != null) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = if (died != null) "Born $born  ·  Died $died" else "Born $born",
                color = colors.mutedForeground,
                fontFamily = GeistMono,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            )
        }
        person?.place_of_birth?.takeIf { it.isNotBlank() }?.let { place ->
            Spacer(Modifier.height(3.dp))
            Text(
                text = place,
                color = colors.mutedForeground,
                fontFamily = GeistMono,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            )
        }
        if (loading) Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun PersonLabel(text: String) {
    Text(
        text = text.uppercase(),
        color = Beam.colors.mutedForeground,
        fontFamily = Fraunces,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp,
    )
}

/** Landscape credit card: same 16:10 shape, radius and overlay as the app rails. */
@Composable
private fun KnownForCard(credit: PersonCredit, onClick: () -> Unit) {
    val colors = Beam.colors
    Box(
        Modifier
            .width(220.dp)
            .height(138.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(colors.card)
            .clickable { onClick() },
    ) {
        val art = Api.backdropUrl(credit.backdrop_path ?: credit.poster_path, "w780")
        if (art != null) {
            AsyncImage(
                model = art,
                contentDescription = credit.displayTitle,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        // Gradient so the two lines below stay readable over any still.
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Overlay75))),
        )
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp),
        ) {
            Text(
                text = credit.displayTitle,
                color = colors.foreground,
                fontFamily = GeistMono,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val sub = listOfNotNull(credit.role, credit.year?.toString()).joinToString("  ·  ")
            if (sub.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = sub,
                    color = colors.mutedForeground,
                    fontFamily = GeistMono,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun RoleChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = Beam.colors
    Text(
        text = label,
        color = if (selected) colors.background else colors.mutedForeground,
        fontFamily = GeistMono,
        fontSize = 12.sp,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) colors.amber500 else colors.card)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 7.dp),
    )
}

/** One filmography row: poster, title, the reason, and the year it came out. */
@Composable
private fun FilmographyRow(credit: PersonCredit, onClick: () -> Unit) {
    val colors = Beam.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .width(46.dp)
                .height(69.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(colors.card),
        ) {
            val art = Api.posterUrl(credit.poster_path, "w185")
            if (art != null) {
                AsyncImage(
                    model = art,
                    contentDescription = credit.displayTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = credit.displayTitle,
                color = colors.foreground,
                fontFamily = GeistMono,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val sub = listOfNotNull(credit.role, if (credit.isTv) "TV" else "Film").joinToString("  ·  ")
            Spacer(Modifier.height(2.dp))
            Text(
                text = sub,
                color = colors.mutedForeground,
                fontFamily = GeistMono,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        credit.year?.let { year ->
            Spacer(Modifier.width(10.dp))
            Text(
                text = year.toString(),
                color = colors.mutedForeground,
                fontFamily = GeistMono,
                fontSize = 12.sp,
            )
        }
    }
}

/** A credit as something the detail screen understands. */
private fun PersonCredit.toMediaItem(): MediaItem = MediaItem(
    id = 0,
    tmdb_id = id,
    title = displayTitle,
    poster_path = poster_path,
    backdrop_path = backdrop_path ?: poster_path,
    release_year = year,
    first_release_year = year,
    tmdb_rating = vote_average,
    type = if (isTv) "series" else "movie",
)