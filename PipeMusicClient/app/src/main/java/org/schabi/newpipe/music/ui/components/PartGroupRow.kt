package org.schabi.newpipe.music.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.ViewList
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.schabi.newpipe.R
import org.schabi.newpipe.music.data.db.TrackEntity
import org.schabi.newpipe.music.stream.TrackEntry

/**
 * Lists [entries]: plain songs through [song], each multi-part video as a [PartGroupRow] whose
 * parts follow it through [part] while its key is in [expanded].
 */
fun LazyListScope.trackEntries(
    entries: List<TrackEntry>,
    expanded: Set<String>,
    song: @Composable (TrackEntity) -> Unit,
    group: @Composable (TrackEntry.Parts) -> Unit,
    part: @Composable (TrackEntity) -> Unit
) {
    entries.forEach { entry ->
        when (entry) {
            is TrackEntry.Single -> item(key = "t${entry.track.id}") { song(entry.track) }
            is TrackEntry.Parts -> {
                item(key = "g${entry.key}") { group(entry) }
                if (entry.key in expanded) {
                    items(entry.tracks, key = { "p${it.id}" }) { track ->
                        Box(Modifier.padding(start = 22.dp)) { part(track) }
                    }
                }
            }
        }
    }
}

/**
 * Song artwork; a multi-part video ([partCount] of at least 2) gets a stack behind it and its
 * part count on it, the same in search results, the library and playlists.
 */
@Composable
fun PartsArtwork(source: String?, partCount: Int?, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val parts = partCount != null && partCount >= 2
    Box(modifier = modifier.size(58.dp), contentAlignment = Alignment.BottomStart) {
        if (parts) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.primary.copy(alpha = 0.35f))
            )
        }
        Box(contentAlignment = Alignment.BottomEnd) {
            Artwork(
                source = source,
                size = 54.dp,
                modifier = Modifier.size(54.dp),
                shape = RoundedCornerShape(15.dp)
            )
            if (parts) {
                Text(
                    text = stringResource(R.string.pm_parts_badge, partCount ?: 0),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onPrimary,
                    modifier = Modifier
                        .padding(3.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(colors.primary)
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
        }
    }
}

/** "Uploader · 12 parts". */
@Composable
fun partsSubtitle(artist: String, partCount: Int): String =
    stringResource(
        R.string.pm_parts_subtitle,
        artist.ifBlank { stringResource(R.string.pm_unknown_artist) },
        partCount
    )

/** One multi-part video in a song list. Tapping it shows or hides its parts. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PartGroupRow(
    entry: TrackEntry.Parts,
    expanded: Boolean,
    isCurrent: Boolean,
    onClick: () -> Unit,
    onPlay: () -> Unit,
    onAddToPlaylist: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    onShowParts: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null,
    removeLabel: String? = null,
    removeIcon: ImageVector = Icons.Rounded.Delete
) {
    val colors = MaterialTheme.colorScheme
    var menu by remember { mutableStateOf(false) }
    val first = entry.tracks.first()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (selected) colors.primary.copy(alpha = 0.12f) else Color.Transparent
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PartsArtwork(
            source = first.artworkUri ?: first.thumbnailUrl,
            partCount = entry.tracks.size
        )
        Spacer(Modifier.width(13.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.title,
                style = MaterialTheme.typography.titleMedium,
                color = if (isCurrent) colors.primary else colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = partsSubtitle(first.artist, entry.tracks.size),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onPlay) {
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = stringResource(R.string.pm_action_play_all),
                tint = colors.primary
            )
        }
        Icon(
            imageVector = if (expanded) {
                Icons.Rounded.KeyboardArrowUp
            } else {
                Icons.Rounded.KeyboardArrowDown
            },
            contentDescription = stringResource(
                if (expanded) R.string.pm_action_hide_parts else R.string.pm_action_show_parts
            ),
            tint = colors.onSurfaceVariant
        )
        Box {
            IconButton(onClick = { menu = true }) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = stringResource(R.string.pm_action_more_options),
                    tint = colors.onSurfaceVariant
                )
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.pm_action_add_to_playlist)) },
                    leadingIcon = { Icon(Icons.Rounded.PlaylistAdd, contentDescription = null) },
                    onClick = {
                        menu = false
                        onAddToPlaylist()
                    }
                )
                if (onShowParts != null) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.pm_action_all_parts)) },
                        leadingIcon = { Icon(Icons.Rounded.ViewList, contentDescription = null) },
                        onClick = {
                            menu = false
                            onShowParts()
                        }
                    )
                }
                if (onRemove != null && removeLabel != null) {
                    DropdownMenuItem(
                        text = { Text(removeLabel, color = colors.error) },
                        leadingIcon = {
                            Icon(
                                imageVector = removeIcon,
                                contentDescription = null,
                                tint = colors.error
                            )
                        },
                        onClick = {
                            menu = false
                            onRemove()
                        }
                    )
                }
            }
        }
    }
}
