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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.cinephile.core.ui.theme.Beam
import app.cinephile.core.ui.theme.Fraunces
import app.cinephile.core.ui.theme.GeistMono
import app.cinephile.data.Api
import app.cinephile.data.CineCollection
import app.cinephile.data.CollItem
import app.cinephile.data.CollectionsRepo
import app.cinephile.data.MediaItem
import coil3.compose.AsyncImage

/** Small pill button used across the collections screens. */
@Composable
private fun CollPill(
    label: String,
    primary: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val colors = Beam.colors
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (primary) colors.amber500 else Color.Transparent)
            .border(1.dp, if (primary) Color.Transparent else colors.border, RoundedCornerShape(50))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            text = label,
            color = if (primary) Color(0xFF101014) else colors.foreground,
            fontFamily = GeistMono,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@Composable
private fun CircleChip(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, onClick: () -> Unit) {
    val colors = Beam.colors
    Box(
        Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(colors.muted)
            .border(1.dp, colors.border, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = colors.foreground.copy(alpha = 0.85f), modifier = Modifier.size(16.dp))
    }
}

/** 2x2 poster mosaic used as a collection cover, like the reference lists. */
@Composable
private fun CollectionCover(collection: CineCollection, modifier: Modifier = Modifier) {
    val colors = Beam.colors
    Box(modifier.clip(RoundedCornerShape(18.dp)).background(colors.muted)) {
        if (collection.items.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Empty",
                    color = colors.mutedForeground,
                    fontFamily = GeistMono,
                    fontSize = 12.sp,
                )
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                for (row in 0 until 2) {
                    Row(Modifier.weight(1f)) {
                        for (col in 0 until 2) {
                            val item = collection.items.getOrNull(row * 2 + col)
                            Box(Modifier.weight(1f).fillMaxHeight().background(colors.card)) {
                                Api.backdropUrl(item?.posterPath, "w342")?.let { url ->
                                    AsyncImage(
                                        model = url,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0x00000000), Color(0x99000000)),
                        ),
                    ),
            )
        }
    }
}

/** Library tab: every collection, with covers, counts and progress. */
@Composable
fun CollectionsScreen(onOpenMedia: (MediaItem) -> Unit) {
    val colors = Beam.colors
    CollectionsRepo.ensureLoaded()

    var openId by remember { mutableStateOf<String?>(null) }
    var showNew by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }

    val collections = CollectionsRepo.ordered()
    val open = openId?.let { CollectionsRepo.byId(it) }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Library",
                color = colors.foreground,
                fontFamily = Fraunces,
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.5).sp,
            )
            Spacer(Modifier.weight(1f))
            CollPill(label = "+ New List", primary = false) { showNew = true }
        }

        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            items(collections, key = { it.id }) { collection ->
                val watched = CollectionsRepo.watchedCount(collection)
                val total = collection.items.size
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(26.dp))
                        .background(colors.card)
                        .border(1.dp, colors.border, RoundedCornerShape(26.dp))
                        .clickable { openId = collection.id }
                        .padding(10.dp),
                ) {
                    CollectionCover(
                        collection = collection,
                        modifier = Modifier.fillMaxWidth().height(140.dp),
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = collection.name,
                            color = colors.foreground,
                            fontFamily = GeistMono,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (collection.pinned) {
                            Spacer(Modifier.width(6.dp))
                            Icon(Icons.Filled.Star, contentDescription = "Pinned", tint = colors.amber500, modifier = Modifier.size(13.dp))
                        }
                        if (collection.isDefault) {
                            Spacer(Modifier.width(6.dp))
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(colors.muted)
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                            ) {
                                Text("Default", color = colors.mutedForeground, fontFamily = GeistMono, fontSize = 10.sp)
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = if (total == 0) "Empty" else "$total item" + (if (total == 1) "" else "s"),
                            color = colors.mutedForeground,
                            fontFamily = GeistMono,
                            fontSize = 12.sp,
                        )
                    }
                    if (total > 0) {
                        Spacer(Modifier.height(8.dp))
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(RoundedCornerShape(50))
                                .background(colors.border),
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth(if (total > 0) watched.toFloat() / total.toFloat() else 0f)
                                    .fillMaxHeight()
                                    .background(colors.amber500),
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "$watched of $total watched",
                            color = colors.mutedForeground,
                            fontFamily = GeistMono,
                            fontSize = 11.sp,
                        )
                    }
                }
            }
        }
    }

    if (showNew) {
        Dialog(onDismissRequest = { showNew = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Box(Modifier.fillMaxSize().background(Color(0xE6000000)), contentAlignment = Alignment.Center) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(colors.card)
                        .border(1.dp, colors.border, RoundedCornerShape(24.dp))
                        .padding(20.dp),
                ) {
                    Text("New list", color = colors.foreground, fontFamily = Fraunces, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(12.dp))
                    TextField(
                        value = newName,
                        onValueChange = { newName = it },
                        placeholder = { Text("List name", color = colors.mutedForeground, fontFamily = GeistMono, fontSize = 13.sp) },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = colors.muted,
                            unfocusedContainerColor = colors.muted,
                            focusedTextColor = colors.foreground,
                            unfocusedTextColor = colors.foreground,
                            cursorColor = colors.amber500,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CollPill(label = "Cancel", primary = false) { showNew = false; newName = "" }
                        CollPill(
                            label = "Create",
                            primary = true,
                            enabled = newName.isNotBlank(),
                        ) {
                            CollectionsRepo.create(newName)
                            newName = ""
                            showNew = false
                        }
                    }
                }
            }
        }
    }

    open?.let { collection ->
        CollectionDetailScreen(
            collection = collection,
            onBack = { openId = null },
            onOpenMedia = { item -> openId = null; onOpenMedia(item) },
        )
    }
}

