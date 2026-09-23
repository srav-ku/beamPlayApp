package app.cinephile.ui

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material.icons.filled.Folder
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.layout.heightIn

/* ------------------------------------------------------------------------- */
/* Building blocks                                                            */
/* ------------------------------------------------------------------------- */

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
            color = when {
                !enabled -> colors.mutedForeground
                primary -> Color(0xFF101014)
                else -> colors.foreground
            },
            fontFamily = GeistMono,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@Composable
private fun CircleChip(icon: ImageVector, description: String, onClick: () -> Unit) {
    val colors = Beam.colors
    Box(
        Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(colors.muted)
            .border(1.dp, colors.border, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = colors.foreground.copy(alpha = 0.85f), modifier = Modifier.size(16.dp))
    }
}

/**
 * The library glyph from the design reference: four bars of rising length,
 * drawn by hand so it matches exactly rather than approximating an icon pack.
 */
@Composable
private fun LibraryGlyph(size: Dp, tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val unit = this.size.minDimension / 24f
        val stroke = 2f * unit
        // "m16 6 4 14" - the slanting bar
        drawLine(
            color = tint,
            start = Offset(x = 16f * unit, y = 6f * unit),
            end = Offset(x = 20f * unit, y = 20f * unit),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(tint, Offset(12f * unit, 6f * unit), Offset(12f * unit, 20f * unit), stroke, StrokeCap.Round)
        drawLine(tint, Offset(8f * unit, 8f * unit), Offset(8f * unit, 20f * unit), stroke, StrokeCap.Round)
        drawLine(tint, Offset(4f * unit, 4f * unit), Offset(4f * unit, 20f * unit), stroke, StrokeCap.Round)
    }
}

