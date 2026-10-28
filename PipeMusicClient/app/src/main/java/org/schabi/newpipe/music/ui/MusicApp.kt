package org.schabi.newpipe.music.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import org.schabi.newpipe.R
import org.schabi.newpipe.music.MusicGraph
import org.schabi.newpipe.music.data.MusicAccountService
import org.schabi.newpipe.music.data.db.DOWNLOAD_COMPLETED
import org.schabi.newpipe.music.data.db.DOWNLOAD_NONE
import org.schabi.newpipe.music.data.db.DOWNLOAD_QUEUED
import org.schabi.newpipe.music.data.db.DOWNLOADING
import org.schabi.newpipe.music.data.db.TrackEntity
import org.schabi.newpipe.music.playback.MusicPlayerController
import org.schabi.newpipe.music.playback.PlaybackQueue
import org.schabi.newpipe.music.ui.components.AddSongsSheet
import org.schabi.newpipe.music.ui.components.AddToPlaylistSheet
import org.schabi.newpipe.music.ui.components.AuroraBackground
import org.schabi.newpipe.music.ui.components.DownloadsSheet
import org.schabi.newpipe.music.ui.components.FullPlayer
import org.schabi.newpipe.music.ui.components.MiniPlayer
import org.schabi.newpipe.music.ui.components.MusicBottomBar
import org.schabi.newpipe.music.ui.components.QueueSheet
import org.schabi.newpipe.music.ui.components.SleepTimerSheet
import org.schabi.newpipe.music.ui.components.UpdateDialog
import org.schabi.newpipe.music.ui.components.TrackActionsSheet
import org.schabi.newpipe.music.ui.screens.HomeScreen
import org.schabi.newpipe.music.ui.screens.LibraryScreen
import org.schabi.newpipe.music.ui.screens.PlaylistDetailScreen
import org.schabi.newpipe.music.ui.screens.PlaylistsScreen
import org.schabi.newpipe.music.ui.screens.ProfileScreen
import org.schabi.newpipe.music.ui.screens.SearchScreen
import org.schabi.newpipe.music.ui.theme.ColorSchemeFactory
import org.schabi.newpipe.music.ui.theme.ThemeSettingsScreen
import org.schabi.newpipe.music.ui.viewmodel.DownloadFeedback
import org.schabi.newpipe.music.ui.viewmodel.MusicViewModel
import org.schabi.newpipe.music.ui.viewmodel.UpdateCheckResult

