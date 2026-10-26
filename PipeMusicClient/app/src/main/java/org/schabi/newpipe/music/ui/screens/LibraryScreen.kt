package org.schabi.newpipe.music.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.schabi.newpipe.R
import org.schabi.newpipe.music.data.db.DOWNLOAD_COMPLETED
import org.schabi.newpipe.music.data.db.LibraryStats
import org.schabi.newpipe.music.data.db.TRACK_TYPE_LOCAL
import org.schabi.newpipe.music.data.db.TrackEntity
import org.schabi.newpipe.music.local.ImportState
import org.schabi.newpipe.music.playback.PlayerUiState
import org.schabi.newpipe.music.ui.components.EmptyState
import org.schabi.newpipe.music.ui.components.GlassCard
import org.schabi.newpipe.music.ui.components.SectionTitle
import org.schabi.newpipe.music.ui.components.SongRow
import org.schabi.newpipe.music.ui.formatBytes
import org.schabi.newpipe.music.ui.formatTotalDuration

enum class LibraryFilter(val labelRes: Int) {
    ALL(R.string.pm_filter_all),
    LOCAL(R.string.pm_filter_local),
    DOWNLOADED(R.string.pm_filter_downloaded),
    RECENT(R.string.pm_filter_recent),
    FAVORITES(R.string.pm_filter_favorites)
}

enum class LibrarySort(val labelRes: Int) {
    TITLE(R.string.pm_sort_title),
    ARTIST(R.string.pm_sort_artist),
    DURATION(R.string.pm_sort_duration),
    ADDED(R.string.pm_sort_added)
}

