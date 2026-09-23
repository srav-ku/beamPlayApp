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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import app.cinephile.core.ui.theme.Beam
import app.cinephile.core.ui.theme.Fraunces
import app.cinephile.core.ui.theme.GeistMono
import app.cinephile.data.Api
import app.cinephile.data.CollectionsRepo
import app.cinephile.data.EntitlementsState
import app.cinephile.data.Session
import app.cinephile.data.SessionManager
import app.cinephile.data.WatchRecord
import app.cinephile.data.clearImageCache
import app.cinephile.data.clearWatchHistory
import app.cinephile.data.localWatchRecords
import app.cinephile.data.nowMillis
import app.cinephile.data.yearOfInstant
import androidx.compose.material.icons.filled.Movie

/**
 * Profile: who you are, what you have watched, and the switches that matter.
 *
 * Overview, History, Year in Review and Settings are tabs of one screen rather
 * than separate pages, so everything stays a single tap away.
 *
 * All of it is computed from on-device watch records, which means it works for
 * guests with no account - the same reasoning behind collections being local.
 */
@Composable
fun ProfileScreen(
    subtitleSettings: @Composable () -> Unit = {},
    onResumeContinue: (ContinueItem) -> Unit = {},
    onBrowse: () -> Unit = {},
) {
    val colors = Beam.colors
    val session = SessionManager.session.collectAsState().value
    // A real paid plan - never the free fallback, or the badge would read "Premium"
    // to somebody who has not paid for anything.
    val isPremium = EntitlementsState.current?.plan?.code?.let { it != "free" } == true

    var tab by remember { mutableStateOf("Overview") }
    var showPremium by remember { mutableStateOf(false) }
    var showSubtitleSettings by remember { mutableStateOf(false) }
    var confirmClearHistory by remember { mutableStateOf(false) }
    var confirmSignOut by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    var records by remember { mutableStateOf<List<WatchRecord>>(emptyList()) }
    var reload by remember { mutableStateOf(0) }
    var reviewPick by remember { mutableStateOf(-1) }
    var bannerDismissed by remember { mutableStateOf(false) }

    CollectionsRepo.ensureLoaded()
    app.cinephile.data.TitleFlags.ensureLoaded()

    // Re-read on every visit and after a clear, so the numbers never lie.
    LaunchedEffect(reload) { records = runCatching { localWatchRecords() }.getOrDefault(emptyList()) }
    LaunchedEffect(notice) {
        if (notice != null) {
            delay(2400)
            notice = null
        }
    }

    if (showSubtitleSettings) {
        Column(
            Modifier
                .fillMaxSize()
                .background(colors.background),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Back",
                    tint = colors.foreground,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable { showSubtitleSettings = false },
                )
                Spacer(Modifier.width(14.dp))
                Text(
                    text = "Subtitle settings",
                    color = colors.foreground,
                    fontFamily = Fraunces,
                    fontSize = 20.sp,
                )
            }
            subtitleSettings()
        }
        return
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        LazyColumn(contentPadding = PaddingValues(bottom = 104.dp)) {
            item {
                Spacer(Modifier.height(14.dp))
                ProfileHeader(session, isPremium) { showPremium = true }
                Spacer(Modifier.height(24.dp))
                ProfileTabs(current = tab) { tab = it }
                if (!isPremium && !bannerDismissed && tab == "Overview") {
                    Spacer(Modifier.height(16.dp))
                    PremiumBanner(onOpen = { showPremium = true }, onDismiss = { bannerDismissed = true })
                }
                notice?.let { message ->
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = message,
                        color = colors.amber500,
                        fontFamily = GeistMono,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
                Spacer(Modifier.height(18.dp))
            }

            when (tab) {
                "History" -> {
                    // Titles marked watched by hand belong here too, not just played ones.
                    val marks = app.cinephile.data.TitleFlags.manualWatched.filter { mark -> records.none { it.title == mark } }
                    if (marks.isNotEmpty()) {
                        item {
                            Column(Modifier.padding(horizontal = 16.dp)) {
                                SectionHead("Marked watched")
                                Spacer(Modifier.height(8.dp))
                                marks.forEach { mark ->
                                    Text(
                                        text = mark,
                                        color = Beam.colors.foreground,
                                        fontFamily = GeistMono,
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(vertical = 3.dp),
                                    )
                                }
                            }
                        }
                    }
                    if (records.isEmpty()) {
                        item { ProfileEmpty("Nothing watched yet. Press play on any title and it lands here.") }
                    } else {
                        item { ProfileClearRow("History") { confirmClearHistory = true } }
                        items(records, key = { it.title + it.updatedAt }) { record ->
                            HistoryRow(record) { onResumeContinue(record.toContinueItem()) }
                        }
                    }
                }

                "Year in Review" -> {
                    val currentYear = yearOfInstant(nowMillis())
                    val years = records.map { yearOfInstant(it.updatedAt) }.distinct().sortedDescending()
                    yearInReview(records, years, currentYear, reviewPick, onBrowse) { reviewPick = it }
                }

                "Settings" -> {
                    item { PremiumCard(isPremium) { showPremium = true } }
                    item {
                        SettingsCard("Playback & data") {
                            SettingsRow("Subtitle settings", "Style, size and background of captions", divider = true) { showSubtitleSettings = true }
                            SettingsRow("Clear image cache", "Reclaims space used by posters", divider = true) {
                                runCatching { clearImageCache() }
                                notice = "Image cache cleared"
                            }
                            SettingsRow("Clear watch history", "Removes Continue Watching and history", divider = false) {
                                confirmClearHistory = true
                            }
                        }
                    }
                    if (session != null) {
                        item {
                            SettingsCard("Account") {
                                OutlinedAction("Sign out") { confirmSignOut = true }
                            }
                        }
                    }
                    item {
                        Spacer(Modifier.height(26.dp))
                        Text(
                            text = "Cinephile  ·  app.cinephile",
                            color = colors.mutedForeground,
                            fontFamily = GeistMono,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                else -> {
                    val completed = records.count { it.completed }
                    val watchedMs = records.sumOf { if (it.completed) it.durationMs else it.positionMs }
                    if (records.isEmpty()) {
                        // Four giant zeroes look broken; invite instead.
                        item { ProfileWelcome() }
                    } else {
                    item {
                        Column(Modifier.padding(horizontal = 16.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                StatTile("Titles", records.size.toString(), Modifier.weight(1f))
                                StatTile("Watch time", humanDuration(watchedMs), Modifier.weight(1f))
                            }
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                StatTile("Finished", completed.toString(), Modifier.weight(1f))
                                StatTile("In progress", (records.size - completed).toString(), Modifier.weight(1f))
                            }
                        }
                    }
                    }

                    // An empty list has nothing to report, so it is not reported.
                    val collections = CollectionsRepo.ordered().filter { it.items.isNotEmpty() }
                    if (collections.isNotEmpty()) {
                        item {
                            SectionHead("Lists progress", Modifier.padding(horizontal = 16.dp))
                            Spacer(Modifier.height(10.dp))
                        }
                        items(collections, key = { it.id }) { collection ->
                            val total = collection.items.size
                            val seen = CollectionsRepo.watchedCount(collection)
                            ProgressRow(
                                name = collection.name,
                                label = seen.toString() + " of " + total + " watched",
                                fraction = if (total == 0) 0f else seen.toFloat() / total,
                            )
                        }
                    }

                    val recent = records.filterNot { it.completed }.ifEmpty { records }.take(12)
                    if (recent.isNotEmpty()) {
                        item {
                            SectionHead("Recently watched", Modifier.padding(horizontal = 16.dp))
                            Spacer(Modifier.height(10.dp))
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                items(recent) { record ->
                                    RecentCard(record) { onResumeContinue(record.toContinueItem()) }
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                        }
                    }
                }
            }
        }

        if (showPremium) PremiumSheet(onDismiss = { showPremium = false })

        if (confirmClearHistory) {
            ProfileConfirm(
                title = "Clear watch history?",
                body = "Continue Watching and your history will be emptied on this device.",
                confirm = "Clear",
                onDismiss = { confirmClearHistory = false },
            ) {
                runCatching { clearWatchHistory() }
                confirmClearHistory = false
                records = emptyList()
                reload += 1
                notice = "Watch history cleared"
            }
        }

        if (confirmSignOut) {
            ProfileConfirm(
                title = "Sign out?",
                body = "Your lists and history stay on this device.",
                confirm = "Sign out",
                onDismiss = { confirmSignOut = false },
            ) {
                SessionManager.set(null)
                confirmSignOut = false
            }
        }
    }
}

/* ----------------------------- header + tabs ----------------------------- */

@Composable
private fun ProfileHeader(session: Session?, isPremium: Boolean, onUpgrade: () -> Unit) {
    val colors = Beam.colors
    val name = session?.displayName?.takeIf { it.isNotBlank() }
        ?: session?.email?.substringBefore('@')?.takeIf { it.isNotBlank() }
        ?: "Guest"
    val initial = name.take(1).uppercase()

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(colors.card)
            .border(1.dp, colors.border, RoundedCornerShape(22.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(colors.amber500),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = initial,
                color = colors.background,
                fontFamily = Fraunces,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = name,
                color = colors.foreground,
                fontFamily = Fraunces,
                fontSize = 22.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            session?.email?.let { mail ->
                Spacer(Modifier.height(2.dp))
                Text(
                    text = mail,
                    color = colors.mutedForeground,
                    fontFamily = GeistMono,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (isPremium) "Premium" else "Upgrade to Premium",
                color = colors.background,
                fontFamily = GeistMono,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(colors.amber500)
                    .clickable { onUpgrade() }
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            )
        }
    }
}

@Composable
private fun ProfileTabs(current: String, onPick: (String) -> Unit) {
    val colors = Beam.colors
    val tabs = listOf("Overview", "History", "Year in Review", "Settings")
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(tabs) { label ->
            val on = label == current
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
                    .clickable { onPick(label) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
    }
}

/* ------------------------------- overview ------------------------------- */

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    val colors = Beam.colors
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(colors.card)
            .border(1.dp, colors.border, RoundedCornerShape(18.dp))
            .padding(14.dp),
    ) {
        Text(
            text = value,
            color = colors.foreground,
            fontFamily = Fraunces,
            fontSize = 24.sp,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            color = colors.mutedForeground,
            fontFamily = GeistMono,
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun SectionHead(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        color = Beam.colors.mutedForeground,
        fontFamily = Fraunces,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier,
    )
}

@Composable
private fun ProgressRow(name: String, label: String, fraction: Float) {
    val colors = Beam.colors
    Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = name,
                color = colors.foreground,
                fontFamily = GeistMono,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = label,
                color = colors.mutedForeground,
                fontFamily = GeistMono,
                fontSize = 11.sp,
            )
        }
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(50))
                .background(colors.muted),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .height(4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(colors.amber500),
            )
        }
    }
}

