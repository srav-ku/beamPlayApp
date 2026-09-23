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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import app.cinephile.core.model.DownloadFile
import app.cinephile.core.model.Episode
import app.cinephile.core.model.Link
import app.cinephile.core.model.Season
import app.cinephile.core.network.servicesOrNull
import app.cinephile.core.ui.theme.Beam
import app.cinephile.core.ui.theme.Fraunces
import app.cinephile.core.ui.theme.GeistMono
import app.cinephile.data.Api
import app.cinephile.data.MediaItem
import app.cinephile.ui.player.PlayerSubtitle

/**
 * Seasons and episodes for a series.
 *
 * A series is not a movie with a longer runtime - the unit people act on is the
 * episode. So every episode card carries its own Stream and Download, and each of
 * those opens exactly the popup a film opens: the same source chooser with its
 * quality and audio filters, the same download sheet, and the same premium sheet
 * when the server says this account may not stream it yet.
 */
@Composable
fun SeriesSection(
    item: MediaItem,
    onUpgrade: () -> Unit,
    onPlay: (String, List<PlayerSubtitle>, String) -> Unit,
) {
    val colors = Beam.colors
    val scope = rememberCoroutineScope()

    var seasons by remember(item.id) { mutableStateOf<List<Season>>(emptyList()) }
    var episodes by remember(item.id) { mutableStateOf<List<Episode>>(emptyList()) }
    var selected by remember(item.id) { mutableStateOf<Long?>(null) }
    var loadingSeasons by remember(item.id) { mutableStateOf(true) }
    var loadingEpisodes by remember(item.id) { mutableStateOf(false) }

    // One episode at a time can be resolving, and one popup can be open.
    var resolvingId by remember { mutableStateOf<Long?>(null) }
    var streamFor by remember { mutableStateOf<Episode?>(null) }
    var streamLinks by remember { mutableStateOf<List<Link>>(emptyList()) }
    var downloadFor by remember { mutableStateOf<Episode?>(null) }
    var downloadFiles by remember { mutableStateOf<List<DownloadFile>>(emptyList()) }

    val seasonNo = seasons.firstOrNull { it.id == selected }?.season_number ?: 0

    LaunchedEffect(item.id) {
        loadingSeasons = true
        val loaded = runCatching { servicesOrNull?.beamApi?.getSeasons(item.id)?.items.orEmpty() }
            .getOrDefault(emptyList())
            .sortedBy { it.season_number }
        seasons = loaded
        selected = loaded.firstOrNull()?.id
        loadingSeasons = false
    }

    LaunchedEffect(selected) {
        val seasonId = selected ?: return@LaunchedEffect
        loadingEpisodes = true
        episodes = runCatching { servicesOrNull?.beamApi?.getEpisodes(seasonId)?.items.orEmpty() }
            .getOrDefault(emptyList())
            .sortedBy { it.episode_number }
        loadingEpisodes = false
    }

    Column(Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(24.dp))
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SeriesLabel("Seasons & Episodes")
            Spacer(Modifier.weight(1f))
            if (episodes.isNotEmpty()) {
                Text(
                    text = episodes.size.toString() + " episode" + (if (episodes.size == 1) "" else "s"),
                    color = colors.mutedForeground,
                    fontFamily = GeistMono,
                    fontSize = 11.sp,
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        if (loadingSeasons) {
            Busy("Loading seasons")
            return@Column
        }

        if (seasons.isEmpty()) {
            Text(
                text = "Episodes for this series are not in the library yet.",
                color = colors.mutedForeground,
                fontFamily = GeistMono,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            return@Column
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(seasons, key = { it.id }) { season ->
                val on = season.id == selected
                Text(
                    text = if (season.name.isNotBlank()) season.name else "Season " + season.season_number,
                    color = if (on) colors.background else colors.mutedForeground,
                    fontFamily = GeistMono,
                    fontSize = 12.sp,
                    fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (on) colors.amber500 else colors.card)
                        .border(1.dp, if (on) colors.amber500 else colors.border, RoundedCornerShape(50))
                        .clickable { if (season.id != selected) selected = season.id }
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        if (loadingEpisodes) {
            Busy("Loading episodes")
        } else if (episodes.isEmpty()) {
            Text(
                text = "No episodes listed for this season.",
                color = colors.mutedForeground,
                fontFamily = GeistMono,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        } else {
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                episodes.forEach { episode ->
                    EpisodeCard(
                        episode = episode,
                        seasonNo = seasonNo,
                        busy = resolvingId == episode.id,
                        onStream = {
                            resolvingId = episode.id
                            scope.launch {
                                val links = runCatching {
                                    servicesOrNull?.beamApi?.getEpisodeLinks(episode.id)?.items.orEmpty()
                                }.getOrDefault(emptyList())
                                resolvingId = null
                                if (links.isEmpty()) {
                                    // The server refused: this is the premium gate talking.
                                    onUpgrade()
                                } else {
                                    streamLinks = links
                                    streamFor = episode
                                }
                            }
                        },
                        onDownload = {
                            resolvingId = episode.id
                            scope.launch {
                                val files = runCatching {
                                    servicesOrNull?.beamApi?.getDownloadOptions("episode", episode.id)?.items.orEmpty()
                                }.getOrDefault(emptyList())
                                resolvingId = null
                                if (files.isEmpty()) {
                                    downloadFiles = emptyList()
                                    downloadFor = episode
                                } else {
                                    downloadFiles = files
                                    downloadFor = episode
                                }
                            }
                        },
                    )
                }
            }
        }
    }

    // ---- the same popups the movie page opens, scoped to this episode ----
    val streaming = streamFor
    if (streaming != null) {
        SourcesSheet(
            title = "S" + seasonNo + " E" + streaming.episode_number,
            links = streamLinks,
            resolvingUrl = null,
            onPick = { link ->
                playEpisodeLink(
                    scope = scope,
                    link = link,
                    label = "S" + seasonNo + " E" + streaming.episode_number,
                    onPlay = onPlay,
                    onDone = { streamFor = null },
                )
            },
            onDismiss = { streamFor = null },
        )
    }

    val downloading = downloadFor
    if (downloading != null) {
        DownloadsSheet(
            title = "S" + seasonNo + " E" + downloading.episode_number,
            files = downloadFiles,
            loading = false,
            onDismiss = { downloadFor = null },
        )
    }
}

/** Still, number, name, synopsis, runtime - and its own two actions. */
@Composable
private fun EpisodeCard(
    episode: Episode,
    seasonNo: Int,
    busy: Boolean,
    onStream: () -> Unit,
    onDownload: () -> Unit,
) {
    val colors = Beam.colors
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.card)
            .border(1.dp, colors.border, RoundedCornerShape(14.dp))
            .padding(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .width(116.dp)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.muted),
            ) {
                val still = Api.backdropUrl(episode.thumbnail_path, "w300")
                if (still != null) {
                    AsyncImage(
                        model = still,
                        contentDescription = episode.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = colors.mutedForeground,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "S" + seasonNo + "  ·  E" + episode.episode_number,
                    color = colors.amber500,
                    fontFamily = GeistMono,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = episode.name.ifBlank { "Episode " + episode.episode_number },
                    color = colors.foreground,
                    fontFamily = GeistMono,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                episode.overview?.takeIf { it.isNotBlank() }?.let { synopsis ->
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = synopsis,
                        color = colors.mutedForeground,
                        fontFamily = GeistMono,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                episode.runtime?.takeIf { it > 0 }?.let { minutes ->
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = minutes.toString() + " min",
                        color = colors.mutedForeground,
                        fontFamily = GeistMono,
                        fontSize = 10.sp,
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            EpisodeButton(
                label = if (busy) "Loading…" else "Stream",
                icon = Icons.Filled.PlayArrow,
                primary = true,
                modifier = Modifier.weight(1f),
                onClick = onStream,
            )
            EpisodeButton(
                label = "Download",
                icon = Icons.Filled.Download,
                primary = false,
                modifier = Modifier.weight(1f),
                onClick = onDownload,
            )
        }
    }
}

@Composable
private fun EpisodeButton(
    label: String,
    icon: ImageVector,
    primary: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val colors = Beam.colors
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(if (primary) colors.amber500 else colors.muted)
            .border(1.dp, if (primary) colors.amber500 else colors.border, RoundedCornerShape(50))
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (primary) colors.background else colors.foreground,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(7.dp))
        Text(
            text = label,
            color = if (primary) colors.background else colors.foreground,
            fontFamily = GeistMono,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun Busy(text: String) {
    Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(color = Beam.colors.amber500, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, color = Beam.colors.mutedForeground, fontFamily = GeistMono, fontSize = 12.sp)
    }
}

/** Resolves the manifest for a chosen episode link, then hands it to the player. */
private fun playEpisodeLink(
    scope: CoroutineScope,
    link: Link,
    label: String,
    onPlay: (String, List<PlayerSubtitle>, String) -> Unit,
    onDone: () -> Unit,
) {
    scope.launch {
        val meta = runCatching { servicesOrNull?.vidaraApi?.getStreamMetadata(link.url) }.getOrNull()
        val url = meta?.streaming_url?.takeIf { it.isNotBlank() } ?: link.url
        val subs = meta?.subtitles?.map { sub ->
            val absolute = if (sub.file_path.startsWith("http")) sub.file_path
            else (servicesOrNull?.vidaraApi?.originOf(link.url) ?: "") + sub.file_path
            PlayerSubtitle(sub.language, absolute)
        } ?: emptyList()
        onPlay(url, subs, link.url)
        onDone()
    }
}

/** Section heading for this file. */
@Composable
private fun SeriesLabel(text: String) {
    Text(
        text = text.uppercase(),
        color = Beam.colors.mutedForeground,
        fontFamily = Fraunces,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
    )
}