/** Everything inside one collection: view toggle, sort, search, remove, manage. */
@Composable
private fun CollectionDetailScreen(
    collection: CineCollection,
    onBack: () -> Unit,
    onOpenMedia: (MediaItem) -> Unit,
) {
    val colors = Beam.colors
    val fresh = CollectionsRepo.byId(collection.id) ?: collection

    var query by remember { mutableStateOf("") }
    var sort by remember { mutableStateOf("Added") }
    var grid by remember { mutableStateOf(true) }
    var renaming by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var draftName by remember { mutableStateOf(fresh.name) }

    val items = remember(fresh, query, sort) {
        val filtered = if (query.isBlank()) fresh.items
        else fresh.items.filter { it.title.contains(query, ignoreCase = true) }
        when (sort) {
            "Year" -> filtered.sortedByDescending { it.year ?: 0 }
            "Title" -> filtered.sortedBy { it.title.lowercase() }
            "Watched" -> filtered.sortedByDescending { it.watched }
            else -> filtered.sortedByDescending { it.addedAt }
        }
    }

    PlatformBackHandler(enabled = true) { onBack() }

    Dialog(
        onDismissRequest = onBack,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .background(colors.background)
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 12.dp, top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = fresh.name,
                    color = colors.foreground,
                    fontFamily = Fraunces,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (fresh.pinned) {
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Filled.Star, contentDescription = "Pinned", tint = colors.amber500, modifier = Modifier.size(15.dp))
                }
                Spacer(Modifier.weight(1f))
                CircleChip(Icons.Filled.Close, "Close") { onBack() }
            }

            Text(
                text = fresh.items.size.toString() + " item" + (if (fresh.items.size == 1) "" else "s"),
                color = colors.mutedForeground,
                fontFamily = GeistMono,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 16.dp, top = 2.dp),
            )

            Spacer(Modifier.height(12.dp))

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item { CollPill(label = if (fresh.pinned) "Unpin" else "Pin", primary = false) { CollectionsRepo.setPinned(fresh.id, !fresh.pinned) } }
                item { CollPill(label = "Rename", primary = false) { draftName = fresh.name; renaming = true } }
                item { CollPill(label = if (grid) "List view" else "Grid view", primary = false) { grid = !grid } }
                if (!fresh.isDefault) {
                    item { CollPill(label = "Delete", primary = false) { showDelete = true } }
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                listOf("Added", "Year", "Title", "Watched").forEach { option ->
                    val on = sort == option
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (on) colors.amber500 else Color.Transparent)
                            .border(1.dp, if (on) Color.Transparent else colors.border, RoundedCornerShape(50))
                            .clickable { sort = option }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text = option,
                            color = if (on) Color(0xFF101014) else colors.mutedForeground,
                            fontFamily = GeistMono,
                            fontSize = 11.sp,
                            maxLines = 1,
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            TextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search in this list", color = colors.mutedForeground, fontFamily = GeistMono, fontSize = 13.sp) },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = colors.muted,
                    unfocusedContainerColor = colors.muted,
                    focusedTextColor = colors.foreground,
                    unfocusedTextColor = colors.foreground,
                    cursorColor = colors.amber500,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )

            Spacer(Modifier.height(12.dp))

            if (items.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (fresh.items.isEmpty()) {
                            "Nothing saved here yet.\nUse the bookmark on any card, or Add to List on a title."
                        } else {
                            "No matches in this list."
                        },
                        color = colors.mutedForeground,
                        fontFamily = GeistMono,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            } else if (grid) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    gridItems(items, key = { it.tmdbId }) { item ->
                        Column {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(2f / 3f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(colors.card)
                                    .clickable { onOpenMedia(item.toMediaItem()) },
                            ) {
                                Api.backdropUrl(item.posterPath, "w342")?.let { url ->
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
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.55f))
                                        .clickable { CollectionsRepo.removeItem(fresh.id, item.tmdbId) },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(Icons.Filled.Close, contentDescription = "Remove", tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(13.dp))
                                }
                                if (item.watched) {
                                    Box(
                                        Modifier
                                            .align(Alignment.BottomStart)
                                            .padding(6.dp)
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(colors.amber500.copy(alpha = 0.85f)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(Icons.Filled.Check, contentDescription = "Watched", tint = Color(0xFF101014), modifier = Modifier.size(12.dp))
                                    }
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = item.title,
                                color = colors.foreground,
                                fontFamily = GeistMono,
                                fontSize = 12.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    itemsIndexed(items, key = { _, item -> item.tmdbId }) { index, item ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(colors.card)
                                .border(1.dp, colors.border, RoundedCornerShape(18.dp))
                                .clickable { onOpenMedia(item.toMediaItem()) }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = (index + 1).toString(),
                                color = colors.mutedForeground,
                                fontFamily = Fraunces,
                                fontSize = 18.sp,
                                modifier = Modifier.width(26.dp),
                            )
                            Box(
                                Modifier
                                    .width(40.dp)
                                    .aspectRatio(2f / 3f)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(colors.muted),
                            ) {
                                Api.backdropUrl(item.posterPath, "w185")?.let { url ->
                                    AsyncImage(
                                        model = url,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    color = colors.foreground,
                                    fontFamily = GeistMono,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(item.year?.toString() ?: "—", color = colors.mutedForeground, fontFamily = GeistMono, fontSize = 11.sp)
                                    Text(item.type?.uppercase() ?: "", color = colors.mutedForeground, fontFamily = GeistMono, fontSize = 11.sp)
                                    if (item.watched) {
                                        Text("WATCHED", color = colors.amber500, fontFamily = GeistMono, fontSize = 11.sp)
                                    }
                                }
                            }
                            CircleChip(Icons.Filled.Close, "Remove") { CollectionsRepo.removeItem(fresh.id, item.tmdbId) }
                        }
                    }
                }
            }
        }
    }

    if (renaming) {
        Dialog(onDismissRequest = { renaming = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Box(Modifier.fillMaxSize().background(Color(0xE6000000)), contentAlignment = Alignment.Center) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(colors.card)
                        .border(1.dp, colors.border, RoundedCornerShape(24.dp))
                        .padding(20.dp),
                ) {
                    Text("Rename list", color = colors.foreground, fontFamily = Fraunces, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(12.dp))
                    TextField(
                        value = draftName,
                        onValueChange = { draftName = it },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = colors.muted,
                            unfocusedContainerColor = colors.muted,
                            focusedTextColor = colors.foreground,
                            unfocusedTextColor = colors.foreground,
                            cursorColor = colors.amber500,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CollPill(label = "Cancel", primary = false) { renaming = false }
                        CollPill(label = "Save", primary = true, enabled = draftName.isNotBlank()) {
                            CollectionsRepo.rename(fresh.id, draftName)
                            renaming = false
                        }
                    }
                }
            }
        }
    }

    if (showDelete) {
        Dialog(onDismissRequest = { showDelete = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Box(Modifier.fillMaxSize().background(Color(0xE6000000)), contentAlignment = Alignment.Center) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(colors.card)
                        .border(1.dp, colors.border, RoundedCornerShape(24.dp))
                        .padding(20.dp),
                ) {
                    Text("Delete \u201C" + fresh.name + "\u201D?", color = colors.foreground, fontFamily = Fraunces, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "The list is removed from this device. The titles themselves stay in the catalogue.",
                        color = colors.mutedForeground,
                        fontFamily = GeistMono,
                        fontSize = 12.sp,
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CollPill(label = "Cancel", primary = false) { showDelete = false }
                        CollPill(label = "Delete", primary = true) {
                            CollectionsRepo.delete(fresh.id)
                            showDelete = false
                            onBack()
                        }
                    }
                }
            }
        }
    }
}