@Composable
fun MusicApp(
    openUrl: StateFlow<String?>,
    onConsumedUrl: () -> Unit,
    openQuery: StateFlow<String?>,
    onConsumedQuery: () -> Unit,
    onLanguageChange: (String) -> Unit
) {
    val context = LocalContext.current
    val viewModel: MusicViewModel = viewModel()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val themeConfig by MusicGraph.preferences.theme.collectAsStateWithLifecycle()
    val downloadQuality by MusicGraph.preferences.downloadQuality.collectAsStateWithLifecycle()
    val language by MusicGraph.preferences.language.collectAsStateWithLifecycle()
    val accountState by viewModel.accountState.collectAsStateWithLifecycle()
    val player by MusicPlayerController.state.collectAsStateWithLifecycle()
    val autoUpdateCheck by MusicGraph.preferences.autoUpdateCheck.collectAsStateWithLifecycle()
    val checkingUpdate by viewModel.checkingUpdate.collectAsStateWithLifecycle()
    val availableUpdate by viewModel.availableUpdate.collectAsStateWithLifecycle()

    val tracks by viewModel.tracks.collectAsStateWithLifecycle()
    val downloadedTracks by viewModel.downloadedTracks.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsStateWithLifecycle()
    val recentlyAdded by viewModel.recentlyAdded.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val recentSearches by viewModel.recentSearches.collectAsStateWithLifecycle()
    val importState by viewModel.importState.collectAsStateWithLifecycle()
    val searchState by viewModel.search.collectAsStateWithLifecycle()

    var tab by rememberSaveable { mutableIntStateOf(0) }
    var detailPlaylistId by rememberSaveable { mutableStateOf<Long?>(null) }
    var showPlayer by rememberSaveable { mutableStateOf(false) }
    var showThemeSheet by remember { mutableStateOf(false) }
    var showQueueSheet by remember { mutableStateOf(false) }
    var showSleepSheet by remember { mutableStateOf(false) }
    var showDownloadsSheet by remember { mutableStateOf(false) }
    var pendingLoginService by remember { mutableStateOf<MusicAccountService?>(null) }
    var addToPlaylistIds by remember { mutableStateOf<List<Long>?>(null) }
    var addSongsPlaylistId by remember { mutableStateOf<Long?>(null) }
    var actionTrack by remember { mutableStateOf<TrackEntity?>(null) }
    var actionPlaylistId by remember { mutableStateOf<Long?>(null) }

    val activeDownloads = tracks.count {
        it.downloadState == DOWNLOADING || it.downloadState == DOWNLOAD_QUEUED
    }
    val completedDownloads = tracks.count { it.downloadState == DOWNLOAD_COMPLETED }

    val currentTrackFlow = remember(player.current?.id) {
        player.current?.id?.let { viewModel.track(it) } ?: flowOf(null)
    }
    val currentTrack by currentTrackFlow.collectAsStateWithLifecycle(null)

    // Live download state of the song being played, so the fullscreen player can show feedback
    // while the download runs (queued / progress / failed).
    val currentDownloadState = currentTrack?.downloadState ?: DOWNLOAD_NONE
    val currentDownloadProgress = currentTrack?.downloadProgress ?: 0

    val detailId = detailPlaylistId
    val playlistFlow = remember(detailId) {
        detailId?.let { viewModel.playlist(it) } ?: flowOf(null)
    }
    val playlist by playlistFlow.collectAsStateWithLifecycle(null)
    val detailTracksFlow = remember(detailId) {
        detailId?.let { viewModel.playlistTracks(it) } ?: flowOf(emptyList<TrackEntity>())
    }
    val detailTracks by detailTracksFlow.collectAsStateWithLifecycle(emptyList())

    val folderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            viewModel.importFolder(context, uri.toString())
        }
    }

    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            viewModel.importFile(context, uri.toString())
        }
    }

    val loginLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val service = pendingLoginService
        pendingLoginService = null
        if (service == null) {
            return@rememberLauncherForActivityResult
        }
        if (result.resultCode != Activity.RESULT_OK) {
            scope.launch {
                snackbarHostState.showSnackbar(context.getString(R.string.pm_login_failed))
            }
            return@rememberLauncherForActivityResult
        }
        val data = result.data
        val saved = viewModel.saveLogin(
            service,
            data?.getStringExtra("cookies"),
            data?.getStringExtra("pot")
        )
        scope.launch {
            snackbarHostState.showSnackbar(
                context.getString(
                    if (saved) R.string.pm_login_success else R.string.pm_login_failed
                )
            )
        }
    }

    fun launchLogin(service: MusicAccountService) {
        pendingLoginService = service
        loginLauncher.launch(Intent(context, service.loginActivity))
    }

    fun showDownloadFeedback(feedback: DownloadFeedback) {
        val message = when (feedback) {
            DownloadFeedback.STARTED -> R.string.pm_feedback_download_queued
            DownloadFeedback.ALREADY_DOWNLOADED -> R.string.pm_feedback_download_already
            DownloadFeedback.IN_PROGRESS -> R.string.pm_feedback_download_in_progress
            DownloadFeedback.NO_SOURCE -> R.string.pm_feedback_download_no_source
        }
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = context.getString(message),
                actionLabel = if (feedback == DownloadFeedback.STARTED) {
                    context.getString(R.string.pm_action_view_downloads)
                } else {
                    null
                },
                withDismissAction = true,
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                showDownloadsSheet = true
            }
        }
    }

    val pendingUrl by openUrl.collectAsStateWithLifecycle()
    LaunchedEffect(pendingUrl) {
        val url = pendingUrl
        if (!url.isNullOrBlank()) {
            tab = 1
            detailPlaylistId = null
            viewModel.openLink(context, url)
            onConsumedUrl()
        }
    }

    val pendingQuery by openQuery.collectAsStateWithLifecycle()
    LaunchedEffect(pendingQuery) {
        val query = pendingQuery
        if (!query.isNullOrBlank()) {
            tab = 1
            detailPlaylistId = null
            viewModel.searchFor(query)
            onConsumedQuery()
        }
    }

    LaunchedEffect(player.error) {
        // Error remains visible in the player until the user interacts again.
    }

    BackHandler(enabled = showPlayer) { showPlayer = false }
    BackHandler(enabled = !showPlayer && detailPlaylistId != null) {
        detailPlaylistId = null
    }
    BackHandler(enabled = !showPlayer && detailPlaylistId == null && tab != 0) {
        tab = 0
    }
    BackHandler(enabled = showThemeSheet) {
        showThemeSheet = false
    }
    BackHandler(enabled = showDownloadsSheet) {
        showDownloadsSheet = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AuroraBackground {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    when {
                        detailId != null -> {
                            PlaylistDetailScreen(
                                playlist = playlist,
                                tracks = detailTracks,
                                player = player,
                                onBack = { detailPlaylistId = null },
                                onPlayAll = {
                                    viewModel.playPlaylist(context, detailId)
                                },
                                onShuffle = {
                                    viewModel.playPlaylist(context, detailId, shuffle = true)
                                },
                                onPlayTrack = { index ->
                                    viewModel.playTracks(context, detailTracks, index)
                                },
                                onTrackMore = { track ->
                                    actionTrack = track
                                    actionPlaylistId = detailId
                                },
                                onAddSongs = { addSongsPlaylistId = detailId },
                                onRename = { name -> viewModel.renamePlaylist(detailId, name) },
                                onDelete = {
                                    viewModel.deletePlaylist(detailId)
                                    detailPlaylistId = null
                                }
                            )
                        }

                        tab == 0 -> {
                            HomeScreen(
                                tracks = tracks.filter {
                                    it.type == org.schabi.newpipe.music.data.db.TRACK_TYPE_LOCAL ||
                                        it.downloadState ==
                                        org.schabi.newpipe.music.data.db.DOWNLOAD_COMPLETED
                                },
                                recentlyPlayed = recentlyPlayed,
                                recentlyAdded = recentlyAdded,
                                player = player,
                                services = viewModel.services,
                                onPlayTrack = { track ->
                                    viewModel.playTracks(context, listOf(track))
                                },
                                onPlayList = { list, index ->
                                    viewModel.playTracks(context, list, index)
                                },
                                onOpenSearch = { serviceId ->
                                    viewModel.selectService(serviceId)
                                    tab = 1
                                },
                                onOpenTheme = { showThemeSheet = true },
                                onOpenLibrary = { tab = 2 },
                                onOpenPlayer = { if (player.hasTrack) showPlayer = true },
                                onToggle = { MusicPlayerController.toggle(context) },
                                onImport = { folderLauncher.launch(null) },
                                onImportFile = { fileLauncher.launch(arrayOf("audio/*")) }
                            )
                        }

                        tab == 1 -> {
                            SearchScreen(
                                state = searchState,
                                services = viewModel.services,
                                recentSearches = recentSearches,
                                onQueryChange = viewModel::onQueryChange,
                                onSubmit = viewModel::search,
                                onSearchFor = viewModel::searchFor,
                                onSelectService = viewModel::selectService,
                                onPlay = { song -> viewModel.playSearchSong(context, song) },
                                onDownload = { song ->
                                    viewModel.downloadSearchSong(
                                        context,
                                        song,
                                        ::showDownloadFeedback
                                    )
                                },
                                onAddToPlaylist = { song ->
                                    viewModel.ensureSearchTrack(song) { track ->
                                        addToPlaylistIds = listOf(track.id)
                                    }
                                },
                                onOpenLink = { url -> viewModel.openLink(context, url) },
                                onLoadMore = viewModel::loadMore,
                                onDeleteHistory = viewModel::deleteSearchHistory,
                                activeDownloads = activeDownloads,
                                onOpenDownloads = { showDownloadsSheet = true }
                            )
                        }

                        tab == 2 -> {
                            LibraryScreen(
                                tracks = tracks,
                                stats = stats,
                                importState = importState,
                                player = player,
                                onImport = { folderLauncher.launch(null) },
                                onImportFile = { fileLauncher.launch(arrayOf("audio/*")) },
                                onCancelImport = { viewModel.cancelImport(context) },
                                onClearImport = viewModel::clearImportStatus,
                                onPlayList = { list, index ->
                                    viewModel.playTracks(context, list, index)
                                },
                                onMore = { track ->
                                    actionTrack = track
                                    actionPlaylistId = null
                                },
                                onPlaySelected = { selected ->
                                    if (selected.isNotEmpty()) {
                                        viewModel.playTracks(context, selected, 0)
                                    }
                                },
                                onFavoriteSelected = { selected ->
                                    viewModel.toggleFavorites(selected)
                                    scope.launch {
                                        val adding = !selected.all { it.isFavorite }
                                        snackbarHostState.showSnackbar(
                                            context.getString(
                                                if (adding) {
                                                    R.string.pm_feedback_favorites_added
                                                } else {
                                                    R.string.pm_feedback_favorites_removed
                                                },
                                                selected.size
                                            )
                                        )
                                    }
                                },
                                onAddSelected = { ids -> addToPlaylistIds = ids },
                                onDeleteSelected = { selected ->
                                    viewModel.deleteTracks(selected)
                                },
                                activeDownloads = activeDownloads,
                                onOpenDownloads = { showDownloadsSheet = true }
                            )
                        }

                        tab == 3 -> {
                            PlaylistsScreen(
                                playlists = playlists,
                                favoritesCount = favorites.size,
                                onOpen = { id -> detailPlaylistId = id },
                                onCreate = { name -> viewModel.createPlaylist(name) },
                                onRename = { id, name -> viewModel.renamePlaylist(id, name) },
                                onDelete = { id -> viewModel.deletePlaylist(id) },
                                onPlayFavorites = { viewModel.playFavorites(context) },
                                onShuffleFavorites = {
                                    viewModel.playFavorites(context, shuffle = true)
                                }
                            )
                        }

                        else -> {
                            ProfileScreen(
                                stats = stats,
                                playlistCount = playlists.size,
                                downloadsCount = downloadedTracks.size,
                                themeConfig = themeConfig,
                                downloadQuality = downloadQuality,
                                language = language,
                                accountState = accountState,
                                activeDownloads = activeDownloads,
                                onOpenTheme = { showThemeSheet = true },
                                onQualityChange = {
                                    MusicGraph.preferences.setDownloadQuality(it)
                                },
                                onSleepTimer = { showSleepSheet = true },
                                autoUpdateCheck = autoUpdateCheck,
                                checkingUpdate = checkingUpdate,
                                onAutoUpdateChange = {
                                    MusicGraph.preferences.setAutoUpdateCheck(it)
                                },
                                onCheckUpdate = {
                                    viewModel.checkForUpdates(manual = true) { result ->
                                        val message = when (result) {
                                            UpdateCheckResult.AVAILABLE -> null
                                            UpdateCheckResult.UP_TO_DATE ->
                                                R.string.pm_update_up_to_date
                                            UpdateCheckResult.FAILED ->
                                                R.string.pm_update_check_failed
                                        }
                                        if (message != null) {
                                            scope.launch {
                                                snackbarHostState.showSnackbar(
                                                    context.getString(message)
                                                )
                                            }
                                        }
                                    }
                                },
                                onClearHistory = viewModel::clearPlayHistory,
                                onDeleteAll = {
                                    viewModel.deleteTracks(
                                        tracks.filter {
                                            it.type == org.schabi.newpipe.music.data.db.TRACK_TYPE_LOCAL ||
                                                it.downloadState ==
                                                org.schabi.newpipe.music.data.db.DOWNLOAD_COMPLETED
                                        }
                                    )
                                },
                                onOpenDownloads = { showDownloadsSheet = true },
                                onLanguageChange = onLanguageChange,
                                onLogin = { service -> launchLogin(service) },
                                onLogout = { service ->
                                    viewModel.logout(service)
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            context.getString(R.string.pm_logout_success)
                                        )
                                    }
                                },
                                onSaveManualCookie = { service, cookies, poToken ->
                                    val saved = viewModel.saveLogin(service, cookies, poToken)
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            context.getString(
                                                if (saved) R.string.pm_login_success
                                                else R.string.pm_login_failed
                                            )
                                        )
                                    }
                                    saved
                                }
                            )
                        }
                    }
                }

                if (!showPlayer) {
                    Column(
                        modifier = Modifier
                            .navigationBarsPadding()
                            .padding(horizontal = 14.dp)
                    ) {
                        if (player.hasTrack) {
                            MiniPlayer(
                                state = player,
                                onOpen = { showPlayer = true },
                                onToggle = { MusicPlayerController.toggle(context) },
                                onNext = { MusicPlayerController.next(context) }
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                        if (detailId == null) {
                            MusicBottomBar(
                                selected = tab,
                                onSelect = { selected ->
                                    tab = selected
                                    detailPlaylistId = null
                                }
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }

        if (showThemeSheet) {
            ThemeSettingsScreen(
                config = themeConfig,
                onDismiss = { showThemeSheet = false },
                onChange = { transform ->
                    MusicGraph.preferences.updateTheme(transform)
                }
            )
        }

        AnimatedVisibility(
            visible = showPlayer && player.hasTrack,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            FullPlayer(
                state = player,
                gradient = ColorSchemeFactory.gradientColors(themeConfig),
                isFavorite = currentTrack?.isFavorite == true,
                isDownloaded = currentTrack?.let {
                    it.type == org.schabi.newpipe.music.data.db.TRACK_TYPE_LOCAL ||
                        it.downloadState == org.schabi.newpipe.music.data.db.DOWNLOAD_COMPLETED
                } == true,
                downloadState = currentDownloadState,
                downloadProgress = currentDownloadProgress,
                artworkTint = themeConfig.artworkTint,
                onClose = { showPlayer = false },
                onToggle = { MusicPlayerController.toggle(context) },
                onNext = { MusicPlayerController.next(context) },
                onPrevious = { MusicPlayerController.previous(context) },
                onSeek = { position -> MusicPlayerController.seekTo(context, position) },
                onToggleFavorite = { currentTrack?.let { viewModel.toggleFavorite(it) } },
                onShuffle = { MusicPlayerController.toggleShuffle(context) },
                onRepeat = {
                    val next = when (player.repeatMode) {
                        PlaybackQueue.REPEAT_ALL -> PlaybackQueue.REPEAT_ONE
                        PlaybackQueue.REPEAT_ONE -> PlaybackQueue.REPEAT_OFF
                        else -> PlaybackQueue.REPEAT_ALL
                    }
                    MusicPlayerController.setRepeatMode(context, next)
                },
                onQueue = { showQueueSheet = true },
                onAddToPlaylist = {
                    player.current?.let { addToPlaylistIds = listOf(it.id) }
                },
                onDownload = {
                    currentTrack?.let {
                        viewModel.startDownload(context, it, ::showDownloadFeedback)
                    }
                },
                onSleep = { showSleepSheet = true }
            )
        }

        // Kept above the fullscreen player: the player fills the whole screen with an opaque
        // background, so a snackbar drawn before it would be invisible (e.g. download feedback
        // triggered from the fullscreen player itself).
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = if (showPlayer) 20.dp else 92.dp)
        )
    }

    addToPlaylistIds?.let { ids ->
        AddToPlaylistSheet(
            playlists = playlists,
            onDismiss = { addToPlaylistIds = null },
            onSelect = { playlistId ->
                viewModel.addToPlaylist(playlistId, ids)
                addToPlaylistIds = null
                scope.launch {
                    snackbarHostState.showSnackbar(
                        context.getString(R.string.pm_feedback_added_to_playlist, ids.size)
                    )
                }
            },
            onCreate = { name ->
                viewModel.createPlaylist(name) { playlistId ->
                    viewModel.addToPlaylist(playlistId, ids)
                }
                addToPlaylistIds = null
                scope.launch {
                    snackbarHostState.showSnackbar(
                        context.getString(R.string.pm_feedback_added_to_playlist, ids.size)
                    )
                }
            }
        )
    }

    addSongsPlaylistId?.let { playlistId ->
        AddSongsSheet(
            tracks = tracks.filter {
                it.type == org.schabi.newpipe.music.data.db.TRACK_TYPE_LOCAL ||
                    it.downloadState == org.schabi.newpipe.music.data.db.DOWNLOAD_COMPLETED
            },
            existingIds = detailTracks.map { it.id }.toSet(),
            onDismiss = { addSongsPlaylistId = null },
            onAdd = { newTracks ->
                viewModel.addToPlaylist(playlistId, newTracks.map { it.id })
                scope.launch {
                    snackbarHostState.showSnackbar(
                        context.getString(
                            R.string.pm_feedback_added_to_playlist,
                            newTracks.size
                        )
                    )
                }
            }
        )
    }

    actionTrack?.let { track ->
        TrackActionsSheet(
            track = track,
            playlistId = actionPlaylistId,
            onDismiss = { actionTrack = null },
            onPlay = { viewModel.playTracks(context, listOf(track)) },
            onAddToPlaylist = { addToPlaylistIds = listOf(track.id) },
            onToggleFavorite = { viewModel.toggleFavorite(track) },
            onDownload = {
                viewModel.startDownload(context, track, ::showDownloadFeedback)
            },
            onCancelDownload = { viewModel.cancelDownload(context, track) },
            onRemoveFromPlaylist = {
                actionPlaylistId?.let { viewModel.removeFromPlaylist(it, track.id) }
            },
            onMoveUp = {
                actionPlaylistId?.let { viewModel.movePlaylistTrack(it, track.id, -1) }
            },
            onMoveDown = {
                actionPlaylistId?.let { viewModel.movePlaylistTrack(it, track.id, 1) }
            },
            onDelete = { viewModel.deleteTracks(listOf(track)) }
        )
    }

    if (showDownloadsSheet) {
        DownloadsSheet(
            tracks = tracks,
            onDismiss = { showDownloadsSheet = false },
            onCancel = { track -> viewModel.cancelDownload(context, track) },
            onRetry = { track ->
                viewModel.startDownload(context, track, ::showDownloadFeedback)
            },
            onPlay = { track ->
                viewModel.playTracks(context, listOf(track))
                showDownloadsSheet = false
            }
        )
    }

    if (showQueueSheet) {
        QueueSheet(
            state = player,
            onDismiss = { showQueueSheet = false },
            onSelect = { index ->
                MusicPlayerController.jumpTo(context, index)
                showQueueSheet = false
            },
            onRemove = { index -> MusicPlayerController.removeFromQueue(context, index) }
        )
    }

    availableUpdate?.let { release ->
        UpdateDialog(
            release = release,
            onDownload = {
                val url = release.downloadUrl ?: release.pageUrl
                runCatching {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                }
                viewModel.dismissUpdate(skipVersion = false)
            },
            onSkip = { viewModel.dismissUpdate(skipVersion = true) },
            onLater = { viewModel.dismissUpdate(skipVersion = false) }
        )
    }

    if (showSleepSheet) {
        SleepTimerSheet(
            endsAt = player.sleepTimerEndsAt,
            onDismiss = { showSleepSheet = false },
            onSelect = { minutes ->
                MusicPlayerController.setSleepTimer(context, minutes)
            }
        )
    }
}
