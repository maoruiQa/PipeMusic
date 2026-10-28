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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.schabi.newpipe.BuildConfig
import org.schabi.newpipe.R
import org.schabi.newpipe.music.data.MusicAccountService
import org.schabi.newpipe.music.data.MusicAccountState
import org.schabi.newpipe.music.data.MusicLocale
import org.schabi.newpipe.music.data.ThemeConfig
import org.schabi.newpipe.music.data.ThemePresets
import org.schabi.newpipe.music.data.db.LibraryStats
import org.schabi.newpipe.music.ui.components.GlassCard
import org.schabi.newpipe.music.ui.formatBytes
import org.schabi.newpipe.music.ui.formatTotalDuration

@Composable
fun ProfileScreen(
    stats: LibraryStats,
    playlistCount: Int,
    downloadsCount: Int,
    themeConfig: ThemeConfig,
    downloadQuality: String,
    language: String,
    accountState: MusicAccountState,
    activeDownloads: Int,
    onOpenTheme: () -> Unit,
    onQualityChange: (String) -> Unit,
    onSleepTimer: () -> Unit,
    autoUpdateCheck: Boolean,
    checkingUpdate: Boolean,
    onAutoUpdateChange: (Boolean) -> Unit,
    onCheckUpdate: () -> Unit,
    onClearHistory: () -> Unit,
    onDeleteAll: () -> Unit,
    onOpenDownloads: () -> Unit,
    onLanguageChange: (String) -> Unit,
    onLogin: (MusicAccountService) -> Unit,
    onLogout: (MusicAccountService) -> Unit,
    onSaveManualCookie: (MusicAccountService, String, String?) -> Boolean
) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    var qualityDialog by remember { mutableStateOf(false) }
    var clearHistoryDialog by remember { mutableStateOf(false) }
    var deleteAllDialog by remember { mutableStateOf(false) }
    var platformsDialog by remember { mutableStateOf(false) }
    var licenseDialog by remember { mutableStateOf(false) }
    var aboutDialog by remember { mutableStateOf(false) }
    var languageDialog by remember { mutableStateOf(false) }
    var youtubeWarning by remember { mutableStateOf(false) }
    var manualCookieDialog by remember { mutableStateOf(false) }
    var manualCookieService by remember { mutableStateOf(MusicAccountService.BILIBILI) }
    var manualCookieText by remember { mutableStateOf("") }
    var manualPoTokenText by remember { mutableStateOf("") }
    var manualCookieInvalid by remember { mutableStateOf(false) }

    val qualityLabel = when (downloadQuality) {
        "low" -> stringResource(R.string.pm_quality_low)
        "medium" -> stringResource(R.string.pm_quality_medium)
        else -> stringResource(R.string.pm_quality_high)
    }
    val themeLabel = if (themeConfig.customSeed != null) {
        stringResource(R.string.pm_theme_custom_color)
    } else {
        stringResource(ThemePresets.find(themeConfig.presetId).labelRes)
    }
    val modeLabel = stringResource(
        when (themeConfig.darkMode) {
            1 -> R.string.pm_theme_mode_light
            2 -> R.string.pm_theme_mode_dark
            else -> R.string.pm_theme_mode_system
        }
    )
    val languageLabel = stringResource(
        when (language) {
            MusicLocale.ENGLISH -> R.string.pm_language_english
            MusicLocale.CHINESE -> R.string.pm_language_chinese
            MusicLocale.JAPANESE -> R.string.pm_language_japanese
            MusicLocale.GERMAN -> R.string.pm_language_german
            MusicLocale.SPANISH -> R.string.pm_language_spanish
            MusicLocale.FRENCH -> R.string.pm_language_french
            MusicLocale.RUSSIAN -> R.string.pm_language_russian
            else -> R.string.pm_language_system
        }
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text(
                    text = stringResource(R.string.pm_profile_title),
                    style = MaterialTheme.typography.displaySmall,
                    color = colors.onSurface
                )
                Text(
                    text = stringResource(R.string.pm_profile_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant
                )
            }
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                colors.primary.copy(alpha = 0.95f),
                                colors.tertiary.copy(alpha = 0.8f)
                            )
                        )
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(22.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(62.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "PipeMusic",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White
                        )
                        Text(
                            text = stringResource(
                                R.string.pm_songs_and_playlists,
                                stats.trackCount,
                                playlistCount
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.82f)
                        )
                        Text(
                            text = stringResource(
                                R.string.pm_downloaded_summary,
                                downloadsCount,
                                formatBytes(stats.totalBytes)
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.72f)
                        )
                    }
                }
            }
        }

        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = onOpenTheme
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(colors.primary, colors.tertiary)
                                )
                            )
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.pm_theme_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.onSurface
                        )
                        Text(
                            text = "$themeLabel · $modeLabel",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = colors.onSurfaceVariant
                    )
                }
            }
        }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.pm_section_language),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurface,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                SettingsRow(
                    icon = Icons.Rounded.Language,
                    title = stringResource(R.string.pm_language),
                    subtitle = languageLabel,
                    onClick = { languageDialog = true }
                )
            }
        }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.pm_profile_section_playback),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurface,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                SettingsRow(
                    icon = Icons.Rounded.CloudDownload,
                    title = stringResource(R.string.pm_download_management),
                    subtitle = if (activeDownloads > 0 || downloadsCount > 0) {
                        stringResource(
                            R.string.pm_download_management_sub,
                            activeDownloads,
                            downloadsCount
                        )
                    } else {
                        stringResource(R.string.pm_download_management_sub_empty)
                    },
                    onClick = onOpenDownloads
                )
                SettingsRow(
                    icon = Icons.Rounded.CloudDownload,
                    title = stringResource(R.string.pm_download_quality),
                    subtitle = qualityLabel,
                    onClick = { qualityDialog = true }
                )
                SettingsRow(
                    icon = Icons.Rounded.Timer,
                    title = stringResource(R.string.pm_sleep_timer),
                    subtitle = stringResource(R.string.pm_sleep_timer_sub),
                    onClick = onSleepTimer
                )
                SettingsRow(
                    icon = Icons.Rounded.History,
                    title = stringResource(R.string.pm_clear_history),
                    subtitle = stringResource(R.string.pm_clear_history_sub),
                    onClick = { clearHistoryDialog = true }
                )
                SettingsRow(
                    icon = Icons.Rounded.DeleteSweep,
                    title = stringResource(R.string.pm_delete_all),
                    subtitle = stringResource(R.string.pm_delete_all_sub),
                    danger = true,
                    onClick = { deleteAllDialog = true }
                )
            }
        }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.pm_section_account),
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Rounded.AccountCircle,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(21.dp)
                    )
                }
                Text(
                    text = stringResource(R.string.pm_account_sub),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 6.dp)
                )
                MusicAccountService.entries.forEach { service ->
                    AccountRow(
                        service = service,
                        loggedIn = accountState.isLoggedIn(service),
                        onLogin = {
                            if (service == MusicAccountService.YOUTUBE) {
                                youtubeWarning = true
                            } else {
                                onLogin(service)
                            }
                        },
                        onLogout = { onLogout(service) }
                    )
                }
                SettingsRow(
                    icon = Icons.Rounded.Edit,
                    title = stringResource(R.string.pm_manual_cookie),
                    subtitle = stringResource(R.string.pm_manual_cookie_sub),
                    onClick = {
                        manualCookieText = ""
                        manualPoTokenText = ""
                        manualCookieInvalid = false
                        manualCookieDialog = true
                    }
                )
            }
        }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.pm_section_about),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurface,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                SettingsRow(
                    icon = Icons.Rounded.Public,
                    title = stringResource(R.string.pm_support_platforms),
                    subtitle = stringResource(R.string.pm_support_platforms_sub),
                    onClick = { platformsDialog = true }
                )
                SettingsRow(
                    icon = Icons.Rounded.Info,
                    title = stringResource(R.string.pm_license),
                    subtitle = stringResource(R.string.pm_license_sub),
                    onClick = { licenseDialog = true }
                )
                SettingsRow(
                    icon = Icons.Rounded.SystemUpdate,
                    title = stringResource(R.string.pm_check_update),
                    subtitle = stringResource(
                        if (checkingUpdate) R.string.pm_update_checking
                        else R.string.pm_check_update_sub
                    ),
                    onClick = onCheckUpdate,
                    trailing = if (checkingUpdate) {
                        {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    } else {
                        null
                    }
                )
                SettingsRow(
                    icon = Icons.Rounded.Autorenew,
                    title = stringResource(R.string.pm_auto_update),
                    subtitle = stringResource(R.string.pm_auto_update_sub),
                    onClick = { onAutoUpdateChange(!autoUpdateCheck) },
                    trailing = {
                        Switch(
                            checked = autoUpdateCheck,
                            onCheckedChange = onAutoUpdateChange
                        )
                    }
                )
                SettingsRow(
                    icon = Icons.Rounded.MusicNote,
                    title = stringResource(R.string.pm_about_app),
                    subtitle = stringResource(R.string.pm_version_fmt, BuildConfig.VERSION_NAME),
                    onClick = { aboutDialog = true }
                )
            }
        }

        item {
            Text(
                text = stringResource(R.string.pm_profile_note),
                style = MaterialTheme.typography.labelMedium,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }

    if (languageDialog) {
        val options = listOf(
            MusicLocale.SYSTEM to stringResource(R.string.pm_language_system),
            MusicLocale.ENGLISH to stringResource(R.string.pm_language_english),
            MusicLocale.CHINESE to stringResource(R.string.pm_language_chinese),
            MusicLocale.JAPANESE to stringResource(R.string.pm_language_japanese),
            MusicLocale.GERMAN to stringResource(R.string.pm_language_german),
            MusicLocale.SPANISH to stringResource(R.string.pm_language_spanish),
            MusicLocale.FRENCH to stringResource(R.string.pm_language_french),
            MusicLocale.RUSSIAN to stringResource(R.string.pm_language_russian)
        )
        AlertDialog(
            onDismissRequest = { languageDialog = false },
            title = { Text(stringResource(R.string.pm_language)) },
            text = {
                Column {
                    options.forEach { (value, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    languageDialog = false
                                    if (value != language) {
                                        onLanguageChange(value)
                                    }
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (language == value) {
                                    colors.primary
                                } else {
                                    colors.onSurface
                                },
                                modifier = Modifier.weight(1f)
                            )
                            if (language == value) {
                                Text(
                                    text = stringResource(R.string.pm_quality_current),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.primary
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { languageDialog = false }) {
                    Text(stringResource(R.string.pm_action_done))
                }
            }
        )
    }

    if (manualCookieDialog) {
        AlertDialog(
            onDismissRequest = { manualCookieDialog = false },
            title = { Text(stringResource(R.string.pm_manual_cookie_title)) },
            text = {
                Column {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        MusicAccountService.entries.forEach { service ->
                            FilterChip(
                                selected = manualCookieService == service,
                                onClick = {
                                    manualCookieService = service
                                    manualCookieInvalid = false
                                },
                                label = { Text(service.label) }
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = manualCookieText,
                        onValueChange = {
                            manualCookieText = it
                            manualCookieInvalid = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 112.dp),
                        placeholder = {
                            Text(stringResource(R.string.pm_manual_cookie_hint))
                        },
                        textStyle = MaterialTheme.typography.bodyMedium,
                        singleLine = false,
                        minLines = 4,
                        maxLines = 6
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.pm_manual_cookie_help),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant
                    )
                    if (manualCookieService == MusicAccountService.YOUTUBE) {
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = manualPoTokenText,
                            onValueChange = { manualPoTokenText = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = {
                                Text(stringResource(R.string.pm_manual_cookie_po))
                            },
                            singleLine = true
                        )
                    }
                    if (manualCookieInvalid) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.pm_manual_cookie_invalid),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.error
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val saved = onSaveManualCookie(
                        manualCookieService,
                        manualCookieText,
                        manualPoTokenText.trim().ifBlank { null }
                    )
                    if (saved) {
                        manualCookieDialog = false
                    } else {
                        manualCookieInvalid = true
                    }
                }) {
                    Text(stringResource(R.string.pm_action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { manualCookieDialog = false }) {
                    Text(stringResource(R.string.pm_action_cancel))
                }
            }
        )
    }

    if (youtubeWarning) {
        AlertDialog(
            onDismissRequest = { youtubeWarning = false },
            title = { Text(stringResource(R.string.pm_account_youtube_warning_title)) },
            text = { Text(stringResource(R.string.pm_account_youtube_warning_message)) },
            confirmButton = {
                TextButton(onClick = {
                    youtubeWarning = false
                    onLogin(MusicAccountService.YOUTUBE)
                }) {
                    Text(stringResource(R.string.pm_action_login))
                }
            },
            dismissButton = {
                TextButton(onClick = { youtubeWarning = false }) {
                    Text(stringResource(R.string.pm_action_cancel))
                }
            }
        )
    }

    if (qualityDialog) {
        val options = listOf(
            "high" to stringResource(R.string.pm_quality_high),
            "medium" to stringResource(R.string.pm_quality_medium),
            "low" to stringResource(R.string.pm_quality_low)
        )
        AlertDialog(
            onDismissRequest = { qualityDialog = false },
            title = { Text(stringResource(R.string.pm_download_quality)) },
            text = {
                Column {
                    options.forEach { (value, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    onQualityChange(value)
                                    qualityDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (downloadQuality == value) {
                                    colors.primary
                                } else {
                                    colors.onSurface
                                },
                                modifier = Modifier.weight(1f)
                            )
                            if (downloadQuality == value) {
                                Text(
                                    text = stringResource(R.string.pm_quality_current),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.primary
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { qualityDialog = false }) {
                    Text(stringResource(R.string.pm_action_done))
                }
            }
        )
    }

    if (clearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { clearHistoryDialog = false },
            title = { Text(stringResource(R.string.pm_clear_history_title)) },
            text = { Text(stringResource(R.string.pm_clear_history_message)) },
            confirmButton = {
                TextButton(onClick = {
                    onClearHistory()
                    clearHistoryDialog = false
                }) {
                    Text(stringResource(R.string.pm_action_clear))
                }
            },
            dismissButton = {
                TextButton(onClick = { clearHistoryDialog = false }) {
                    Text(stringResource(R.string.pm_action_cancel))
                }
            }
        )
    }

    if (deleteAllDialog) {
        AlertDialog(
            onDismissRequest = { deleteAllDialog = false },
            title = { Text(stringResource(R.string.pm_delete_all_title)) },
            text = { Text(stringResource(R.string.pm_delete_all_message)) },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteAll()
                    deleteAllDialog = false
                }) {
                    Text(
                        stringResource(R.string.pm_action_delete),
                        color = colors.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteAllDialog = false }) {
                    Text(stringResource(R.string.pm_action_cancel))
                }
            }
        )
    }

    if (platformsDialog) {
        AlertDialog(
            onDismissRequest = { platformsDialog = false },
            title = { Text(stringResource(R.string.pm_platforms_title)) },
            text = { Text(stringResource(R.string.pm_platforms_message)) },
            confirmButton = {
                TextButton(onClick = { platformsDialog = false }) {
                    Text(stringResource(R.string.pm_action_ok))
                }
            }
        )
    }

    if (licenseDialog) {
        AlertDialog(
            onDismissRequest = { licenseDialog = false },
            title = { Text(stringResource(R.string.pm_license_title)) },
            text = { Text(stringResource(R.string.pm_license_message)) },
            confirmButton = {
                TextButton(onClick = { licenseDialog = false }) {
                    Text(stringResource(R.string.pm_action_close))
                }
            }
        )
    }

    if (aboutDialog) {
        AlertDialog(
            onDismissRequest = { aboutDialog = false },
            title = { Text("PipeMusic") },
            text = {
                Text(
                    stringResource(
                        R.string.pm_about_message,
                        BuildConfig.VERSION_NAME,
                        formatTotalDuration(context, stats.totalDurationMs),
                        formatBytes(stats.totalBytes)
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = { aboutDialog = false }) {
                    Text(stringResource(R.string.pm_action_close))
                }
            }
        )
    }
}

@Composable
private fun AccountRow(
    service: MusicAccountService,
    loggedIn: Boolean,
    onLogin: () -> Unit,
    onLogout: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { if (loggedIn) onLogout() else onLogin() }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (loggedIn) colors.primary else colors.outlineVariant)
        )
        Spacer(Modifier.width(11.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = service.label,
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurface
            )
            Text(
                text = stringResource(
                    if (loggedIn) R.string.pm_account_logged_in
                    else R.string.pm_account_not_logged_in
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = if (loggedIn) colors.primary else colors.onSurfaceVariant
            )
        }
        Text(
            text = stringResource(
                if (loggedIn) R.string.pm_action_logout else R.string.pm_action_login
            ),
            style = MaterialTheme.typography.labelLarge,
            color = if (loggedIn) colors.error else colors.primary
        )
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    danger: Boolean = false,
    trailing: (@Composable () -> Unit)? = null
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (danger) colors.error else colors.onSurfaceVariant,
            modifier = Modifier.size(21.dp)
        )
        Spacer(Modifier.width(13.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = if (danger) colors.error else colors.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant
            )
        }
        if (trailing != null) {
            trailing()
        } else {
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = colors.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}
