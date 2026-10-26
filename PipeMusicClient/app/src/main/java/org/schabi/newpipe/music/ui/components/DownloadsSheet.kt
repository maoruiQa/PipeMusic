package org.schabi.newpipe.music.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.schabi.newpipe.R
import org.schabi.newpipe.music.data.db.DOWNLOAD_COMPLETED
import org.schabi.newpipe.music.data.db.DOWNLOAD_FAILED
import org.schabi.newpipe.music.data.db.DOWNLOAD_NONE
import org.schabi.newpipe.music.data.db.DOWNLOAD_QUEUED
import org.schabi.newpipe.music.data.db.DOWNLOADING
import org.schabi.newpipe.music.data.db.TrackEntity

/**
 * Live view of every track that has ever entered the download queue. Progress is read from the
 * Room flow, so the rows update while the sheet stays open.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsSheet(
    tracks: List<TrackEntity>,
    onDismiss: () -> Unit,
    onCancel: (TrackEntity) -> Unit,
    onRetry: (TrackEntity) -> Unit,
    onPlay: (TrackEntity) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val entries = tracks
        .filter { it.downloadState != DOWNLOAD_NONE }
        .sortedWith(
            compareBy<TrackEntity>(
                {
                    when (it.downloadState) {
                        DOWNLOADING, DOWNLOAD_QUEUED -> 0
                        DOWNLOAD_FAILED -> 1
                        else -> 2
                    }
                },
                { -it.addedAt }
            )
        )
    val active = entries.count {
        it.downloadState == DOWNLOADING || it.downloadState == DOWNLOAD_QUEUED
    }
    val completed = entries.count { it.downloadState == DOWNLOAD_COMPLETED }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(bottom = 26.dp)
        ) {
            Text(
                text = stringResource(R.string.pm_downloads_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.pm_downloads_subtitle, active, completed),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            if (entries.isEmpty()) {
                EmptyState(
                    title = stringResource(R.string.pm_downloads_empty_title),
                    description = stringResource(R.string.pm_downloads_empty_desc),
                    icon = Icons.Rounded.CloudDownload
                )
            } else {
                LazyColumn(
                    modifier = Modifier.height(460.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(entries, key = { it.id }) { track ->
                        DownloadRow(
                            track = track,
                            onCancel = { onCancel(track) },
                            onRetry = { onRetry(track) },
                            onPlay = { onPlay(track) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadRow(
    track: TrackEntity,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onPlay: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Artwork(
            source = track.artworkUri ?: track.thumbnailUrl,
            size = 46.dp,
            modifier = Modifier.size(46.dp),
            shape = RoundedCornerShape(13.dp)
        )
        Spacer(Modifier.width(11.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val status = when (track.downloadState) {
                DOWNLOADING -> stringResource(
                    R.string.pm_download_status_downloading,
                    track.downloadProgress
                )
                DOWNLOAD_QUEUED -> stringResource(R.string.pm_download_status_queued)
                DOWNLOAD_FAILED -> track.downloadError?.takeIf { it.isNotBlank() }?.let {
                    stringResource(R.string.pm_download_status_failed_reason, it)
                } ?: stringResource(R.string.pm_download_status_failed)
                DOWNLOAD_COMPLETED -> stringResource(R.string.pm_download_status_completed)
                else -> ""
            }
            Text(
                text = status,
                style = MaterialTheme.typography.bodyMedium,
                color = when (track.downloadState) {
                    DOWNLOAD_FAILED -> colors.error
                    DOWNLOADING, DOWNLOAD_QUEUED -> colors.primary
                    else -> colors.onSurfaceVariant
                },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (track.downloadState == DOWNLOADING) {
                Spacer(Modifier.height(5.dp))
                LinearProgressIndicator(
                    progress = { track.downloadProgress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape)
                )
            }
        }
        Spacer(Modifier.width(6.dp))
        when (track.downloadState) {
            DOWNLOADING, DOWNLOAD_QUEUED -> IconButton(onClick = onCancel) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.pm_download_cancel),
                    tint = colors.onSurfaceVariant
                )
            }

            DOWNLOAD_FAILED -> IconButton(onClick = onRetry) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = stringResource(R.string.pm_download_retry),
                    tint = colors.primary
                )
            }

            DOWNLOAD_COMPLETED -> IconButton(onClick = onPlay) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = stringResource(R.string.pm_action_play),
                    tint = colors.primary
                )
            }
        }
    }
}
