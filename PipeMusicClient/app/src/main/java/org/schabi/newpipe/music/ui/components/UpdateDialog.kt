package org.schabi.newpipe.music.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.schabi.newpipe.BuildConfig
import org.schabi.newpipe.R
import org.schabi.newpipe.music.update.ReleaseInfo

@Composable
fun UpdateDialog(
    release: ReleaseInfo,
    onDownload: () -> Unit,
    onSkip: () -> Unit,
    onLater: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onLater,
        title = { Text(stringResource(R.string.pm_update_available_title, release.version)) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 320.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = stringResource(
                        R.string.pm_update_current_version,
                        BuildConfig.VERSION_NAME
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (release.notes.isNotBlank()) {
                    Text(
                        text = release.notes,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.heightIn(min = 0.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDownload) {
                Text(stringResource(R.string.pm_update_download))
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onSkip) {
                    Text(stringResource(R.string.pm_update_skip))
                }
                TextButton(onClick = onLater) {
                    Text(stringResource(R.string.pm_update_later))
                }
            }
        }
    )
}
