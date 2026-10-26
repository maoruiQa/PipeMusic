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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Shuffle
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.schabi.newpipe.R
import org.schabi.newpipe.music.data.db.PlaylistWithCount
import org.schabi.newpipe.music.ui.components.Artwork
import org.schabi.newpipe.music.ui.components.EmptyState
import org.schabi.newpipe.music.ui.components.SectionTitle

@Composable
fun PlaylistsScreen(
    playlists: List<PlaylistWithCount>,
    favoritesCount: Int,
    onOpen: (Long) -> Unit,
    onCreate: (String) -> Unit,
    onRename: (Long, String) -> Unit,
    onDelete: (Long) -> Unit,
    onPlayFavorites: () -> Unit,
    onShuffleFavorites: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    var createDialog by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<PlaylistWithCount?>(null) }
    var deleteTarget by remember { mutableStateOf<PlaylistWithCount?>(null) }
    var nameInput by remember { mutableStateOf("") }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 26.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(span = { GridItemSpan(2) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.pm_playlists_title),
                        style = MaterialTheme.typography.displaySmall,
                        color = colors.onSurface
                    )
                    Text(
                        text = stringResource(R.string.pm_playlists_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant
                    )
                }
                IconButton(onClick = {
                    nameInput = ""
                    createDialog = true
                }) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = stringResource(R.string.pm_action_new_playlist),
                        tint = colors.primary
                    )
                }
            }
        }

        item(span = { GridItemSpan(2) }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(166.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                colors.primary.copy(alpha = 0.95f),
                                colors.tertiary.copy(alpha = 0.85f)
                            )
                        )
                    )
                    .clickable { onPlayFavorites() }
            ) {
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(22.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Favorite,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.pm_favorites_title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White
                    )
                    Text(
                        text = stringResource(R.string.pm_tracks_count, favoritesCount),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Spacer(Modifier.height(12.dp))
                    Row {
                        Button(
                            onClick = onPlayFavorites,
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color.Black
                            )
                        ) {
                            Icon(
                                Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.pm_action_play))
                        }
                        Spacer(Modifier.width(8.dp))
                        IconButton(
                            onClick = onShuffleFavorites,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.18f))
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Shuffle,
                                contentDescription = stringResource(R.string.pm_action_shuffle_favorites),
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }

        item(span = { GridItemSpan(2) }) {
            SectionTitle(
                title = stringResource(R.string.pm_my_playlists),
                actionLabel = stringResource(R.string.pm_action_new_playlist),
                onAction = {
                nameInput = ""
                createDialog = true
            })
        }

        if (playlists.isEmpty()) {
            item(span = { GridItemSpan(2) }) {
                EmptyState(
                    title = stringResource(R.string.pm_playlists_empty_title),
                    description = stringResource(R.string.pm_playlists_empty_desc),
                    icon = Icons.Rounded.PlaylistPlay,
                    actionLabel = stringResource(R.string.pm_action_new_playlist),
                    onAction = {
                        nameInput = ""
                        createDialog = true
                    }
                )
            }
        } else {
            items(playlists) { playlist ->
                PlaylistCard(
                    playlist = playlist,
                    onClick = { onOpen(playlist.id) },
                    onRename = {
                        renameTarget = playlist
                        nameInput = playlist.name
                    },
                    onDelete = { deleteTarget = playlist }
                )
            }
        }
    }

    if (createDialog) {
        AlertDialog(
            onDismissRequest = { createDialog = false },
            title = { Text(stringResource(R.string.pm_new_playlist_title)) },
            text = {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    placeholder = { Text(stringResource(R.string.pm_playlist_name_hint)) },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (nameInput.isNotBlank()) {
                        onCreate(nameInput.trim())
                    }
                    createDialog = false
                }) {
                    Text(stringResource(R.string.pm_action_create))
                }
            },
            dismissButton = {
                TextButton(onClick = { createDialog = false }) {
                    Text(stringResource(R.string.pm_action_cancel))
                }
            }
        )
    }

    val rename = renameTarget
    if (rename != null) {
        AlertDialog(
            onDismissRequest = { renameTarget = null },
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
                        onRename(rename.id, nameInput.trim())
                    }
                    renameTarget = null
                }) {
                    Text(stringResource(R.string.pm_action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) {
                    Text(stringResource(R.string.pm_action_cancel))
                }
            }
        )
    }

    val pendingDelete = deleteTarget
    if (pendingDelete != null) {
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(stringResource(R.string.pm_delete_playlist_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.pm_delete_playlist_message,
                        pendingDelete.name
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(pendingDelete.id)
                    deleteTarget = null
                }) {
                    Text(
                        stringResource(R.string.pm_action_delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text(stringResource(R.string.pm_action_cancel))
                }
            }
        )
    }
}

@Composable
private fun PlaylistCard(
    playlist: PlaylistWithCount,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    var menu by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            // Only the top corners are rounded: the artwork defines the card shape, while the
            // playlist name below must not be clipped by the card's bottom-left corner.
            .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(148.dp)
                .clip(RoundedCornerShape(22.dp))
        ) {
            Artwork(
                source = playlist.coverUrl,
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(22.dp),
                size = 300.dp
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.55f)
                            )
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
            ) {
                IconButton(onClick = { menu = true }) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = stringResource(R.string.pm_playlist_actions),
                        tint = Color.White
                    )
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.pm_action_rename)) },
                        onClick = {
                            menu = false
                            onRename()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.pm_delete_playlist_title)) },
                        onClick = {
                            menu = false
                            onDelete()
                        }
                    )
                }
            }
            Text(
                text = stringResource(R.string.pm_tracks_short, playlist.trackCount),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.9f),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = playlist.name,
            style = MaterialTheme.typography.titleMedium,
            color = colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
