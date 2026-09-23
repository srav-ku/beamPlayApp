package app.cinephile.ui

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
import app.cinephile.core.model.DownloadFile
import app.cinephile.core.model.Link
import app.cinephile.core.ui.theme.Beam
import app.cinephile.core.ui.theme.GeistMono
import app.cinephile.core.ui.theme.Inter
import app.cinephile.core.ui.theme.PillShape
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults

/**
 * Centred modal card with a dimmed backdrop — matching the website's
 * LinkSelector / DownloadModal rather than a Material bottom sheet.
 */
@Composable
internal fun CenterModal(
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
 * "Download" - quality and audio chosen from vertical radio lists, then one
 * button.
 *
 * The old version put the options in wrapping chips (where "1080p WEB-DL x264"
 * was clipped to "720") and then listed the same files again underneath. Here the
 * options *are* the selection: full-width rows, nothing truncated, and a single
 * confirmation.
 */
@Composable
fun DownloadsSheet(
    title: String,
    files: List<DownloadFile>,
    loading: Boolean,
    onDismiss: () -> Unit,
    onStart: (DownloadFile) -> Unit = {},
) {
    val colors = Beam.colors
    val qualities = remember(files) { files.map { it.quality }.filter { it.isNotBlank() }.distinct() }
    val audios = remember(files) {
        files.flatMap { it.audioLanguageList() }.filter { it.isNotBlank() }.distinct()
    }
    var quality by remember(files) { mutableStateOf(qualities.firstOrNull() ?: "") }
    var audio by remember(files) { mutableStateOf(audios.firstOrNull() ?: "") }

    CenterModal(
        icon = Icons.Filled.Download,
        title = "Download",
        subtitle = title,
        onDismiss = onDismiss,
    ) {
        when {
            loading -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(color = colors.amber500, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text("Fetching files", color = colors.mutedForeground, fontFamily = GeistMono, fontSize = 12.sp)
            }

            files.isEmpty() -> Text(
                text = "No download files for this title yet.",
                color = colors.mutedForeground,
                fontFamily = GeistMono,
                fontSize = 12.sp,
            )

            else -> Column(Modifier.fillMaxWidth()) {
                if (qualities.isNotEmpty()) {
                    OptionLabel("QUALITY")
                    OptionCard {
                        qualities.forEachIndexed { index, label ->
                            if (index > 0) OptionDivider()
                            val size = files.firstOrNull { it.quality == label }?.file_size
                            OptionRow(
                                label = label,
                                trailing = size?.takeIf { it > 0 }?.let { humanSize(it) },
                                selected = label == quality,
                                onSelect = { quality = label },
                            )
                        }
                    }
                }

                if (audios.isNotEmpty()) {
                    OptionLabel("AUDIO")
                    OptionCard {
                        audios.forEachIndexed { index, label ->
                            if (index > 0) OptionDivider()
                            OptionRow(
                                label = label,
                                trailing = null,
                                selected = label == audio,
                                onSelect = { audio = label },
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(colors.amber500)
                        .clickable {
                            val pick = files.firstOrNull {
                                (quality.isBlank() || it.quality == quality) &&
                                    (audio.isBlank() || it.audioLanguageList().contains(audio))
                            } ?: files.firstOrNull()
                            if (pick != null) onStart(pick)
                        },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Download,
                        contentDescription = null,
                        tint = colors.background,
                        modifier = Modifier.size(19.dp),
                    )
                    Spacer(Modifier.width(9.dp))
                    Text(
                        text = "Start Download",
                        color = colors.background,
                        fontFamily = GeistMono,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Files are delivered through Telegram, so the transfer survives a closed app.",
                    color = colors.mutedForeground,
                    fontFamily = GeistMono,
                    fontSize = 11.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun OptionLabel(text: String) {
    Text(
        text = text,
        color = Beam.colors.mutedForeground,
        fontFamily = GeistMono,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
    )
}

/** Grouping card: page colour inside the sheet's card colour, like the spec. */
@Composable
private fun OptionCard(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Beam.colors.background)
            .border(1.dp, Beam.colors.border, RoundedCornerShape(12.dp)),
    ) {
        content()
    }
}

@Composable
private fun OptionDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(start = 12.dp)
            .height(1.dp)
            .background(Beam.colors.border),
    )
}

@Composable
private fun OptionRow(
    label: String,
    trailing: String?,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    val colors = Beam.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(
                selectedColor = colors.amber500,
                unselectedColor = colors.mutedForeground,
            ),
            modifier = Modifier.size(36.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = label,
            color = colors.foreground,
            fontFamily = GeistMono,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        trailing?.let {
            Spacer(Modifier.width(8.dp))
            Text(
                text = it,
                color = colors.mutedForeground,
                fontFamily = GeistMono,
                fontSize = 13.sp,
                maxLines = 1,
            )
        }
    }
}


private fun humanSize(bytes: Long): String {
    val gb = bytes / 1_073_741_824.0
    if (gb >= 1) return "${(gb * 10).toLong() / 10.0} GB"
    val mb = bytes / 1_048_576.0
    return "${(mb * 10).toLong() / 10.0} MB"
}
