package org.schabi.newpipe.music.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.ViewList
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import kotlin.math.max
import org.schabi.newpipe.R
import org.schabi.newpipe.music.data.db.DOWNLOAD_COMPLETED
import org.schabi.newpipe.music.data.db.DOWNLOAD_FAILED
import org.schabi.newpipe.music.data.db.DOWNLOAD_NONE
import org.schabi.newpipe.music.data.db.DOWNLOAD_QUEUED
import org.schabi.newpipe.music.data.db.DOWNLOADING
import org.schabi.newpipe.music.playback.MusicPlayerController
import org.schabi.newpipe.music.playback.PlaybackQueue
import org.schabi.newpipe.music.playback.PlayerUiState
import org.schabi.newpipe.music.ui.formatDuration

@Composable
fun FullPlayer(
    state: PlayerUiState,
    gradient: List<Color>,
    isFavorite: Boolean,
    isDownloaded: Boolean,
    downloadState: Int = DOWNLOAD_NONE,
    downloadProgress: Int = 0,
    artworkTint: Boolean,
    onClose: () -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleFavorite: () -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    onQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onDownload: () -> Unit,
    onSleep: () -> Unit,
    modifier: Modifier = Modifier,
    /** "P3 / 12" when the song is a part of a multi-part video. */
    partLabel: String? = null,
    onParts: (() -> Unit)? = null
) {
    val track = state.current ?: return
    val colors = MaterialTheme.colorScheme
    val livePosition by MusicPlayerController.position.collectAsStateWithLifecycle()
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(livePosition, track.id) {
        if (!dragging) {
            dragValue = livePosition.toFloat()
        }
    }

    val duration = if (state.durationMs > 0) state.durationMs else track.durationMs
    val artworkColor = rememberArtworkTint(
        track.artworkUri ?: track.thumbnailUrl,
        artworkTint
    )
    val topColor = artworkColor ?: gradient.firstOrNull() ?: colors.primary

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .background(
                Brush.verticalGradient(
                    listOf(
                        topColor.copy(alpha = 0.5f),
                        colors.background.copy(alpha = 0.97f),
                        colors.background
                    )
                )
            )
    ) {
        Artwork(
            source = track.artworkUri ?: track.thumbnailUrl,
            modifier = Modifier
                .fillMaxSize()
                .blur(90.dp)
                .alpha(0.25f),
            shape = RoundedCornerShape(0.dp),
            size = 480.dp
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            colors.background.copy(alpha = 0.12f),
                            colors.background.copy(alpha = 0.8f),
                            colors.background
                        )
                    )
                )
        )

        val artSize = min(maxWidth * 0.7f, maxHeight * 0.34f)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.pm_collapse_player),
                        tint = colors.onSurface,
                        modifier = Modifier.size(30.dp)
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.pm_now_playing),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onSurfaceVariant
                    )
                    Text(
                        text = track.artist,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = onQueue) {
                    Icon(
                        imageVector = Icons.Rounded.QueueMusic,
                        contentDescription = stringResource(R.string.pm_queue_title),
                        tint = colors.onSurface
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            Box(
                modifier = Modifier
                    .size(artSize)
                    .align(Alignment.CenterHorizontally)
                    .shadow(30.dp, RoundedCornerShape(34.dp), clip = false)
            ) {
                Artwork(
                    source = track.artworkUri ?: track.thumbnailUrl,
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(34.dp),
                    size = artSize
                )
            }
            Spacer(Modifier.height(24.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = colors.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = track.album.ifBlank { track.artist },
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (isFavorite) {
                            Icons.Rounded.Favorite
                        } else {
                            Icons.Rounded.FavoriteBorder
                        },
                        contentDescription = stringResource(
                            if (isFavorite) R.string.pm_action_unfavorite
                            else R.string.pm_action_favorite
                        ),
                        tint = if (isFavorite) colors.tertiary else colors.onSurfaceVariant
                    )
                }
            }

            if (partLabel != null && onParts != null) {
                Spacer(Modifier.height(8.dp))
                PartsChip(label = partLabel, onClick = onParts)
            }

            Spacer(Modifier.height(4.dp))
            PlayerSeekBar(
                positionMs = if (dragging) dragValue.toLong() else livePosition,
                durationMs = duration,
                onDrag = {
                    dragging = true
                    dragValue = it.toFloat()
                },
                onSeek = {
                    dragging = false
                    onSeek(it)
                }
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = formatDuration(if (dragging) dragValue.toLong() else livePosition),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = formatDuration(duration),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant
                )
            }

            if (state.error != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = state.error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.error
                )
            }

            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onShuffle) {
                    Icon(
                        imageVector = Icons.Rounded.Shuffle,
                        contentDescription = stringResource(R.string.pm_action_shuffle),
                        tint = if (state.shuffle) colors.primary else colors.onSurfaceVariant
                    )
                }
                IconButton(onClick = onPrevious, modifier = Modifier.size(54.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.SkipPrevious,
                        contentDescription = stringResource(R.string.pm_action_previous),
                        tint = colors.onSurface,
                        modifier = Modifier.size(36.dp)
                    )
                }
                FilledIconButton(
                    onClick = onToggle,
                    modifier = Modifier.size(74.dp),
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = if (state.isPlaying) {
                            Icons.Rounded.Pause
                        } else {
                            Icons.Rounded.PlayArrow
                        },
                        contentDescription = stringResource(
                            if (state.isPlaying) R.string.pm_action_pause
                            else R.string.pm_action_play
                        ),
                        tint = colors.onPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                }
                IconButton(onClick = onNext, modifier = Modifier.size(54.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = stringResource(R.string.pm_action_next),
                        tint = colors.onSurface,
                        modifier = Modifier.size(36.dp)
                    )
                }
                IconButton(onClick = onRepeat) {
                    Icon(
                        imageVector = if (state.repeatMode == PlaybackQueue.REPEAT_ONE) {
                            Icons.Rounded.RepeatOne
                        } else {
                            Icons.Rounded.Repeat
                        },
                        contentDescription = stringResource(R.string.pm_action_repeat),
                        tint = when (state.repeatMode) {
                            PlaybackQueue.REPEAT_OFF -> colors.onSurfaceVariant.copy(alpha = 0.45f)
                            else -> colors.primary
                        }
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            val downloadIcon = when {
                downloadState == DOWNLOAD_FAILED -> Icons.Rounded.Refresh
                downloadState == DOWNLOAD_COMPLETED || isDownloaded ->
                    Icons.Rounded.CheckCircle
                else -> Icons.Rounded.Download
            }
            val downloadLabel = when (downloadState) {
                DOWNLOADING -> stringResource(
                    R.string.pm_download_status_downloading,
                    downloadProgress
                )
                DOWNLOAD_QUEUED -> stringResource(R.string.pm_download_status_queued)
                DOWNLOAD_FAILED -> stringResource(R.string.pm_download_retry)
                DOWNLOAD_COMPLETED -> stringResource(R.string.pm_action_downloaded)
                else -> stringResource(
                    if (isDownloaded) R.string.pm_action_downloaded
                    else R.string.pm_action_download
                )
            }
            val downloadTint = when (downloadState) {
                DOWNLOAD_FAILED -> colors.error
                DOWNLOADING, DOWNLOAD_QUEUED, DOWNLOAD_COMPLETED -> colors.primary
                else -> null
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(colors.surfaceContainer.copy(alpha = 0.55f))
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                PlayerAction(
                    icon = Icons.Rounded.PlaylistAdd,
                    label = stringResource(R.string.pm_action_add_to_playlist),
                    onClick = onAddToPlaylist
                )
                // A part of a multi-part video can be downloaded on its own or together with
                // other parts picked from the parts list, which also merges them.
                var downloadMenu by remember { mutableStateOf(false) }
                Box {
                    PlayerAction(
                        icon = downloadIcon,
                        label = downloadLabel,
                        tint = downloadTint,
                        progress = if (downloadState == DOWNLOADING) {
                            (downloadProgress / 100f).coerceIn(0f, 1f)
                        } else {
                            null
                        },
                        onClick = if (onParts != null) {
                            { downloadMenu = true }
                        } else {
                            onDownload
                        }
                    )
                    if (onParts != null) {
                        DropdownMenu(
                            expanded = downloadMenu,
                            onDismissRequest = { downloadMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.pm_download_this_part)) },
                                leadingIcon = {
                                    Icon(Icons.Rounded.Download, contentDescription = null)
                                },
                                onClick = {
                                    downloadMenu = false
                                    onDownload()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.pm_download_choose_parts)) },
                                leadingIcon = {
                                    Icon(Icons.Rounded.ViewList, contentDescription = null)
                                },
                                onClick = {
                                    downloadMenu = false
                                    onParts()
                                }
                            )
                        }
                    }
                }
                if (onParts != null) {
                    PlayerAction(
                        icon = Icons.Rounded.ViewList,
                        label = stringResource(R.string.pm_action_parts),
                        onClick = onParts
                    )
                }
                PlayerAction(
                    icon = Icons.Rounded.Timer,
                    label = stringResource(
                        if (state.sleepTimerEndsAt > 0) R.string.pm_sleep_active
                        else R.string.pm_sleep
                    ),
                    tint = if (state.sleepTimerEndsAt > 0) colors.primary else null,
                    onClick = onSleep
                )
                PlayerAction(
                    icon = Icons.Rounded.QueueMusic,
                    label = stringResource(R.string.pm_action_queue),
                    onClick = onQueue
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

/** Marks a part of a multi-part video and opens the list of all its parts. */
@Composable
private fun PartsChip(label: String, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(colors.primary.copy(alpha = 0.14f))
            .clickable(onClick = onClick)
            .padding(start = 10.dp, end = 6.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.ViewList,
            contentDescription = null,
            tint = colors.primary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = stringResource(R.string.pm_part_chip, label),
            style = MaterialTheme.typography.labelLarge,
            color = colors.primary,
            maxLines = 1
        )
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = colors.primary,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun PlayerSeekBar(
    positionMs: Long,
    durationMs: Long,
    onDrag: (Long) -> Unit,
    onSeek: (Long) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val duration = durationMs.coerceAtLeast(1)
    val fraction = (positionMs.toFloat() / duration).coerceIn(0f, 1f)
    var lastFraction by remember { mutableFloatStateOf(fraction) }

    LaunchedEffect(positionMs) {
        lastFraction = (positionMs.toFloat() / duration).coerceIn(0f, 1f)
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .pointerInput(duration) {
                detectTapGestures { offset ->
                    val tapped = (offset.x / size.width).coerceIn(0f, 1f)
                    lastFraction = tapped
                    onDrag((tapped * duration).toLong())
                    onSeek((tapped * duration).toLong())
                }
            }
            .pointerInput(duration) {
                detectDragGestures(
                    onDragStart = { offset ->
                        lastFraction = (offset.x / size.width).coerceIn(0f, 1f)
                        onDrag((lastFraction * duration).toLong())
                    },
                    onDragEnd = {
                        onSeek((lastFraction * duration).toLong())
                    },
                    onDragCancel = {
                        onSeek((lastFraction * duration).toLong())
                    }
                ) { change, _ ->
                    lastFraction = (change.position.x / size.width).coerceIn(0f, 1f)
                    onDrag((lastFraction * duration).toLong())
                    change.consume()
                }
            }
    ) {
        val trackHeight = 4.dp.toPx()
        val centerY = size.height / 2f
        drawRoundRect(
            color = colors.surfaceVariant,
            topLeft = Offset(0f, centerY - trackHeight / 2f),
            size = Size(size.width, trackHeight),
            cornerRadius = CornerRadius(trackHeight / 2f)
        )
        drawRoundRect(
            brush = Brush.horizontalGradient(listOf(colors.primary, colors.tertiary)),
            topLeft = Offset(0f, centerY - trackHeight / 2f),
            size = Size(size.width * fraction, trackHeight),
            cornerRadius = CornerRadius(trackHeight / 2f)
        )
        val radius = 7.dp.toPx()
        val centerX = radius + fraction * (size.width - radius * 2)
        drawCircle(
            color = colors.primary,
            radius = radius,
            center = Offset(centerX, centerY)
        )
        drawCircle(
            color = colors.onPrimary.copy(alpha = 0.85f),
            radius = 2.5.dp.toPx(),
            center = Offset(centerX, centerY)
        )
    }
}

@Composable
private fun rememberArtworkTint(source: String?, enabled: Boolean): Color? {
    val color by produceState<Color?>(initialValue = null, source, enabled) {
        value = if (!enabled || source.isNullOrBlank()) {
            null
        } else {
            ArtworkCache.load(source, 96)?.let { bitmap -> averageTint(bitmap) }
        }
    }
    return color
}

private fun averageTint(bitmap: Bitmap): Color {
    val stepX = max(1, bitmap.width / 8)
    val stepY = max(1, bitmap.height / 8)
    var red = 0L
    var green = 0L
    var blue = 0L
    var samples = 0L
    var y = 0
    while (y < bitmap.height) {
        var x = 0
        while (x < bitmap.width) {
            val pixel = bitmap.getPixel(x, y)
            red += (pixel shr 16) and 0xFF
            green += (pixel shr 8) and 0xFF
            blue += pixel and 0xFF
            samples += 1
            x += stepX
        }
        y += stepY
    }
    if (samples == 0L) {
        return Color(0xFF7C5CFF)
    }
    val hsv = FloatArray(3)
    AndroidColor.RGBToHSV(
        (red / samples).toInt(),
        (green / samples).toInt(),
        (blue / samples).toInt(),
        hsv
    )
    hsv[1] = hsv[1].coerceAtLeast(0.42f)
    hsv[2] = hsv[2].coerceIn(0.45f, 0.82f)
    return Color(AndroidColor.HSVToColor(hsv))
}

@Composable
private fun PlayerAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color? = null,
    progress: Float? = null
) {
    val colors = MaterialTheme.colorScheme
    val effectiveTint = tint ?: colors.onSurfaceVariant
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = onClick) {
            if (progress != null) {
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.size(26.dp),
                        color = colors.primary,
                        trackColor = colors.surfaceVariant,
                        strokeWidth = 2.dp
                    )
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = colors.primary,
                        modifier = Modifier.size(13.dp)
                    )
                }
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = effectiveTint
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = effectiveTint
        )
    }
}
