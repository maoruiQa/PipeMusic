package org.schabi.newpipe.music.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.ViewList
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.schabi.newpipe.R
import org.schabi.newpipe.music.data.db.SearchHistoryEntity
import org.schabi.newpipe.music.stream.MusicService
import org.schabi.newpipe.music.ui.components.Artwork
import org.schabi.newpipe.music.ui.components.EmptyState
import org.schabi.newpipe.music.ui.components.ServiceChip
import org.schabi.newpipe.music.ui.formatDuration
import org.schabi.newpipe.music.ui.viewmodel.SearchSong
import org.schabi.newpipe.music.ui.viewmodel.SearchUiState

@Composable
fun SearchScreen(
    state: SearchUiState,
    services: List<MusicService>,
    recentSearches: List<SearchHistoryEntity>,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onSearchFor: (String) -> Unit,
    onSelectService: (Int) -> Unit,
    onPlay: (SearchSong) -> Unit,
    onDownload: (SearchSong) -> Unit,
    onAddToPlaylist: (SearchSong) -> Unit,
    onOpenLink: (String) -> Unit,
    onLoadMore: () -> Unit,
    onDeleteHistory: (String, Int) -> Unit,
    activeDownloads: Int = 0,
    onOpenDownloads: () -> Unit = {},
    supportsParts: (Int) -> Boolean = { false },
    onShowParts: (SearchSong) -> Unit = {}
) {
    val colors = MaterialTheme.colorScheme
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Row(
                modifier = Modifier.padding(top = 14.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.pm_search_title),
                    style = MaterialTheme.typography.displaySmall,
                    color = colors.onSurface,
                    modifier = Modifier.weight(1f)
                )
                if (activeDownloads > 0) {
                    Text(
                        text = stringResource(
                            R.string.pm_search_active_downloads,
                            activeDownloads
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.primary
                    )
                    IconButton(onClick = onOpenDownloads) {
                        Icon(
                            imageVector = Icons.Rounded.CloudDownload,
                            contentDescription = stringResource(R.string.pm_action_view_downloads),
                            tint = colors.primary
                        )
                    }
                }
            }
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.pm_search_placeholder)) },
                leadingIcon = {
                    Icon(Icons.Rounded.Search, contentDescription = null, tint = colors.primary)
                },
                trailingIcon = {
                    if (state.loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(onClick = {
                            focusManager.clearFocus()
                            onSubmit()
                        }) {
                            Icon(
                                Icons.Rounded.Search,
                                contentDescription = stringResource(R.string.pm_search_title)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    focusManager.clearFocus()
                    onSubmit()
                })
            )
            if (state.query.trim().startsWith("http")) {
                TextButton(onClick = { onOpenLink(state.query.trim()) }) {
                    Text(stringResource(R.string.pm_search_detect_link))
                }
            }
            Spacer(Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(services) { service ->
                    ServiceChip(
                        label = service.shortName,
                        selected = state.serviceId == service.id,
                        onClick = { onSelectService(service.id) }
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
        }

        if (state.query.isBlank() && state.songs.isEmpty()) {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                if (recentSearches.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.pm_search_recent),
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    recentSearches.take(8).forEach { history ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onSearchFor(history.query) }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = null,
                                tint = colors.onSurfaceVariant,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = history.query,
                                style = MaterialTheme.typography.bodyLarge,
                                color = colors.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = stringResource(R.string.pm_action_delete),
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.onSurfaceVariant,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable {
                                        onDeleteHistory(history.query, history.serviceId)
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
            EmptyState(
                title = stringResource(R.string.pm_search_empty_title),
                description = stringResource(R.string.pm_search_empty_desc),
                icon = Icons.Rounded.SearchOff
            )
            return
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 28.dp)
        ) {
            if (state.loading && state.songs.isEmpty()) {
                items(6) { SkeletonRow() }
            }
            items(state.songs) { song ->
                SearchResultRow(
                    song = song,
                    onPlay = { onPlay(song) },
                    onDownload = { onDownload(song) },
                    onAddToPlaylist = { onAddToPlaylist(song) },
                    onShowParts = if (supportsParts(song.serviceId)) {
                        { onShowParts(song) }
                    } else {
                        null
                    }
                )
            }
            if (state.loadingMore) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    }
                }
            } else if (state.nextPage != null) {
                item {
                    TextButton(
                        onClick = onLoadMore,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.pm_search_load_more))
                    }
                }
            }
            if (state.error != null) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = state.error,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.error
                        )
                        TextButton(onClick = onSubmit) {
                            Text(stringResource(R.string.pm_action_retry))
                        }
                    }
                }
            }
            if (state.searchSubmitted && !state.loading && state.songs.isEmpty() && state.error == null) {
                item {
                    EmptyState(
                        title = stringResource(R.string.pm_search_no_result_title),
                        description = stringResource(R.string.pm_search_no_result_desc),
                        icon = Icons.Rounded.SearchOff
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchResultRow(
    song: SearchSong,
    onPlay: () -> Unit,
    onDownload: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onShowParts: (() -> Unit)?
) {
    var menu by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onPlay() }
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Artwork(
            source = song.thumbnailUrl,
            modifier = Modifier.size(54.dp),
            shape = RoundedCornerShape(15.dp),
            size = 54.dp
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist.ifBlank { stringResource(R.string.pm_unknown_artist) },
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = formatDuration(song.durationMs),
            style = MaterialTheme.typography.labelMedium,
            color = colors.onSurfaceVariant
        )
        IconButton(onClick = onDownload, modifier = Modifier.size(38.dp)) {
            Icon(
                imageVector = Icons.Rounded.CloudDownload,
                contentDescription = stringResource(R.string.pm_action_download_audio),
                tint = colors.primary
            )
        }
        Box {
            IconButton(onClick = { menu = true }, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = stringResource(R.string.pm_action_more),
                    tint = colors.onSurfaceVariant
                )
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.pm_action_play)) },
                    leadingIcon = {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                    },
                    onClick = {
                        menu = false
                        onPlay()
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.pm_action_download_audio)) },
                    leadingIcon = {
                        Icon(Icons.Rounded.CloudDownload, contentDescription = null)
                    },
                    onClick = {
                        menu = false
                        onDownload()
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.pm_action_add_to_playlist)) },
                    leadingIcon = {
                        Icon(Icons.Rounded.PlaylistAdd, contentDescription = null)
                    },
                    onClick = {
                        menu = false
                        onAddToPlaylist()
                    }
                )
                if (onShowParts != null) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.pm_action_all_parts)) },
                        leadingIcon = {
                            Icon(Icons.Rounded.ViewList, contentDescription = null)
                        },
                        onClick = {
                            menu = false
                            onShowParts()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SkeletonRow() {
    val colors = MaterialTheme.colorScheme
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(15.dp))
                .alpha(alpha)
                .background(colors.surfaceContainerHigh)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.68f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .alpha(alpha)
                    .background(colors.surfaceContainerHigh)
            )
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.42f)
                    .height(11.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .alpha(alpha)
                    .background(colors.surfaceContainerHigh)
            )
        }
    }
}