/** Turns a saved entry back into something the detail screen understands. */
fun CollItem.toMediaItem(): MediaItem = MediaItem(
    id = mediaId,
    tmdb_id = tmdbId,
    title = title,
    poster_path = posterPath,
    release_year = year,
    first_release_year = year,
    type = type,
)

/**
 * "Add to Collections" picker: tick several lists at once, or create one inline.
 * Used from the detail page and from Browse's multi-select.
 */
@Composable
fun CollectionPickerModal(
    items: List<CollItem>,
    onDismiss: () -> Unit,
) {
    val colors = Beam.colors
    CollectionsRepo.ensureLoaded()

    var selected by remember { mutableStateOf<List<String>>(emptyList()) }
    var newName by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color(0xE6000000)), contentAlignment = Alignment.Center) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(colors.card)
                    .border(1.dp, colors.border, RoundedCornerShape(24.dp))
                    .padding(20.dp),
            ) {
                Text(
                    text = "Add " + items.size + " item" + (if (items.size == 1) "" else "s") + " to Collections",
                    color = colors.foreground,
                    fontFamily = Fraunces,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                )

                Spacer(Modifier.height(14.dp))

                Column(Modifier.heightIn(max = 300.dp)) {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(CollectionsRepo.ordered(), key = { it.id }) { collection ->
                            val on = selected.contains(collection.id)
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (on) colors.amber500.copy(alpha = 0.12f) else Color.Transparent)
                                    .border(1.dp, if (on) colors.amber500.copy(alpha = 0.55f) else colors.border, RoundedCornerShape(16.dp))
                                    .clickable {
                                        selected = if (on) selected - collection.id else selected + collection.id
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    Modifier
                                        .size(20.dp)
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(if (on) colors.amber500 else Color.Transparent)
                                        .border(1.dp, if (on) Color.Transparent else colors.border, RoundedCornerShape(5.dp)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (on) {
                                        Icon(Icons.Filled.Check, contentDescription = null, tint = Color(0xFF101014), modifier = Modifier.size(14.dp))
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = collection.name,
                                    color = colors.foreground,
                                    fontFamily = GeistMono,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    text = collection.items.size.toString(),
                                    color = colors.mutedForeground,
                                    fontFamily = GeistMono,
                                    fontSize = 12.sp,
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextField(
                        value = newName,
                        onValueChange = { newName = it },
                        placeholder = { Text("New list name", color = colors.mutedForeground, fontFamily = GeistMono, fontSize = 13.sp) },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = colors.muted,
                            unfocusedContainerColor = colors.muted,
                            focusedTextColor = colors.foreground,
                            unfocusedTextColor = colors.foreground,
                            cursorColor = colors.amber500,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                        modifier = Modifier.weight(1f),
                    )
                    CircleChip(Icons.Filled.Add, "Create list") {
                        if (newName.isNotBlank()) {
                            val created = CollectionsRepo.create(newName)
                            selected = selected + created.id
                            newName = ""
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CollPill(label = "Cancel", primary = false, onClick = onDismiss)
                    CollPill(
                        label = if (selected.isEmpty()) "Select a list" else "Add to " + selected.size + " list" + (if (selected.size == 1) "" else "s"),
                        primary = true,
                        enabled = selected.isNotEmpty(),
                    ) {
                        CollectionsRepo.addItems(selected, items)
                        onDismiss()
                    }
                }
            }
        }
    }
}