/** Dashed placeholder used when a list has nothing in it yet. */
@Composable
private fun EmptyListBox(message: String, modifier: Modifier = Modifier) {
    val colors = Beam.colors
    val dash = remember { PathEffect.dashPathEffect(floatArrayOf(16f, 14f), 0f) }
    Box(
        modifier
            .clip(RoundedCornerShape(22.dp))
            .drawBehind {
                drawRoundRect(
                    color = colors.border,
                    cornerRadius = CornerRadius(22.dp.toPx(), 22.dp.toPx()),
                    style = Stroke(width = 2f, pathEffect = dash),
                )
            }
            .padding(14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(colors.amber500.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center,
            ) {
                LibraryGlyph(size = 22.dp, tint = colors.amber500.copy(alpha = 0.65f))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = message,
                color = colors.mutedForeground,
                fontFamily = GeistMono,
                fontSize = 10.sp,
                lineHeight = 13.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Mosaic cover, or the dashed empty state when the list holds nothing. */
@Composable
private fun CollectionCover(collection: CineCollection, modifier: Modifier = Modifier) {
    val colors = Beam.colors
    if (collection.items.isEmpty()) {
        EmptyListBox(
            message = "This list is empty. Search for movies to add them.",
            modifier = modifier,
        )
        return
    }
    Box(modifier.clip(RoundedCornerShape(18.dp)).background(colors.muted)) {
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
                .background(Brush.verticalGradient(listOf(Color(0x00000000), Color(0x99000000)))),
        )
    }
}

/** Horizontal switcher: a pill container with one amber segment. */
@Composable
private fun SegmentedChips(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    scrollable: Boolean = false,
) {
    val colors = Beam.colors
    val container: @Composable () -> Unit = {
        Row(
            Modifier
                .clip(RoundedCornerShape(50))
                .background(colors.card)
                .border(1.dp, colors.border, RoundedCornerShape(50))
                .padding(3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            options.forEach { option ->
                val on = option == selected
                Box(
                    Modifier
                        .height(30.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (on) colors.amber500 else Color.Transparent)
                        .clickable { onSelect(option) }
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = option,
                        color = if (on) Color(0xFF101014) else colors.mutedForeground,
                        fontFamily = GeistMono,
                        fontSize = 11.sp,
                        fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                    )
                }
            }
        }
    }
    if (scrollable) {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { container() }
        }
    } else {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) { container() }
    }
}

/* ------------------------------------------------------------------------- */
/* Library: the grid of collections                                           */
/* ------------------------------------------------------------------------- */

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

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            gridItems(collections, key = { it.id }) { collection ->
                val watched = CollectionsRepo.watchedCount(collection)
                val total = collection.items.size
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(colors.card)
                        .border(1.dp, colors.border, RoundedCornerShape(22.dp))
                        .clickable { openId = collection.id }
                        .padding(8.dp),
                ) {
                    CollectionCover(
                        collection = collection,
                        modifier = Modifier.fillMaxWidth().height(118.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (collection.pinned) {
                            Icon(Icons.Filled.Star, contentDescription = "Pinned", tint = colors.amber500, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(4.dp))
                        }
                        Text(
                            text = collection.name,
                            color = colors.foreground,
                            fontFamily = GeistMono,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (total == 0) "Empty" else "$total item" + (if (total == 1) "" else "s"),
                            color = colors.mutedForeground,
                            fontFamily = GeistMono,
                            fontSize = 11.sp,
                            maxLines = 1,
                        )
                        if (collection.isDefault) {
                            Spacer(Modifier.width(6.dp))
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(colors.muted)
                                    .padding(horizontal = 6.dp, vertical = 1.dp),
                            ) {
                                Text("Default", color = colors.mutedForeground, fontFamily = GeistMono, fontSize = 9.sp)
                            }
                        }
                    }
                    if (total > 0) {
                        Spacer(Modifier.height(6.dp))
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(RoundedCornerShape(50))
                                .background(colors.border),
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth(watched.toFloat() / total.toFloat())
                                    .fillMaxHeight()
                                    .background(colors.amber500),
                            )
                        }
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = "$watched/$total watched",
                            color = colors.mutedForeground,
                            fontFamily = GeistMono,
                            fontSize = 10.sp,
                        )
                    }
                }
            }
        }
    }

    if (showNew) {
        Dialog(onDismissRequest = { showNew = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Box(
            Modifier
                .fillMaxSize()
                .background(Color(0xE6000000))
                // Lifts the card above the keyboard so the new-list field stays visible.
                .imePadding(),
            contentAlignment = Alignment.Center,
        ) {
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
                        CollPill(label = "Create", primary = true, enabled = newName.isNotBlank()) {
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

/* ------------------------------------------------------------------------- */
/* One collection                                                             */
/* ------------------------------------------------------------------------- */

@Composable
private fun CollectionDetailScreen(
    collection: CineCollection,
    onBack: () -> Unit,
    onOpenMedia: (MediaItem) -> Unit,
) {
    val colors = Beam.colors
    val fresh = CollectionsRepo.byId(collection.id) ?: collection

    var query by remember { mutableStateOf("") }
    var sort by remember { mutableStateOf("Custom order") }
    var filter by remember { mutableStateOf("All") }
    var view by remember { mutableStateOf("Grid") }
    var renaming by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var draftName by remember { mutableStateOf(fresh.name) }

    val items = remember(fresh, query, sort, filter) {
        var working = fresh.items
        if (query.isNotBlank()) working = working.filter { it.title.contains(query, ignoreCase = true) }
        working = when (filter) {
            "Watched" -> working.filter { it.watched }
            "Not watched" -> working.filterNot { it.watched }
            else -> working
        }
        when (sort) {
            "Year" -> working.sortedByDescending { it.year ?: 0 }
            "Title" -> working.sortedBy { it.title.lowercase() }
            else -> working
        }
    }

    PlatformBackHandler(enabled = true) { onBack() }

    Dialog(onDismissRequest = onBack, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .fillMaxSize()
                .background(colors.background)
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(colors.amber500.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center,
                ) {
                    LibraryGlyph(size = 22.dp, tint = colors.amber500.copy(alpha = 0.65f))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = fresh.name,
                            color = colors.foreground,
                            fontFamily = Fraunces,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (fresh.pinned) {
                            Spacer(Modifier.width(6.dp))
                            Icon(Icons.Filled.Star, contentDescription = "Pinned", tint = colors.amber500, modifier = Modifier.size(14.dp))
                        }
                    }
                    Text(
                        text = fresh.items.size.toString() + " item" + (if (fresh.items.size == 1) "" else "s"),
                        color = colors.mutedForeground,
                        fontFamily = GeistMono,
                        fontSize = 12.sp,
                    )
                }
                Spacer(Modifier.width(12.dp))
                CircleChip(Icons.Filled.Close, "Close") { onBack() }
            }

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item { CollPill(label = if (fresh.pinned) "Unpin" else "Pin", primary = false) { CollectionsRepo.setPinned(fresh.id, !fresh.pinned) } }
                // Defaults stay as they are: their names are part of the app.
                if (!fresh.isDefault) {
                    item { CollPill(label = "Rename", primary = false) { draftName = fresh.name; renaming = true } }
                    item { CollPill(label = "Delete", primary = false) { showDelete = true } }
                }
            }

            Spacer(Modifier.height(14.dp))

            SegmentedChips(options = listOf("Grid", "List"), selected = view, onSelect = { view = it })

            Spacer(Modifier.height(10.dp))

            SegmentedChips(
                options = listOf("Custom order", "Year", "Title"),
                selected = sort,
                onSelect = { sort = it },
                scrollable = true,
            )

            Spacer(Modifier.height(8.dp))

            SegmentedChips(
                options = listOf("All", "Watched", "Not watched"),
                selected = filter,
                onSelect = { filter = it },
                scrollable = true,
            )

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

            if (fresh.items.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    EmptyListBox(
                        message = "This list is empty. Search for movies to add them.",
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            } else if (items.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No matches with these filters.",
                        color = colors.mutedForeground,
                        fontFamily = GeistMono,
                        fontSize = 13.sp,
                    )
                }
            } else if (view == "Grid") {
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
                                    Text(item.year?.toString() ?: "\u2014", color = colors.mutedForeground, fontFamily = GeistMono, fontSize = 11.sp)
                                    Text(item.type?.uppercase() ?: "", color = colors.mutedForeground, fontFamily = GeistMono, fontSize = 11.sp)
                                    if (item.watched) {
                                        Text("WATCHED", color = colors.amber500, fontFamily = GeistMono, fontSize = 11.sp)
                                    }
                                }
                            }
                            if (sort == "Custom order") {
                                Column {
                                    Box(
                                        Modifier.size(26.dp).clickable { CollectionsRepo.moveItem(fresh.id, item.tmdbId, -1) },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Move up", tint = colors.mutedForeground, modifier = Modifier.size(18.dp))
                                    }
                                    Box(
                                        Modifier.size(26.dp).clickable { CollectionsRepo.moveItem(fresh.id, item.tmdbId, 1) },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Move down", tint = colors.mutedForeground, modifier = Modifier.size(18.dp))
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
            Box(
            Modifier
                .fillMaxSize()
                .background(Color(0xE6000000))
                // Lifts the card above the keyboard so the new-list field stays visible.
                .imePadding(),
            contentAlignment = Alignment.Center,
        ) {
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
            Box(
            Modifier
                .fillMaxSize()
                .background(Color(0xE6000000))
                // Lifts the card above the keyboard so the new-list field stays visible.
                .imePadding(),
            contentAlignment = Alignment.Center,
        ) {
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
 * "Add to Lists" picker: tick several lists at once, or create one inline.
 * Reached from the detail page's My List button (and later from Browse's
 * multi-select).
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
        Box(
            Modifier
                .fillMaxSize()
                .background(Color(0xE6000000))
                // Lifts the card above the keyboard so the new-list field stays visible.
                .imePadding(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.card)
                    .padding(16.dp),
            ) {
                // Plain sans header: the serif belongs to editorial titles, not chrome.
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Add " + items.size + " item" + (if (items.size == 1) "" else "s") + " to Lists",
                        color = colors.foreground,
                        fontFamily = GeistMono,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = colors.foreground.copy(alpha = 0.7f),
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { onDismiss() },
                    )
                }

                Spacer(Modifier.height(14.dp))

                Column(Modifier.heightIn(max = 300.dp)) {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(CollectionsRepo.ordered(), key = { it.id }) { collection ->
                            val on = selected.contains(collection.id)
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (on) colors.amber500.copy(alpha = 0.1f) else colors.background)
                                    .border(1.dp, if (on) colors.amber500 else colors.border, RoundedCornerShape(12.dp))
                                    .clickable {
                                        selected = if (on) selected - collection.id else selected + collection.id
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = on,
                                    onCheckedChange = null,
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = colors.amber500,
                                        checkmarkColor = colors.background,
                                        uncheckedColor = colors.mutedForeground,
                                    ),
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(Modifier.width(10.dp))
                                Icon(
                                    imageVector = Icons.Filled.Folder,
                                    contentDescription = null,
                                    tint = colors.mutedForeground,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(Modifier.width(8.dp))
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
                    Box(
                        Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.background)
                            .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (newName.isEmpty()) {
                            Text(
                                text = "New list name",
                                color = colors.mutedForeground,
                                fontFamily = GeistMono,
                                fontSize = 14.sp,
                            )
                        }
                        BasicTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            singleLine = true,
                            textStyle = TextStyle(color = colors.foreground, fontFamily = GeistMono, fontSize = 14.sp),
                            cursorBrush = SolidColor(colors.amber500),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    // The same 48dp square as the field, so the pair reads as one control.
                    Box(
                        Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (newName.isNotBlank()) colors.amber500 else colors.muted)
                            .clickable(enabled = newName.isNotBlank()) {
                                val created = CollectionsRepo.create(newName)
                                selected = selected + created.id
                                newName = ""
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Create list",
                            tint = colors.background,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

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
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (selected.isEmpty()) colors.muted else colors.amber500)
                            .clickable(enabled = selected.isNotEmpty()) {
                                CollectionsRepo.addItems(selected, items)
                                onDismiss()
                            }
                            .padding(horizontal = 18.dp, vertical = 10.dp),
                    ) {
                        Text(
                            text = if (selected.isEmpty()) {
                                "Add to Lists"
                            } else {
                                "Add to " + selected.size + " List" + (if (selected.size == 1) "" else "s")
                            },
                            color = if (selected.isEmpty()) colors.mutedForeground else colors.background,
                            fontFamily = GeistMono,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