@Composable
fun LibraryScreen(
    tracks: List<TrackEntity>,
    stats: LibraryStats,
    importState: ImportState,
    player: PlayerUiState,
    onImport: () -> Unit,
    onImportFile: () -> Unit = {},
    onCancelImport: () -> Unit,
    onClearImport: () -> Unit,
    onPlayList: (List<TrackEntity>, Int) -> Unit,
    onMore: (TrackEntity) -> Unit,
    onPlaySelected: (List<TrackEntity>) -> Unit = {},
    onFavoriteSelected: (List<TrackEntity>) -> Unit = {},
    onAddSelected: (List<Long>) -> Unit,
    onDeleteSelected: (List<TrackEntity>) -> Unit,
    activeDownloads: Int = 0,
    onOpenDownloads: () -> Unit = {}
) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    var filter by remember { mutableStateOf(LibraryFilter.ALL) }
    var sort by remember { mutableStateOf(LibrarySort.TITLE) }
    var query by remember { mutableStateOf("") }
    var selection by remember { mutableStateOf(setOf<Long>()) }
    var sortMenu by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    val offline = tracks.filter {
        it.type == TRACK_TYPE_LOCAL || it.downloadState == DOWNLOAD_COMPLETED
    }
    val filtered = remember(tracks, filter, sort, query) {
        val base = when (filter) {
            LibraryFilter.ALL -> offline
            LibraryFilter.LOCAL -> offline.filter { it.type == TRACK_TYPE_LOCAL }
            LibraryFilter.DOWNLOADED -> offline.filter { it.downloadState == DOWNLOAD_COMPLETED }
            LibraryFilter.RECENT -> offline
            LibraryFilter.FAVORITES -> tracks.filter { it.isFavorite }
        }.filter {
            query.isBlank() ||
                it.title.contains(query, true) ||
                it.artist.contains(query, true) ||
                it.album.contains(query, true)
        }
        when (sort) {
            LibrarySort.TITLE -> base.sortedBy { it.title.lowercase() }
            LibrarySort.ARTIST -> base.sortedBy { it.artist.lowercase() }
            LibrarySort.DURATION -> base.sortedByDescending { it.durationMs }
            LibrarySort.ADDED -> base.sortedByDescending { it.addedAt }
        }
    }

    val selectedTracks = tracks.filter { it.id in selection }
    val allFilteredSelected = filtered.isNotEmpty() && filtered.all { it.id in selection }
    val allSelectedFavorites = selectedTracks.isNotEmpty() && selectedTracks.all { it.isFavorite }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 26.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.pm_library_title),
                        style = MaterialTheme.typography.displaySmall,
                        color = colors.onSurface
                    )
                    Text(
                        text = stringResource(R.string.pm_library_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant
                    )
                }
                if (activeDownloads > 0) {
                    IconButton(onClick = onOpenDownloads) {
                        Icon(
                            imageVector = Icons.Rounded.CloudDownload,
                            contentDescription = stringResource(R.string.pm_action_view_downloads),
                            tint = colors.primary
                        )
                    }
                }
                IconButton(onClick = onImport) {
                    Icon(
                        imageVector = Icons.Rounded.FolderOpen,
                        contentDescription = stringResource(R.string.pm_action_import_folder),
                        tint = colors.primary
                    )
                }
            }
        }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row {
                    StatBlock(
                        value = "${stats.trackCount}",
                        label = stringResource(R.string.pm_library_stat_songs),
                        modifier = Modifier.weight(1f)
                    )
                    StatBlock(
                        value = formatTotalDuration(context, stats.totalDurationMs),
                        label = stringResource(R.string.pm_library_stat_duration),
                        modifier = Modifier.weight(1f)
                    )
                    StatBlock(
                        value = formatBytes(stats.totalBytes),
                        label = stringResource(R.string.pm_library_stat_size),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        item {
            if (importState.running) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.LibraryMusic,
                            contentDescription = null,
                            tint = colors.primary
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (importState.total == 0) {
                                    stringResource(R.string.pm_import_scanning)
                                } else {
                                    stringResource(
                                        R.string.pm_import_progress,
                                        importState.current,
                                        importState.total
                                    )
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.onSurface
                            )
                            Text(
                                text = importState.currentName.ifBlank {
                                    stringResource(R.string.pm_import_preparing)
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        TextButton(onClick = onCancelImport) {
                            Text(stringResource(R.string.pm_action_cancel))
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    if (importState.total == 0) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        LinearProgressIndicator(
                            progress = { importState.fraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape)
                        )
                    }
                }
            } else if (importState.finished) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = importState.error
                                    ?: stringResource(R.string.pm_import_complete),
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.onSurface
                            )
                            Text(
                                text = stringResource(
                                    R.string.pm_import_summary,
                                    importState.added,
                                    importState.skipped,
                                    importState.failed
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = onClearImport) {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = stringResource(R.string.pm_action_close)
                            )
                        }
                    }
                }
            } else {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(colors.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.FolderOpen,
                                contentDescription = null,
                                tint = colors.primary
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.pm_import_card_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.onSurface
                            )
                            Text(
                                text = stringResource(R.string.pm_import_card_desc),
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onImport, shape = CircleShape) {
                            Icon(
                                imageVector = Icons.Rounded.FolderOpen,
                                contentDescription = null,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(stringResource(R.string.pm_action_import_folder))
                        }
                        TextButton(onClick = onImportFile, shape = CircleShape) {
                            Icon(
                                imageVector = Icons.Rounded.MusicNote,
                                contentDescription = null,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(stringResource(R.string.pm_action_import_file))
                        }
                    }
                }
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(LibraryFilter.entries.toList()) { item ->
                    FilterPill(
                        label = stringResource(item.labelRes),
                        selected = filter == item,
                        onClick = { filter = item }
                    )
                }
            }
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(stringResource(R.string.pm_library_search_hint)) },
                    leadingIcon = {
                        Icon(Icons.Rounded.Search, contentDescription = null)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Box {
                    IconButton(onClick = { sortMenu = true }) {
                        Icon(
                            imageVector = Icons.Rounded.Sort,
                            contentDescription = stringResource(R.string.pm_sort_title),
                            tint = colors.onSurfaceVariant
                        )
                    }
                    DropdownMenu(
                        expanded = sortMenu,
                        onDismissRequest = { sortMenu = false }
                    ) {
                        LibrarySort.entries.toList().forEach { item ->
                            val label = stringResource(item.labelRes)
                            DropdownMenuItem(
                                text = { Text(if (sort == item) "✓ $label" else label) },
                                onClick = {
                                    sort = item
                                    sortMenu = false
                                }
                            )
                        }
                    }
                }
            }
        }

        item {
            AnimatedVisibility(visible = selection.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = colors.primary.copy(alpha = 0.14f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(
                                R.string.pm_selected_count,
                                selection.size
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.primary,
                            maxLines = 1,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(Modifier.width(4.dp))
                        IconButton(
                            onClick = {
                                selection = if (allFilteredSelected) {
                                    emptySet()
                                } else {
                                    filtered.map { it.id }.toSet()
                                }
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SelectAll,
                                contentDescription = stringResource(R.string.pm_action_select_all),
                                tint = colors.primary
                            )
                        }
                        IconButton(
                            onClick = { onPlaySelected(selectedTracks) },
                            enabled = selectedTracks.isNotEmpty(),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = stringResource(R.string.pm_action_play_selected),
                                tint = colors.primary
                            )
                        }
                        IconButton(
                            onClick = { onFavoriteSelected(selectedTracks) },
                            enabled = selectedTracks.isNotEmpty(),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = if (allSelectedFavorites) {
                                    Icons.Rounded.Favorite
                                } else {
                                    Icons.Rounded.FavoriteBorder
                                },
                                contentDescription = stringResource(
                                    if (allSelectedFavorites) {
                                        R.string.pm_action_unfavorite
                                    } else {
                                        R.string.pm_action_favorite
                                    }
                                ),
                                tint = colors.primary
                            )
                        }
                        IconButton(
                            onClick = {
                                onAddSelected(selection.toList())
                                selection = emptySet()
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlaylistAdd,
                                contentDescription = stringResource(R.string.pm_action_add_to_playlist),
                                tint = colors.primary
                            )
                        }
                        IconButton(
                            onClick = { confirmDelete = true },
                            enabled = selectedTracks.isNotEmpty(),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = stringResource(R.string.pm_action_delete),
                                tint = colors.error
                            )
                        }
                        IconButton(
                            onClick = { selection = emptySet() },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = stringResource(R.string.pm_action_cancel_selection),
                                tint = colors.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                val favorites = filter == LibraryFilter.FAVORITES
                EmptyState(
                    title = stringResource(
                        if (favorites) R.string.pm_library_empty_fav_title
                        else R.string.pm_library_empty_title
                    ),
                    description = stringResource(
                        if (favorites) R.string.pm_library_empty_fav_desc
                        else R.string.pm_library_empty_desc
                    ),
                    icon = Icons.Rounded.LibraryMusic,
                    actionLabel = if (favorites) {
                        null
                    } else {
                        stringResource(R.string.pm_action_import_folder)
                    },
                    onAction = if (favorites) null else onImport
                )
            }
        } else {
            if (filtered.isNotEmpty()) {
                item {
                    SectionTitle(
                        title = stringResource(R.string.pm_tracks_count, filtered.size)
                    )
                }
            }
            items(filtered) { track ->
                SongRow(
                    track = track,
                    isCurrent = player.current?.id == track.id,
                    isPlaying = player.isPlaying,
                    selected = track.id in selection,
                    onClick = {
                        if (selection.isNotEmpty()) {
                            selection = if (track.id in selection) {
                                selection - track.id
                            } else {
                                selection + track.id
                            }
                        } else {
                            onPlayList(filtered, filtered.indexOf(track))
                        }
                    },
                    onLongClick = {
                        selection = if (track.id in selection) {
                            selection - track.id
                        } else {
                            selection + track.id
                        }
                    },
                    onMore = { onMore(track) }
                )
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.pm_delete_selected_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.pm_delete_selected_message,
                        selectedTracks.size
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteSelected(selectedTracks)
                    selection = emptySet()
                    confirmDelete = false
                }) {
                    Text(
                        stringResource(R.string.pm_action_delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(stringResource(R.string.pm_action_cancel))
                }
            }
        )
    }
}

@Composable
private fun StatBlock(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .clip(CircleShape)
            .clickable { onClick() },
        shape = CircleShape,
        color = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        }
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}