@Composable
private fun RecentCard(record: WatchRecord, onClick: () -> Unit) {
    val colors = Beam.colors
    Column(
        Modifier
            .width(104.dp)
            .clickable { onClick() },
    ) {
        Box(
            Modifier
                .width(104.dp)
                .height(156.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(colors.card),
        ) {
            val art = record.art?.takeIf { it.isNotBlank() }
            if (art != null) {
                AsyncImage(
                    model = art,
                    contentDescription = record.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            if (!record.completed && record.progress > 0f) {
                Box(
                    Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(colors.muted),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(record.progress)
                            .height(3.dp)
                            .background(colors.amber500),
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = record.title,
            color = colors.foreground,
            fontFamily = GeistMono,
            fontSize = 11.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/* -------------------------------- history -------------------------------- */

@Composable
private fun ProfileClearRow(label: String, onClear: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = recordsLabel(label),
            color = Beam.colors.mutedForeground,
            fontFamily = GeistMono,
            fontSize = 11.sp,
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = "Clear",
            color = Beam.colors.amber500,
            fontFamily = GeistMono,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clickable { onClear() }
                .padding(horizontal = 16.dp, vertical = 4.dp),
        )
    }
}

private fun recordsLabel(label: String) = label.uppercase()

@Composable
private fun HistoryRow(record: WatchRecord, onResume: () -> Unit) {
    val colors = Beam.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onResume() }
            .padding(horizontal = 16.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .width(52.dp)
                .height(78.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.card),
        ) {
            val art = record.art?.takeIf { it.isNotBlank() }
            if (art != null) {
                AsyncImage(
                    model = art,
                    contentDescription = record.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = record.title,
                color = colors.foreground,
                fontFamily = GeistMono,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            val state = if (record.completed) "Finished" else (record.progress * 100).toInt().toString() + "%"
            Text(
                text = state + "  ·  " + relativeTime(record.updatedAt),
                color = colors.mutedForeground,
                fontFamily = GeistMono,
                fontSize = 11.sp,
            )
        }
        Spacer(Modifier.width(10.dp))
        Icon(
            imageVector = Icons.Filled.PlayArrow,
            contentDescription = "Play",
            tint = colors.amber500,
            modifier = Modifier.size(20.dp),
        )
    }
}

/* ----------------------------- year in review ---------------------------- */

private fun androidx.compose.foundation.lazy.LazyListScope.yearInReview(
    records: List<WatchRecord>,
    years: List<Int>,
    currentYear: Int,
    picked: Int,
    onBrowse: () -> Unit,
    onPick: (Int) -> Unit,
) {
    val year = if (picked == -1) (years.firstOrNull() ?: currentYear) else picked
    val inYear = records.filter { yearOfInstant(it.updatedAt) == year }
    val hours = inYear.sumOf { if (it.completed) it.durationMs else it.positionMs }

    item {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "‹",
                color = Beam.colors.mutedForeground,
                fontFamily = GeistMono,
                fontSize = 18.sp,
                modifier = Modifier
                    .clickable { onPick(year - 1) }
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
            Text(
                text = year.toString(),
                color = Beam.colors.foreground,
                fontFamily = Fraunces,
                fontSize = 28.sp,
            )
            Text(
                text = "›",
                color = Beam.colors.mutedForeground,
                fontFamily = GeistMono,
                fontSize = 18.sp,
                modifier = Modifier
                    .clickable { onPick(year + 1) }
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
        Spacer(Modifier.height(14.dp))
    }

    item {
        Column(Modifier.padding(horizontal = 16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile("Titles", inYear.size.toString(), Modifier.weight(1f))
                StatTile("Hours", humanDuration(hours), Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile("Finished", inYear.count { it.completed }.toString(), Modifier.weight(1f))
                StatTile("Avg", if (inYear.isEmpty()) "-" else humanDuration(hours / inYear.size), Modifier.weight(1f))
            }
            Spacer(Modifier.height(20.dp))
        }
    }

    if (inYear.isEmpty()) {
        item {
            // Fills the space a list would have used, so the screen reads as
            // deliberately empty rather than unfinished.
            Column(
                Modifier
                    .fillMaxWidth()
                    .fillParentMaxHeight(0.55f)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Movie,
                    contentDescription = null,
                    tint = Beam.colors.mutedForeground,
                    modifier = Modifier.size(34.dp),
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Nothing from " + year + " yet.",
                    color = Beam.colors.mutedForeground,
                    fontFamily = GeistMono,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "Browse Movies",
                    color = Beam.colors.background,
                    fontFamily = GeistMono,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Beam.colors.amber500)
                        .clickable { onBrowse() }
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                )
            }
        }
    } else {
        items(inYear, key = { it.title + it.updatedAt }) { record ->
            HistoryRow(record) { }
        }
    }
}

/* -------------------------------- settings ------------------------------- */

@Composable
private fun PremiumCard(isPremium: Boolean, onUpgrade: () -> Unit) {
    val colors = Beam.colors
    val active = isPremium
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(colors.card)
            .border(1.dp, colors.border, RoundedCornerShape(18.dp))
            .padding(16.dp),
    ) {
        Text(
            text = "Cinephile Premium",
            color = colors.foreground,
            fontFamily = Fraunces,
            fontSize = 20.sp,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = if (active) {
                "Active  ·  " + EntitlementsState.planName
            } else {
                "Stream instantly, download at full speed, no ads."
            },
            color = colors.mutedForeground,
            fontFamily = GeistMono,
            fontSize = 12.sp,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = if (active) "View plans" else "Upgrade to Premium",
            color = colors.background,
            fontFamily = GeistMono,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(colors.amber500)
                .clickable { onUpgrade() }
                .padding(horizontal = 18.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun SettingsRow(title: String, subtitle: String, divider: Boolean = false, onClick: () -> Unit) {
    val colors = Beam.colors
    if (divider) {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp)
                .height(1.dp)
                .background(colors.border),
        )
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                color = colors.foreground,
                fontFamily = GeistMono,
                fontSize = 15.sp,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = colors.mutedForeground,
                fontFamily = GeistMono,
                fontSize = 11.sp,
            )
        }
        Text(
            text = "›",
            color = colors.mutedForeground,
            fontFamily = GeistMono,
            fontSize = 16.sp,
        )
    }
}

/* -------------------------------- helpers -------------------------------- */

@Composable
private fun ProfileEmpty(text: String) {
    Text(
        text = text,
        color = Beam.colors.mutedForeground,
        fontFamily = GeistMono,
        fontSize = 13.sp,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun ProfileConfirm(
    title: String,
    body: String,
    confirm: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val colors = Beam.colors
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color(0xE6000000)), contentAlignment = Alignment.Center) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.card)
                    .padding(18.dp),
            ) {
                Text(
                    text = title,
                    color = colors.foreground,
                    fontFamily = GeistMono,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = body,
                    color = colors.mutedForeground,
                    fontFamily = GeistMono,
                    fontSize = 13.sp,
                )
                Spacer(Modifier.height(18.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Cancel",
                        color = colors.mutedForeground,
                        fontFamily = GeistMono,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .clickable { onDismiss() }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = confirm,
                        color = colors.background,
                        fontFamily = GeistMono,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(colors.amber500)
                            .clickable { onConfirm() }
                            .padding(horizontal = 18.dp, vertical = 10.dp),
                    )
                }
            }
        }
    }
}

/** "1h 12m", "48m", "just started" - never a bare number of milliseconds. */
private fun humanDuration(ms: Long): String {
    if (ms <= 0L) return "0m"
    val totalMinutes = ms / 60_000L
    if (totalMinutes < 1L) return "<1m"
    val hours = totalMinutes / 60L
    val minutes = totalMinutes % 60L
    return when {
        hours <= 0L -> minutes.toString() + "m"
        minutes == 0L -> hours.toString() + "h"
        else -> hours.toString() + "h " + minutes + "m"
    }
}

/** Coarse, dependency-free "how long ago" for history rows. */
private fun relativeTime(timestamp: Long): String {
    val diff = nowMillis() - timestamp
    if (diff < 0L) return "just now"
    val minutes = diff / 60_000L
    if (minutes < 1L) return "just now"
    if (minutes < 60L) return minutes.toString() + " min ago"
    val hours = minutes / 60L
    if (hours < 24L) return hours.toString() + "h ago"
    val days = hours / 24L
    if (days < 30L) return days.toString() + "d ago"
    val months = days / 30L
    if (months < 12L) return months.toString() + "mo ago"
    return (months / 12L).toString() + "y ago"
}

/** No watch data yet: say so warmly instead of printing four zeroes. */
@Composable
private fun ProfileWelcome() {
    val colors = Beam.colors
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(colors.card)
            .border(1.dp, colors.border, RoundedCornerShape(18.dp))
            .padding(horizontal = 20.dp, vertical = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Filled.Movie,
            contentDescription = null,
            tint = colors.mutedForeground,
            modifier = Modifier.size(34.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Start watching to see your stats here.",
            color = colors.mutedForeground,
            fontFamily = GeistMono,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
        )
    }
}

/** Dismissible upsell: present, never in the way. */
@Composable
private fun PremiumBanner(onOpen: () -> Unit, onDismiss: () -> Unit) {
    val colors = Beam.colors
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(colors.amber500.copy(alpha = 0.10f))
            .border(1.dp, colors.amber500.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = "Cinephile Premium",
                color = colors.foreground,
                fontFamily = Fraunces,
                fontSize = 17.sp,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = "Stream instantly. Download at full speed. No ads.",
                color = colors.mutedForeground,
                fontFamily = GeistMono,
                fontSize = 11.sp,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "See plans",
                color = colors.background,
                fontFamily = GeistMono,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(colors.amber500)
                    .clickable { onOpen() }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
        Spacer(Modifier.width(8.dp))
        Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = "Dismiss",
            tint = colors.mutedForeground,
            modifier = Modifier
                .size(16.dp)
                .clickable { onDismiss() },
        )
    }
}

/** A titled card that groups related settings rows. */
@Composable
private fun SettingsCard(title: String, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    val colors = Beam.colors
    Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
        Text(
            text = title.uppercase(),
            color = colors.mutedForeground,
            fontFamily = Fraunces,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(colors.card)
                .border(1.dp, colors.border, RoundedCornerShape(18.dp))
                .padding(vertical = 4.dp),
            content = content,
        )
    }
}

/** Full-width outlined action, for things that deserve a real button. */
@Composable
private fun OutlinedAction(label: String, danger: Boolean = false, onClick: () -> Unit) {
    val colors = Beam.colors
    Text(
        text = label,
        color = colors.foreground,
        fontFamily = GeistMono,
        fontSize = 14.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(50))
            .border(1.dp, colors.border, RoundedCornerShape(50))
            .clickable { onClick() }
            .padding(vertical = 12.dp),
    )
}
