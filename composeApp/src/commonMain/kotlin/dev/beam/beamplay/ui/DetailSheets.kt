package dev.beam.beamplay.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.beam.beamplay.core.model.DownloadFile
import dev.beam.beamplay.core.model.Link
import dev.beam.beamplay.core.ui.theme.Beam
import dev.beam.beamplay.core.ui.theme.GeistMono
import dev.beam.beamplay.core.ui.theme.Inter
import dev.beam.beamplay.core.ui.theme.PillShape

/**
 * Centred modal card with a dimmed backdrop — matching the website's
 * LinkSelector / DownloadModal rather than a Material bottom sheet.
 */
@Composable
private fun CenterModal(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onDismiss: () -> Unit,
    footer: String? = null,
    content: @Composable () -> Unit,
) {
    val colors = Beam.colors

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xB8000000))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 560.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(colors.card)
                .border(1.dp, colors.borderLight, RoundedCornerShape(20.dp))
                .clickable(enabled = false) {}
                .padding(18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background(colors.muted),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, null, tint = colors.foreground, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = colors.foreground,
                        fontFamily = Inter,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = subtitle,
                        color = colors.mutedForeground,
                        fontFamily = GeistMono,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Box(
                    Modifier.size(32.dp).clip(CircleShape).clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Close, "Close", tint = colors.mutedForeground, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(Modifier.height(14.dp))

            Column(Modifier.verticalScroll(rememberScrollState())) { content() }

            footer?.let {
                Spacer(Modifier.height(14.dp))
                Text(
                    text = it,
                    color = colors.mutedForeground,
                    fontFamily = GeistMono,
                    fontSize = 10.sp,
                    lineHeight = 15.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = Beam.colors
    Text(
        text = label,
        color = if (selected) colors.background else colors.mutedForeground,
        fontFamily = GeistMono,
        fontSize = 12.sp,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        maxLines = 1,
        modifier = Modifier
            .clip(PillShape)
            .background(if (selected) colors.foreground else Color.Transparent)
            .border(1.dp, if (selected) colors.foreground else colors.border, PillShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 6.dp),
    )
}

@Composable
private fun ChipRow(label: String, values: List<String>, active: String, onPick: (String) -> Unit) {
    val colors = Beam.colors
    Column(Modifier.padding(bottom = 12.dp)) {
        Text(
            text = label,
            color = colors.mutedForeground,
            fontFamily = GeistMono,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.1.sp,
        )
        Spacer(Modifier.height(7.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("Any", active.isBlank()) { onPick("") }
            values.forEach { value -> Chip(value, active == value) { onPick(value) } }
        }
    }
}

/**
 * "Choose a stream" — quality + language chips filter the source rows, exactly
 * like the website's LinkSelector.
 */
@Composable
fun SourcesSheet(
    title: String,
    links: List<Link>,
    resolvingUrl: String?,
    onPick: (Link) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = Beam.colors
    var quality by remember { mutableStateOf("") }
    var language by remember { mutableStateOf("") }

    val qualities = remember(links) { links.map { it.quality }.filter { it.isNotBlank() }.distinct() }
    val languages = remember(links) {
        links.flatMap { it.audioLanguageList() }.filter { it.isNotBlank() }.distinct()
    }
    val visible = links.filter { link ->
        (quality.isBlank() || link.quality == quality) &&
            (language.isBlank() || link.audioLanguageList().contains(language))
    }

    CenterModal(
        icon = Icons.Filled.PlayArrow,
        title = "Choose a stream",
        subtitle = title,
        onDismiss = onDismiss,
    ) {
        if (links.isEmpty()) {
            Text(
                text = "No stream sources for this title yet.",
                color = colors.mutedForeground,
                fontFamily = GeistMono,
                fontSize = 12.sp,
            )
            return@CenterModal
        }

        if (qualities.isNotEmpty()) ChipRow("QUALITY", qualities, quality) { quality = it }
        if (languages.isNotEmpty()) ChipRow("LANGUAGE", languages, language) { language = it }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            visible.forEach { link ->
                val langs = link.audioLanguageList().joinToString(", ")
                val busy = resolvingUrl == link.url
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(13.dp))
                        .background(colors.muted)
                        .border(1.dp, colors.borderLight, RoundedCornerShape(13.dp))
                        .clickable(enabled = resolvingUrl == null) { onPick(link) }
                        .padding(horizontal = 13.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(colors.foreground)
                            .padding(horizontal = 7.dp, vertical = 3.dp),
                    ) {
                        Text(
                            text = link.quality.ifBlank { "Source" },
                            color = colors.background,
                            fontFamily = GeistMono,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(Modifier.width(11.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = title,
                            color = colors.foreground,
                            fontFamily = GeistMono,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (langs.isNotBlank()) {
                            Text(
                                text = langs,
                                color = colors.mutedForeground,
                                fontFamily = GeistMono,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    if (busy) {
                        CircularProgressIndicator(color = colors.foreground, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                    } else {
                        Icon(Icons.Filled.PlayArrow, null, tint = colors.mutedForeground, modifier = Modifier.size(18.dp))
                    }
                }
            }
            if (visible.isEmpty()) {
                Text(
                    text = "No source matches those filters.",
                    color = colors.mutedForeground,
                    fontFamily = GeistMono,
                    fontSize = 12.sp,
                )
            }
        }
    }
}

/**
 * "Download" — files grouped by quality, each with a circular action, plus the
 * website's Telegram footer note.
 */
@Composable
fun DownloadsSheet(
    title: String,
    files: List<DownloadFile>,
    loading: Boolean,
    onDismiss: () -> Unit,
) {
    val colors = Beam.colors
    var hint by remember { mutableStateOf(false) }

    CenterModal(
        icon = Icons.Filled.Download,
        title = "Download",
        subtitle = title,
        onDismiss = onDismiss,
        footer = if (hint) {
            "Files are delivered via Telegram. Tap an option to open the BEAM bot and receive your file."
        } else {
            null
        },
    ) {
        when {
            loading -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text("Fetching files", color = colors.mutedForeground, fontFamily = GeistMono, fontSize = 12.sp)
            }

            files.isEmpty() -> Text(
                text = "No download files for this title yet.",
                color = colors.mutedForeground,
                fontFamily = GeistMono,
                fontSize = 12.sp,
            )

            else -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                files.forEach { file ->
                    val langs = file.audioLanguageList().joinToString(", ")
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.muted)
                            .border(1.dp, colors.borderLight, RoundedCornerShape(14.dp))
                            .clickable { hint = true }
                            .padding(horizontal = 13.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(colors.foreground)
                                .padding(horizontal = 7.dp, vertical = 3.dp),
                        ) {
                            Text(
                                text = file.quality.ifBlank { "File" },
                                color = colors.background,
                                fontFamily = GeistMono,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Spacer(Modifier.width(11.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = file.file_name,
                                color = colors.foreground,
                                fontFamily = GeistMono,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            val meta = buildList {
                                if (langs.isNotBlank()) add(langs)
                                if (file.hasSubtitles) add("Subs")
                                file.file_size?.takeIf { it > 0 }?.let { add(humanSize(it)) }
                            }.joinToString("  \u2022  ")
                            if (meta.isNotBlank()) {
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    text = meta,
                                    color = colors.mutedForeground,
                                    fontFamily = GeistMono,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        Box(
                            Modifier.size(34.dp).clip(CircleShape).background(colors.foreground),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.Download, null, tint = colors.background, modifier = Modifier.size(17.dp))
                        }
                    }
                }

                if (!hint) {
                    Text(
                        text = "Files are delivered via Telegram. Tap an option to open the BEAM bot and receive your file.",
                        color = colors.mutedForeground,
                        fontFamily = GeistMono,
                        fontSize = 10.sp,
                        lineHeight = 15.sp,
                    )
                }
            }
        }
    }
}

private fun humanSize(bytes: Long): String {
    val gb = bytes / 1_073_741_824.0
    if (gb >= 1) return "${(gb * 10).toLong() / 10.0} GB"
    val mb = bytes / 1_048_576.0
    return "${(mb * 10).toLong() / 10.0} MB"
}
