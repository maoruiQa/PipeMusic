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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.schabi.newpipe.R
import org.schabi.newpipe.music.data.db.TrackEntity
import org.schabi.newpipe.music.playback.PlayerUiState
import org.schabi.newpipe.music.stream.MusicService
import org.schabi.newpipe.music.ui.components.Artwork
import org.schabi.newpipe.music.ui.components.EmptyState
import org.schabi.newpipe.music.ui.components.GlassCard
import org.schabi.newpipe.music.ui.components.SectionTitle
import org.schabi.newpipe.music.ui.components.ServiceChip
import org.schabi.newpipe.music.ui.components.SongRow
import java.util.Calendar

@Composable
fun HomeScreen(
    tracks: List<TrackEntity>,
    recentlyPlayed: List<TrackEntity>,
    recentlyAdded: List<TrackEntity>,
    player: PlayerUiState,
    services: List<MusicService>,
    onPlayTrack: (TrackEntity) -> Unit,
    onPlayList: (List<TrackEntity>, Int) -> Unit,
    onOpenSearch: (Int) -> Unit,
    onOpenTheme: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenPlayer: () -> Unit,
    onToggle: () -> Unit,
    onImport: () -> Unit,
    onImportFile: (() -> Unit)? = null
) {
    val colors = MaterialTheme.colorScheme
    val hero = player.current ?: recentlyPlayed.firstOrNull()
    val greeting = rememberGreeting()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = greeting,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant
                    )
                    Text(
                        text = "PipeMusic",
                        style = MaterialTheme.typography.displaySmall,
                        color = colors.onSurface
                    )
                }
                IconButton(onClick = onOpenTheme) {
                    Icon(
                        imageVector = Icons.Rounded.Palette,
                        contentDescription = stringResource(R.string.pm_theme_title),
                        tint = colors.onSurface
                    )
                }
            }
        }

        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                onClick = { onOpenSearch(player.current?.serviceId ?: 0) }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = colors.primary
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.pm_search_placeholder),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant
                    )
                }
            }
        }

        item {
            if (hero != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(186.dp)
                        .clip(RoundedCornerShape(30.dp))
                        .clickable { onOpenPlayer() }
                ) {
                    Artwork(
                        source = hero.artworkUri ?: hero.thumbnailUrl,
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
                                        Color.Black.copy(alpha = 0.15f),
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
                            text = stringResource(
                                if (player.current != null) R.string.pm_home_continue
                                else R.string.pm_home_last_played
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = hero.title,
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = hero.artist,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.72f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Button(
                                onClick = {
                                    if (player.current?.id == hero.id) {
                                        onToggle()
                                    } else {
                                        onPlayTrack(hero)
                                    }
                                },
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = Color.Black
                                )
                            ) {
                                Icon(
                                    imageVector = if (player.isPlaying && player.current?.id == hero.id) {
                                        Icons.Rounded.Pause
                                    } else {
                                        Icons.Rounded.PlayArrow
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    stringResource(
                                        if (player.isPlaying && player.current?.id == hero.id) {
                                            R.string.pm_action_pause
                                        } else {
                                            R.string.pm_action_play
                                        }
                                    )
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.pm_home_open_player),
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable { onOpenPlayer() }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            } else {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(30.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(colors.primary.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MusicNote,
                                contentDescription = null,
                                tint = colors.primary
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.pm_home_start_title),
                                style = MaterialTheme.typography.titleLarge,
                                color = colors.onSurface
                            )
                            Text(
                                text = stringResource(R.string.pm_home_start_desc),
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = onImport,
                            shape = CircleShape,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.FolderOpen,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.pm_home_import_local))
                        }
                        Button(
                            onClick = { onOpenSearch(0) },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.surfaceContainerHighest,
                                contentColor = colors.onSurface
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.pm_home_online_search))
                        }
                    }
                    if (onImportFile != null) {
                        TextButton(onClick = onImportFile) {
                            Icon(
                                imageVector = Icons.Rounded.FolderOpen,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(stringResource(R.string.pm_action_import_file))
                        }
                    }
                }
            }
        }

        item {
            Column {
                SectionTitle(
                    title = stringResource(R.string.pm_home_platforms),
                    actionLabel = stringResource(R.string.pm_action_all),
                    onAction = { onOpenSearch(0) }
                )
                Spacer(Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(services) { service ->
                        ServiceChip(
                            label = service.shortName,
                            selected = false,
                            onClick = { onOpenSearch(service.id) }
                        )
                    }
                }
            }
        }

        if (recentlyPlayed.isNotEmpty()) {
            item {
                Column {
                    SectionTitle(
                        title = stringResource(R.string.pm_home_recent_played),
                        actionLabel = stringResource(R.string.pm_home_open_player),
                        onAction = onOpenPlayer
                    )
                    Spacer(Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(recentlyPlayed) { track ->
                            RecentCard(track = track) { onPlayTrack(track) }
                        }
                    }
                }
            }
        }

        if (recentlyAdded.isNotEmpty()) {
            item {
                SectionTitle(
                    title = stringResource(R.string.pm_home_recent_added),
                    actionLabel = stringResource(R.string.pm_action_view_library),
                    onAction = onOpenLibrary
                )
            }
            items(recentlyAdded.take(5)) { track ->
                SongRow(
                    track = track,
                    isCurrent = player.current?.id == track.id,
                    isPlaying = player.isPlaying,
                    onClick = {
                        val index = recentlyAdded.indexOf(track)
                        onPlayList(recentlyAdded, index.coerceAtLeast(0))
                    },
                    onMore = { onOpenLibrary() }
                )
            }
        }

        if (tracks.isEmpty() && recentlyPlayed.isEmpty()) {
            item {
                EmptyState(
                    title = stringResource(R.string.pm_home_empty_title),
                    description = stringResource(R.string.pm_home_empty_desc),
                    icon = Icons.Rounded.MusicNote,
                    actionLabel = stringResource(R.string.pm_action_go_search),
                    onAction = { onOpenSearch(0) }
                )
            }
        }
    }
}

@Composable
private fun RecentCard(track: TrackEntity, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(126.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() }
    ) {
        Artwork(
            source = track.artworkUri ?: track.thumbnailUrl,
            modifier = Modifier
                .fillMaxWidth()
                .height(126.dp),
            shape = RoundedCornerShape(20.dp),
            size = 160.dp
        )
        Spacer(Modifier.height(8.dp))
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

@Composable
private fun rememberGreeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour in 5..10 -> stringResource(R.string.pm_greeting_morning)
        hour in 11..13 -> stringResource(R.string.pm_greeting_noon)
        hour in 14..18 -> stringResource(R.string.pm_greeting_afternoon)
        hour in 19..22 -> stringResource(R.string.pm_greeting_evening)
        else -> stringResource(R.string.pm_greeting_night)
    }
}

@Composable
private fun SeeAllRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.pm_action_all),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Rounded.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
    }
}
