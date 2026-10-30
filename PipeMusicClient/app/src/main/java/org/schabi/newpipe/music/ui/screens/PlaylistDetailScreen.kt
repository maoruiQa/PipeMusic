package org.schabi.newpipe.music.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.schabi.newpipe.R
import org.schabi.newpipe.music.data.PlaylistOrder
import org.schabi.newpipe.music.data.PlaylistSort
import org.schabi.newpipe.music.data.db.PlaylistEntity
import org.schabi.newpipe.music.data.db.TrackEntity
import org.schabi.newpipe.music.playback.PlayerUiState
import org.schabi.newpipe.music.ui.components.Artwork
import org.schabi.newpipe.music.ui.components.EmptyState
import org.schabi.newpipe.music.ui.components.SongRow
import org.schabi.newpipe.music.ui.formatTotalDuration

@Composable
fun PlaylistDetailScreen(
    playlist: PlaylistEntity?,
    tracks: List<TrackEntity>,
    player: PlayerUiState,
    onBack: () -> Unit,
    onPlayAll: (List<TrackEntity>) -> Unit,
    onShuffle: (List<TrackEntity>) -> Unit,
    onPlayTrack: (List<TrackEntity>, Int) -> Unit,
    onTrackMore: (TrackEntity) -> Unit,
    onAddSongs: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    var renameDialog by remember { mutableStateOf(false) }
    var deleteDialog by remember { mutableStateOf(false) }
    var nameInput by remember { mutableStateOf(playlist?.name.orEmpty()) }
    var sortMenu by remember { mutableStateOf(false) }
    // Keyed by playlist so that opening another playlist starts unfiltered.
    var query by rememberSaveable(playlist?.id) { mutableStateOf("") }
    var sort by rememberSaveable(playlist?.id) { mutableStateOf(PlaylistSort.CUSTOM) }
    var reversed by rememberSaveable(playlist?.id) { mutableStateOf(false) }
    val cover = tracks.firstOrNull()?.let { it.artworkUri ?: it.thumbnailUrl }
    // Play all, shuffle and tapping a song follow what is on screen.
    val shown = remember(tracks, query, sort, reversed) {
        PlaylistOrder.apply(tracks, query, sort, reversed)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.pm_action_back),
                        tint = colors.onSurface
                    )
                }
                Text(
                    text = stringResource(R.string.pm_playlist_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Box {
                    IconButton(onClick = { menu = true }) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = stringResource(R.string.pm_action_more),
                            tint = colors.onSurface
                        )
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.pm_action_rename)) },
                            leadingIcon = {
                                Icon(Icons.Rounded.Edit, contentDescription = null)
                            },
                            onClick = {
                                menu = false
                                nameInput = playlist?.name.orEmpty()
                                renameDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.pm_delete_playlist_title)) },
                            leadingIcon = {
                                Icon(Icons.Rounded.Delete, contentDescription = null)
                            },
                            onClick = {
                                menu = false
                                deleteDialog = true
                            }
                        )
                    }
                }
            }
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(206.dp)
                    .clip(RoundedCornerShape(30.dp))
            ) {
                Artwork(
                    source = cover,
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(30.dp),
                    size = 420.dp
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Black.copy(alpha = 0.18f),
                                    Color.Black.copy(alpha = 0.55f),
                                    Color.Black.copy(alpha = 0.86f)
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(20.dp)
                ) {
                    Text(
                        text = playlist?.name ?: stringResource(R.string.pm_playlist_title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(
                            R.string.pm_playlist_summary,
                            tracks.size,
                            formatTotalDuration(context, tracks.sumOf { it.durationMs })
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                    Spacer(Modifier.height(12.dp))
                    Row {
                        Button(
                            onClick = { onPlayAll(shown) },
                            enabled = shown.isNotEmpty(),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color.Black
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.pm_action_play_all))
                        }
                        Spacer(Modifier.width(8.dp))
                        IconButton(
                            onClick = { onShuffle(shown) },
                            enabled = shown.isNotEmpty(),
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.18f))
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Shuffle,
                                contentDescription = stringResource(R.string.pm_action_shuffle),
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { onAddSongs() }
                    .padding(horizontal = 10.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                    tint = colors.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.pm_add_songs_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.primary
                )
            }
        }

        if (tracks.isEmpty()) {
            item {
                EmptyState(
                    title = stringResource(R.string.pm_playlist_empty_title),
                    description = stringResource(R.string.pm_playlist_empty_desc),
                    icon = Icons.Rounded.QueueMusic,
                    actionLabel = stringResource(R.string.pm_add_songs_title),
                    onAction = onAddSongs
                )
            }
        } else {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text(stringResource(R.string.pm_playlist_search_hint)) },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        trailingIcon = if (query.isNotEmpty()) {
                            {
                                IconButton(onClick = { query = "" }) {
                                    Icon(
                                        Icons.Rounded.Close,
                                        contentDescription = stringResource(R.string.pm_action_clear)
                                    )
                                }
                            }
                        } else {
                            null
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Box {
                        IconButton(onClick = { sortMenu = true }) {
                            Icon(
                                imageVector = Icons.Rounded.Sort,
                                contentDescription = stringResource(R.string.pm_action_sort),
                                tint = if (sort != PlaylistSort.CUSTOM || reversed) {
                                    colors.primary
                                } else {
                                    colors.onSurfaceVariant
                                }
                            )
                        }
                        DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                            PlaylistSort.entries.forEach { item ->
                                DropdownMenuItem(
                                    text = { Text(stringResource(item.labelRes())) },
                                    trailingIcon = if (sort == item) {
                                        { Icon(Icons.Rounded.Check, contentDescription = null) }
                                    } else {
                                        null
                                    },
                                    onClick = {
                                        sort = item
                                        sortMenu = false
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.pm_sort_reverse)) },
                                leadingIcon = {
                                    Icon(Icons.Rounded.SwapVert, contentDescription = null)
                                },
                                trailingIcon = if (reversed) {
                                    { Icon(Icons.Rounded.Check, contentDescription = null) }
                                } else {
                                    null
                                },
                                onClick = {
                                    reversed = !reversed
                                    sortMenu = false
                                }
                            )
                        }
                    }
                }
            }
            if (shown.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.pm_playlist_search_empty, query.trim()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 16.dp)
                    )
                }
            }
            items(shown, key = { it.id }) { track ->
                SongRow(
                    track = track,
                    isCurrent = player.current?.id == track.id,
                    isPlaying = player.isPlaying,
                    onClick = { onPlayTrack(shown, shown.indexOf(track)) },
                    onMore = { onTrackMore(track) }
                )
            }
        }
    }

    if (renameDialog) {
        AlertDialog(
            onDismissRequest = { renameDialog = false },
            title = { Text(stringResource(R.string.pm_rename_playlist_title)) },
            text = {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (nameInput.isNotBlank()) {
                        onRename(nameInput.trim())
                    }
                    renameDialog = false
                }) {
                    Text(stringResource(R.string.pm_action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { renameDialog = false }) {
                    Text(stringResource(R.string.pm_action_cancel))
                }
            }
        )
    }

    if (deleteDialog) {
        AlertDialog(
            onDismissRequest = { deleteDialog = false },
            title = { Text(stringResource(R.string.pm_delete_playlist_title)) },
            text = { Text(stringResource(R.string.pm_delete_playlist_message_generic)) },
            confirmButton = {
                TextButton(onClick = {
                    deleteDialog = false
                    onDelete()
                }) {
                    Text(
                        stringResource(R.string.pm_action_delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteDialog = false }) {
                    Text(stringResource(R.string.pm_action_cancel))
                }
            }
        )
    }
}

private fun PlaylistSort.labelRes(): Int = when (this) {
    PlaylistSort.CUSTOM -> R.string.pm_sort_custom
    PlaylistSort.TITLE -> R.string.pm_sort_title
    PlaylistSort.ARTIST -> R.string.pm_sort_artist
    PlaylistSort.ALBUM -> R.string.pm_sort_album
    PlaylistSort.DURATION -> R.string.pm_sort_duration
    PlaylistSort.RECENTLY_PLAYED -> R.string.pm_sort_recently_played
    PlaylistSort.MOST_PLAYED -> R.string.pm_sort_most_played
}
