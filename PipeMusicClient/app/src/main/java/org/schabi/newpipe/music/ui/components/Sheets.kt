package org.schabi.newpipe.music.ui.components

import android.text.format.DateFormat
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.automirrored.rounded.CallMerge
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.QueuePlayNext
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.schabi.newpipe.R
import org.schabi.newpipe.music.data.db.DOWNLOADING
import org.schabi.newpipe.music.data.db.DOWNLOAD_COMPLETED
import org.schabi.newpipe.music.data.db.DOWNLOAD_FAILED
import org.schabi.newpipe.music.data.db.DOWNLOAD_QUEUED
import org.schabi.newpipe.music.data.db.PlaylistWithCount
import org.schabi.newpipe.music.data.db.TrackEntity
import org.schabi.newpipe.music.playback.PlayerUiState
import org.schabi.newpipe.music.playback.SleepTimer
import org.schabi.newpipe.music.ui.formatDuration
import org.schabi.newpipe.music.ui.viewmodel.SongListKind
import org.schabi.newpipe.music.ui.viewmodel.SongListUiState
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToPlaylistSheet(
    playlists: List<PlaylistWithCount>,
    onDismiss: () -> Unit,
    onSelect: (Long) -> Unit,
    onCreate: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf("") }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            Text(
                text = stringResource(R.string.pm_action_add_to_playlist),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(stringResource(R.string.pm_new_playlist_title)) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )
                Spacer(Modifier.width(10.dp))
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            onCreate(name.trim())
                            name = ""
                        }
                    },
                    shape = CircleShape
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                }
            }
            Spacer(Modifier.height(10.dp))
            if (playlists.isEmpty()) {
                Text(
                    text = stringResource(R.string.pm_playlists_empty_title),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 18.dp)
                )
            } else {
                LazyColumn(modifier = Modifier.height(320.dp)) {
                    items(playlists.size) { index ->
                        val playlist = playlists[index]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(playlist.id) }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Artwork(
                                source = playlist.coverUrl,
                                size = 42.dp,
                                modifier = Modifier.size(42.dp),
                                shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = playlist.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = stringResource(
                                        R.string.pm_tracks_short,
                                        playlist.trackCount
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = Icons.Rounded.PlaylistAdd,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackActionsSheet(
    track: TrackEntity,
    playlistId: Long?,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDownload: () -> Unit,
    onCancelDownload: () -> Unit = {},
    onRemoveFromPlaylist: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
    onShowParts: (() -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Artwork(
                    source = track.artworkUri ?: track.thumbnailUrl,
                    size = 52.dp,
                    modifier = Modifier.size(52.dp),
                    shape = RoundedCornerShape(14.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = track.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            ActionRow(
                Icons.Rounded.PlayArrow,
                stringResource(R.string.pm_action_play),
                onPlay,
                onDismiss
            )
            ActionRow(
                Icons.Rounded.PlaylistAdd,
                stringResource(R.string.pm_action_add_to_playlist),
                onAddToPlaylist,
                onDismiss
            )
            ActionRow(
                if (track.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                stringResource(
                    if (track.isFavorite) R.string.pm_action_unfavorite
                    else R.string.pm_action_favorite
                ),
                onToggleFavorite,
                onDismiss
            )
            if (onShowParts != null) {
                ActionRow(
                    Icons.Rounded.ViewList,
                    stringResource(R.string.pm_action_all_parts),
                    onShowParts,
                    onDismiss
                )
            }
            if (track.remoteUrl != null &&
                track.downloadState != org.schabi.newpipe.music.data.db.DOWNLOAD_COMPLETED
            ) {
                val downloading = track.downloadState ==
                    org.schabi.newpipe.music.data.db.DOWNLOADING ||
                    track.downloadState == org.schabi.newpipe.music.data.db.DOWNLOAD_QUEUED
                ActionRow(
                    Icons.Rounded.Download,
                    stringResource(
                        if (downloading) R.string.pm_action_cancel_download
                        else R.string.pm_action_download_audio
                    ),
                    if (downloading) onCancelDownload else onDownload,
                    onDismiss
                )
            }
            if (playlistId != null) {
                ActionRow(
                    Icons.Rounded.ArrowUpward,
                    stringResource(R.string.pm_action_move_up),
                    onMoveUp,
                    onDismiss
                )
                ActionRow(
                    Icons.Rounded.ArrowDownward,
                    stringResource(R.string.pm_action_move_down),
                    onMoveDown,
                    onDismiss
                )
                ActionRow(
                    Icons.Rounded.RemoveCircleOutline,
                    stringResource(R.string.pm_action_remove_from_playlist),
                    onRemoveFromPlaylist,
                    onDismiss
                )
            }
            if (track.type == org.schabi.newpipe.music.data.db.TRACK_TYPE_LOCAL ||
                track.downloadState == org.schabi.newpipe.music.data.db.DOWNLOAD_COMPLETED
            ) {
                ActionRow(
                    Icons.Rounded.Delete,
                    stringResource(R.string.pm_action_delete_local),
                    onDelete,
                    onDismiss,
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/**
 * The parts of a multi-part video, or a playlist / album opened from a link. Songs can be picked
 * with their checkboxes (or a long press); every action works on the picked songs, or on the
 * whole list while none are picked.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SongListSheet(
    state: SongListUiState,
    /** Library rows of the listed songs, by song URL, for their download state. */
    tracks: Map<String, TrackEntity>,
    onDismiss: () -> Unit,
    onPlay: (indices: List<Int>, startIndex: Int) -> Unit,
    onDownload: (List<Int>) -> Unit,
    onDeleteDownloads: (List<Int>) -> Unit,
    onMerge: (indices: List<Int>, title: String, deleteParts: Boolean) -> Unit,
    onAddToPlaylist: (List<Int>) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colors = MaterialTheme.colorScheme
    val isParts = state.kind == SongListKind.PARTS
    var selection by remember(state.url) { mutableStateOf(setOf<Int>()) }
    var confirmDelete by remember { mutableStateOf(false) }
    var mergeDialog by remember { mutableStateOf(false) }
    val targets = if (selection.isEmpty()) state.songs.indices.toList() else selection.sorted()
    val downloadedTargets = targets.count { index ->
        state.songs.getOrNull(index)?.let { tracks[it.url] }?.let {
            it.downloadState == DOWNLOAD_COMPLETED ||
                it.downloadState == DOWNLOADING ||
                it.downloadState == DOWNLOAD_QUEUED
        } == true
    }
    val toggle = { index: Int ->
        selection = if (index in selection) selection - index else selection + index
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surfaceContainerLow
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = state.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (state.loadingMore) {
                    Spacer(Modifier.width(12.dp))
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                }
                if (state.songs.isNotEmpty()) {
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = stringResource(
                            if (isParts) R.string.pm_parts_count else R.string.pm_tracks_short,
                            state.songs.size
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.onSurfaceVariant
                    )
                }
            }
            if (state.truncated) {
                Text(
                    text = stringResource(R.string.pm_link_playlist_truncated, state.songs.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            when {
                state.loading -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                }

                state.error != null || state.songs.isEmpty() -> Text(
                    text = state.error ?: stringResource(
                        if (isParts) R.string.pm_parts_single else R.string.pm_link_playlist_empty
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (state.error != null) colors.error else colors.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 16.dp)
                )

                else -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TriStateCheckbox(
                            state = when {
                                selection.isEmpty() -> ToggleableState.Off
                                selection.size == state.songs.size -> ToggleableState.On
                                else -> ToggleableState.Indeterminate
                            },
                            onClick = {
                                selection = if (selection.size == state.songs.size) {
                                    emptySet()
                                } else {
                                    state.songs.indices.toSet()
                                }
                            }
                        )
                        Text(
                            text = if (selection.isEmpty()) {
                                stringResource(R.string.pm_action_select_all)
                            } else {
                                stringResource(R.string.pm_selected_count, selection.size)
                            },
                            style = MaterialTheme.typography.titleSmall,
                            color = if (selection.isEmpty()) colors.onSurfaceVariant else colors.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (isParts) {
                            IconButton(onClick = { mergeDialog = true }, enabled = targets.size >= 2) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.CallMerge,
                                    contentDescription = stringResource(R.string.pm_action_merge),
                                    tint = if (targets.size >= 2) colors.primary else colors.outline
                                )
                            }
                        }
                        IconButton(
                            onClick = { confirmDelete = true },
                            enabled = downloadedTargets > 0
                        ) {
                            Icon(
                                Icons.Rounded.Delete,
                                contentDescription = stringResource(R.string.pm_action_delete_downloads),
                                tint = if (downloadedTargets > 0) colors.error else colors.outline
                            )
                        }
                        IconButton(onClick = { onAddToPlaylist(targets) }) {
                            Icon(
                                Icons.Rounded.PlaylistAdd,
                                contentDescription = stringResource(
                                    R.string.pm_action_add_to_playlist
                                ),
                                tint = colors.primary
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(onClick = { onPlay(targets, 0) }, modifier = Modifier.weight(1f)) {
                            Icon(
                                Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = stringResource(
                                    if (selection.isEmpty()) {
                                        R.string.pm_action_play_all
                                    } else {
                                        R.string.pm_action_play_selected
                                    }
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                onDownload(targets)
                                selection = emptySet()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Rounded.Download,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = stringResource(
                                    if (selection.isEmpty()) {
                                        R.string.pm_action_download_all
                                    } else {
                                        R.string.pm_action_download_selected
                                    }
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                        itemsIndexed(state.songs, key = { _, song -> song.url }) { index, song ->
                            val selected = index in selection
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        if (selected) {
                                            colors.primary.copy(alpha = 0.12f)
                                        } else {
                                            Color.Transparent
                                        }
                                    )
                                    .combinedClickable(
                                        onClick = {
                                            if (selection.isEmpty()) {
                                                onPlay(state.songs.indices.toList(), index)
                                            } else {
                                                toggle(index)
                                            }
                                        },
                                        onLongClick = { toggle(index) }
                                    )
                                    .padding(end = 8.dp, top = 4.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(checked = selected, onCheckedChange = { toggle(index) })
                                Text(
                                    text = if (isParts) "P${index + 1}" else "${index + 1}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = colors.primary,
                                    modifier = Modifier.width(44.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = song.title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = colors.onSurface,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    // Parts share one uploader; playlist songs may not.
                                    if (!isParts && song.artist.isNotBlank()) {
                                        Text(
                                            text = song.artist,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    tracks[song.url]?.let { track -> DownloadStatus(track) }
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = formatDuration(song.durationMs),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colors.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.pm_delete_downloads_title)) },
            text = {
                Text(stringResource(R.string.pm_delete_downloads_message, downloadedTargets))
            },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteDownloads(targets)
                    selection = emptySet()
                    confirmDelete = false
                }) {
                    Text(stringResource(R.string.pm_action_delete), color = colors.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(stringResource(R.string.pm_action_cancel))
                }
            }
        )
    }

    if (mergeDialog) {
        var name by remember { mutableStateOf(state.title) }
        var deleteParts by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { mergeDialog = false },
            title = { Text(stringResource(R.string.pm_merge_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.pm_merge_message, targets.size))
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.pm_merge_name)) },
                        singleLine = true
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { deleteParts = !deleteParts }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = deleteParts, onCheckedChange = { deleteParts = it })
                        Text(
                            text = stringResource(R.string.pm_merge_delete_parts),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onMerge(targets, name.trim(), deleteParts)
                        selection = emptySet()
                        mergeDialog = false
                    },
                    enabled = name.isNotBlank()
                ) {
                    Text(stringResource(R.string.pm_action_merge))
                }
            },
            dismissButton = {
                TextButton(onClick = { mergeDialog = false }) {
                    Text(stringResource(R.string.pm_action_cancel))
                }
            }
        )
    }
}

/** One line under a listed song telling whether it is downloaded or downloading. */
@Composable
private fun DownloadStatus(track: TrackEntity) {
    val colors = MaterialTheme.colorScheme
    val (text, color) = when (track.downloadState) {
        DOWNLOAD_COMPLETED -> stringResource(R.string.pm_action_downloaded) to colors.primary
        DOWNLOADING -> stringResource(
            R.string.pm_download_status_downloading,
            track.downloadProgress
        ) to colors.tertiary
        DOWNLOAD_QUEUED -> stringResource(R.string.pm_download_status_queued) to colors.tertiary
        DOWNLOAD_FAILED -> stringResource(R.string.pm_download_status_failed) to colors.error
        else -> return
    }
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        maxLines = 1
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueSheet(
    state: PlayerUiState,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit,
    onPlayNext: (Int) -> Unit,
    onMove: (from: Int, to: Int) -> Unit,
    onRemove: (Int) -> Unit,
    onTrackActions: (TrackEntity) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.pm_queue_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = stringResource(R.string.pm_tracks_short, state.queue.size),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(10.dp))
            if (state.queue.isEmpty()) {
                EmptyState(
                    title = stringResource(R.string.pm_queue_empty_title),
                    description = stringResource(R.string.pm_queue_empty_desc),
                    icon = Icons.Rounded.PlaylistAdd
                )
            } else {
                LazyColumn(modifier = Modifier.height(420.dp)) {
                    itemsIndexed(state.queue) { index, track ->
                        QueueRow(
                            track = track,
                            index = index,
                            state = state,
                            onSelect = onSelect,
                            onPlayNext = onPlayNext,
                            onMove = onMove,
                            onRemove = onRemove,
                            onTrackActions = onTrackActions
                        )
                    }
                }
            }
        }
    }
}

/** A queued song whose overflow menu offers queue actions instead of removing it at once. */
@Composable
private fun QueueRow(
    track: TrackEntity,
    index: Int,
    state: PlayerUiState,
    onSelect: (Int) -> Unit,
    onPlayNext: (Int) -> Unit,
    onMove: (from: Int, to: Int) -> Unit,
    onRemove: (Int) -> Unit,
    onTrackActions: (TrackEntity) -> Unit
) {
    var menu by remember { mutableStateOf(false) }
    val isCurrent = index == state.index
    Box {
        SongRow(
            track = track,
            isCurrent = isCurrent,
            isPlaying = state.isPlaying,
            onClick = { onSelect(index) },
            onMore = { menu = true },
            showMore = true
        )
        // Anchors the menu under the overflow button at the end of the row.
        Box(modifier = Modifier.align(Alignment.BottomEnd)) {
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                if (!isCurrent) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.pm_action_play)) },
                        leadingIcon = { Icon(Icons.Rounded.PlayArrow, contentDescription = null) },
                        onClick = {
                            menu = false
                            onSelect(index)
                        }
                    )
                    if (index != state.index + 1) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.pm_action_play_next)) },
                            leadingIcon = {
                                Icon(Icons.Rounded.QueuePlayNext, contentDescription = null)
                            },
                            onClick = {
                                menu = false
                                onPlayNext(index)
                            }
                        )
                    }
                }
                if (index > 0) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.pm_action_move_up)) },
                        leadingIcon = { Icon(Icons.Rounded.ArrowUpward, contentDescription = null) },
                        onClick = {
                            menu = false
                            onMove(index, index - 1)
                        }
                    )
                }
                if (index < state.queue.lastIndex) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.pm_action_move_down)) },
                        leadingIcon = {
                            Icon(Icons.Rounded.ArrowDownward, contentDescription = null)
                        },
                        onClick = {
                            menu = false
                            onMove(index, index + 1)
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.pm_action_remove_from_queue)) },
                    leadingIcon = {
                        Icon(Icons.Rounded.RemoveCircleOutline, contentDescription = null)
                    },
                    onClick = {
                        menu = false
                        onRemove(index)
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.pm_action_more_options)) },
                    leadingIcon = { Icon(Icons.Rounded.MoreHoriz, contentDescription = null) },
                    onClick = {
                        menu = false
                        onTrackActions(track)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepTimerSheet(
    endsAt: Long,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var customHours by remember { mutableStateOf("") }
    var customMinutes by remember { mutableStateOf("") }
    var customInvalid by remember { mutableStateOf(false) }
    val context = LocalContext.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            Text(
                text = stringResource(R.string.pm_sleep_sheet_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (endsAt > 0) {
                    stringResource(
                        R.string.pm_sleep_ends_at,
                        DateFormat.getTimeFormat(context).format(Date(endsAt))
                    )
                } else {
                    stringResource(R.string.pm_sleep_sheet_desc)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (endsAt > 0) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Spacer(Modifier.height(12.dp))
            SleepTimer.PRESETS.forEach { minutes ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onSelect(minutes)
                            onDismiss()
                        }
                        .padding(vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Timer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.pm_minutes_later, minutes),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.pm_sleep_custom),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(8.dp))
            val startCustom = {
                val total = SleepTimer.customMinutes(customHours, customMinutes)
                if (total == null) {
                    customInvalid = true
                } else {
                    onSelect(total)
                    onDismiss()
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = customHours,
                    onValueChange = { value ->
                        customHours = value.filter { it.isDigit() }.take(2)
                        customInvalid = false
                    },
                    modifier = Modifier.weight(1f),
                    label = { Text(stringResource(R.string.pm_sleep_hours)) },
                    singleLine = true,
                    isError = customInvalid,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                Spacer(Modifier.width(10.dp))
                OutlinedTextField(
                    value = customMinutes,
                    onValueChange = { value ->
                        customMinutes = value.filter { it.isDigit() }.take(4)
                        customInvalid = false
                    },
                    modifier = Modifier.weight(1f),
                    label = { Text(stringResource(R.string.pm_sleep_minutes)) },
                    singleLine = true,
                    isError = customInvalid,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { startCustom() }),
                    shape = RoundedCornerShape(16.dp)
                )
                Spacer(Modifier.width(10.dp))
                Button(onClick = startCustom) {
                    Text(stringResource(R.string.pm_sleep_start))
                }
            }
            if (customInvalid) {
                Text(
                    text = stringResource(R.string.pm_sleep_custom_invalid),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = {
                onSelect(0)
                onDismiss()
            }) {
                Text(stringResource(R.string.pm_sleep_off))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSongsSheet(
    tracks: List<TrackEntity>,
    existingIds: Set<Long>,
    onDismiss: () -> Unit,
    onAdd: (List<TrackEntity>) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(setOf<Long>()) }
    val filtered = tracks.filter {
        query.isBlank() ||
            it.title.contains(query, true) ||
            it.artist.contains(query, true)
    }
    val addable = filtered.filter { it.id !in existingIds }
    val allAddableSelected = addable.isNotEmpty() && addable.all { it.id in selected }
    val selectedTracks = tracks.filter { it.id in selected }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(modifier = Modifier.padding(horizontal = 18.dp).padding(bottom = 24.dp)) {
            Text(
                text = stringResource(R.string.pm_add_songs_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.pm_add_songs_batch_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.pm_search_library_hint)) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.pm_selected_count, selectedTracks.size),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(Modifier.width(8.dp))
                TextButton(
                    onClick = {
                        selected = if (allAddableSelected) {
                            emptySet()
                        } else {
                            addable.map { it.id }.toSet()
                        }
                    },
                    enabled = addable.isNotEmpty()
                ) {
                    Text(stringResource(R.string.pm_action_select_all))
                }
                Spacer(Modifier.width(4.dp))
                Button(
                    onClick = {
                        onAdd(selectedTracks)
                        onDismiss()
                    },
                    enabled = selectedTracks.isNotEmpty(),
                    shape = CircleShape
                ) {
                    Text(
                        stringResource(
                            R.string.pm_add_selected_count,
                            selectedTracks.size
                        )
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            if (filtered.isEmpty()) {
                Text(
                    text = stringResource(R.string.pm_no_songs_to_add),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            } else {
                LazyColumn(modifier = Modifier.height(380.dp)) {
                    items(filtered) { track ->
                        val added = track.id in existingIds
                        val isSelected = track.id in selected
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (isSelected) {
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    } else {
                                        Color.Transparent
                                    }
                                )
                                .clickable(enabled = !added) {
                                    selected = if (isSelected) {
                                        selected - track.id
                                    } else {
                                        selected + track.id
                                    }
                                }
                                .padding(vertical = 9.dp, horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Artwork(
                                source = track.artworkUri ?: track.thumbnailUrl,
                                size = 46.dp,
                                modifier = Modifier.size(46.dp),
                                shape = RoundedCornerShape(13.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = track.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = track.artist,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (added) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = stringResource(R.string.pm_added),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(R.string.pm_added),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = null
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    onDismiss: () -> Unit,
    tint: Color? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onClick()
                onDismiss()
            }
            .padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint ?: MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(14.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = tint ?: MaterialTheme.colorScheme.onSurface
        )
    }
}
