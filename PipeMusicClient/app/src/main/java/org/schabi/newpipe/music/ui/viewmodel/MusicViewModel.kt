package org.schabi.newpipe.music.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.schabi.newpipe.R
import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.music.MusicGraph
import org.schabi.newpipe.music.data.MusicAccountService
import org.schabi.newpipe.music.data.db.DOWNLOAD_QUEUED
import org.schabi.newpipe.music.data.db.DOWNLOADING
import org.schabi.newpipe.music.data.db.PlaylistEntity
import org.schabi.newpipe.music.data.db.PlaylistWithCount
import org.schabi.newpipe.music.data.db.SearchHistoryEntity
import org.schabi.newpipe.music.data.db.TrackEntity
import org.schabi.newpipe.music.local.ImportStatus
import org.schabi.newpipe.music.local.LocalImportWorker
import org.schabi.newpipe.music.playback.MusicPlayerController
import org.schabi.newpipe.music.stream.AudioDownloadWorker
import org.schabi.newpipe.music.stream.MusicService
import org.schabi.newpipe.util.ExtractorHelper

data class SearchSong(
    val serviceId: Int,
    val url: String,
    val title: String,
    val artist: String,
    val durationMs: Long,
    val thumbnailUrl: String?
)

data class SearchUiState(
    val query: String = "",
    val serviceId: Int = 0,
    val songs: List<SearchSong> = emptyList(),
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    val error: String? = null,
    val nextPage: Page? = null,
    val showLinkHint: Boolean = false,
    /** True once a search was submitted for the current query (enter/search pressed). */
    val searchSubmitted: Boolean = false
)

enum class DownloadFeedback {
    STARTED,
    ALREADY_DOWNLOADED,
    IN_PROGRESS,
    NO_SOURCE
}

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val appContext: Context =
        org.schabi.newpipe.music.data.MusicLocale.localized(application)
    private val repository = MusicGraph.repository
    private var searchJob: Job? = null
    private var linkRequestId = 0

    val tracks: StateFlow<List<TrackEntity>> = repository.allTracks
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val localTracks: StateFlow<List<TrackEntity>> = repository.localTracks
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val downloadedTracks: StateFlow<List<TrackEntity>> = repository.downloadedTracks
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val favorites: StateFlow<List<TrackEntity>> = repository.favoriteTracks
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val recentlyPlayed: StateFlow<List<TrackEntity>> = repository.recentlyPlayed
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val recentlyAdded: StateFlow<List<TrackEntity>> = repository.recentlyAdded
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val playlists: StateFlow<List<PlaylistWithCount>> = repository.playlists
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val stats = repository.stats.stateIn(
        scope,
        SharingStarted.WhileSubscribed(5_000),
        org.schabi.newpipe.music.data.db.LibraryStats(0, 0, 0)
    )
    val recentSearches: StateFlow<List<SearchHistoryEntity>> = repository.recentSearches
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val importState = ImportStatus.state
    val accountState = MusicGraph.accounts.state

    val services: List<MusicService> = MusicGraph.search.services

    private val _search = MutableStateFlow(
        SearchUiState(
            serviceId = MusicGraph.preferences.preferredService.value
        )
    )
    val search: StateFlow<SearchUiState> = _search.asStateFlow()

    fun onQueryChange(query: String) {
        // Typing alone must not be treated as a submitted search, otherwise the result list
        // would flash "no results" before the user presses enter.
        _search.update {
            it.copy(query = query, searchSubmitted = false, error = null)
        }
    }

    fun selectService(serviceId: Int) {
        if (serviceId == _search.value.serviceId) {
            return
        }
        MusicGraph.preferences.setPreferredService(serviceId)
        _search.update {
            it.copy(
                serviceId = serviceId,
                songs = emptyList(),
                nextPage = null,
                error = null,
                searchSubmitted = false
            )
        }
        if (_search.value.query.isNotBlank()) {
            search()
        }
    }

    fun search() {
        val state = _search.value
        if (state.query.isBlank()) {
            return
        }
        linkRequestId += 1
        searchJob?.cancel()
        _search.update {
            it.copy(
                loading = true,
                error = null,
                songs = emptyList(),
                nextPage = null,
                searchSubmitted = true
            )
        }
        val requestQuery = state.query
        val requestService = state.serviceId
        searchJob = scope.launch {
            try {
                val page = MusicGraph.search.search(requestService, requestQuery)
                _search.update {
                    if (it.query == requestQuery && it.serviceId == requestService) {
                        it.copy(
                            loading = false,
                            songs = page.items.map { item ->
                                item.toSearchSong(requestService)
                            },
                            nextPage = page.nextPage
                        )
                    } else {
                        it
                    }
                }
                if (_search.value.query == requestQuery &&
                    _search.value.serviceId == requestService
                ) {
                    repository.recordSearch(requestQuery, requestService)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                android.util.Log.w("MusicSearch", "Search failed for $requestService", error)
                _search.update {
                    if (it.query == requestQuery && it.serviceId == requestService) {
                        it.copy(loading = false, error = friendlyMessage(error))
                    } else {
                        it
                    }
                }
            }
        }
    }

    fun searchFor(query: String) {
        _search.update { it.copy(query = query) }
        search()
    }

    fun ensureSearchTrack(song: SearchSong, onReady: (TrackEntity) -> Unit) {
        scope.launch {
            val track = repository.ensureStreamTrack(
                serviceId = song.serviceId,
                url = song.url,
                title = song.title,
                artist = song.artist,
                durationMs = song.durationMs,
                thumbnailUrl = song.thumbnailUrl
            )
            onReady(track)
        }
    }

    fun loadMore() {
        val state = _search.value
        val page = state.nextPage ?: return
        if (state.loading || state.loadingMore) {
            return
        }
        _search.update { it.copy(loadingMore = true, error = null) }
        val requestQuery = state.query
        val requestService = state.serviceId
        scope.launch {
            try {
                val result = MusicGraph.search.search(requestService, requestQuery, page)
                _search.update {
                    if (it.query == requestQuery && it.serviceId == requestService) {
                        it.copy(
                            loadingMore = false,
                            songs = it.songs + result.items.map { item ->
                                item.toSearchSong(requestService)
                            },
                            nextPage = result.nextPage
                        )
                    } else {
                        it
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _search.update {
                    if (it.query == requestQuery && it.serviceId == requestService) {
                        it.copy(loadingMore = false, error = friendlyMessage(error))
                    } else {
                        it
                    }
                }
            }
        }
    }

    fun openLink(context: Context, url: String) {
        if (url.isBlank()) {
            return
        }
        searchJob?.cancel()
        val requestId = ++linkRequestId
        _search.update { it.copy(loading = true, error = null, showLinkHint = false, searchSubmitted = false) }
        searchJob = scope.launch {
            try {
                val serviceId = MusicGraph.search.serviceIdForUrl(url)
                if (requestId != linkRequestId) {
                    return@launch
                }
                if (serviceId == null) {
                    _search.update {
                        it.copy(
                            loading = false,
                            error = appContext.getString(R.string.pm_error_unrecognized_link)
                        )
                    }
                    return@launch
                }
                val info = withContext(Dispatchers.IO) {
                    ExtractorHelper.getNewStreamInfo(serviceId, url)
                }
                if (requestId != linkRequestId) {
                    return@launch
                }
                val track = repository.ensureStreamTrack(
                    serviceId = serviceId,
                    url = url,
                    title = info.name.orEmpty()
                        .ifBlank { appContext.getString(R.string.pm_untitled_song) },
                    artist = info.uploaderName.orEmpty(),
                    durationMs = if (info.duration > 0) info.duration * 1000 else 0,
                    thumbnailUrl = info.thumbnailUrl
                )
                _search.update {
                    it.copy(
                        loading = false,
                        serviceId = serviceId,
                        query = info.name.orEmpty(),
                        songs = emptyList(),
                        nextPage = null
                    )
                }
                MusicPlayerController.playSingle(context, track)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (requestId == linkRequestId) {
                    _search.update {
                        it.copy(loading = false, error = friendlyMessage(error))
                    }
                }
            }
        }
    }

    fun playSearchSong(context: Context, song: SearchSong) {
        scope.launch {
            val track = repository.ensureStreamTrack(
                serviceId = song.serviceId,
                url = song.url,
                title = song.title,
                artist = song.artist,
                durationMs = song.durationMs,
                thumbnailUrl = song.thumbnailUrl
            )
            MusicPlayerController.playSingle(context, track)
        }
    }

    fun downloadSearchSong(
        context: Context,
        song: SearchSong,
        onFeedback: ((DownloadFeedback) -> Unit)? = null
    ) {
        scope.launch {
            val track = repository.ensureStreamTrack(
                serviceId = song.serviceId,
                url = song.url,
                title = song.title,
                artist = song.artist,
                durationMs = song.durationMs,
                thumbnailUrl = song.thumbnailUrl
            )
            startDownload(context, track, onFeedback)
        }
    }

    fun playTracks(context: Context, tracks: List<TrackEntity>, startIndex: Int = 0) {
        MusicPlayerController.playQueue(context, tracks, startIndex)
    }

    fun playPlaylist(context: Context, playlistId: Long, shuffle: Boolean = false) {
        scope.launch {
            val songs = repository.playlistSongs(playlistId)
            if (songs.isEmpty()) {
                return@launch
            }
            MusicPlayerController.playQueue(context, songs, 0)
            if (shuffle) {
                MusicPlayerController.toggleShuffle(context)
            }
        }
    }

    fun playFavorites(context: Context, shuffle: Boolean = false) {
        val songs = favorites.value
        if (songs.isEmpty()) {
            return
        }
        MusicPlayerController.playQueue(context, songs, 0)
        if (shuffle) {
            MusicPlayerController.toggleShuffle(context)
        }
    }

    fun toggleFavorite(track: TrackEntity) {
        scope.launch {
            repository.setFavorite(track.id, !track.isFavorite)
        }
    }

    /**
     * Batch favorite/unfavorite. When every track is already a favorite the selection is
     * unfavorited instead, so the action works as a toggle for the whole selection.
     */
    fun toggleFavorites(tracks: List<TrackEntity>) {
        if (tracks.isEmpty()) {
            return
        }
        val favorite = !tracks.all { it.isFavorite }
        scope.launch {
            repository.setFavorites(tracks.map { it.id }, favorite)
        }
    }

    fun startDownload(
        context: Context,
        track: TrackEntity,
        onFeedback: ((DownloadFeedback) -> Unit)? = null
    ) {
        if (track.remoteUrl == null) {
            onFeedback?.invoke(DownloadFeedback.NO_SOURCE)
            return
        }
        if (repository.localFile(track) != null) {
            onFeedback?.invoke(DownloadFeedback.ALREADY_DOWNLOADED)
            return
        }
        if (track.downloadState == DOWNLOADING || track.downloadState == DOWNLOAD_QUEUED) {
            onFeedback?.invoke(DownloadFeedback.IN_PROGRESS)
            return
        }
        scope.launch {
            repository.setDownloadState(track.id, DOWNLOAD_QUEUED, 0, null)
            AudioDownloadWorker.enqueue(context, track.id)
            onFeedback?.invoke(DownloadFeedback.STARTED)
        }
    }

    fun cancelDownload(context: Context, track: TrackEntity) {
        scope.launch {
            AudioDownloadWorker.cancel(context, track.id)
            repository.setDownloadState(track.id, 0, 0, null)
        }
    }

    fun saveLogin(
        service: MusicAccountService,
        cookies: String?,
        poToken: String? = null
    ): Boolean = MusicGraph.accounts.saveLogin(service, cookies, poToken)

    fun logout(service: MusicAccountService) {
        MusicGraph.accounts.logout(service)
    }

    fun deleteTracks(tracks: List<TrackEntity>) {
        scope.launch {
            repository.deleteTracks(tracks)
        }
    }

    fun createPlaylist(name: String, onCreated: ((Long) -> Unit)? = null) {
        scope.launch {
            val id = repository.createPlaylist(name)
            onCreated?.invoke(id)
        }
    }

    fun renamePlaylist(id: Long, name: String) {
        scope.launch { repository.renamePlaylist(id, name) }
    }

    fun deletePlaylist(id: Long) {
        scope.launch { repository.deletePlaylist(id) }
    }

    fun addToPlaylist(playlistId: Long, trackIds: List<Long>) {
        if (trackIds.isEmpty()) {
            return
        }
        scope.launch { repository.addAllToPlaylist(playlistId, trackIds) }
    }

    fun removeFromPlaylist(playlistId: Long, trackId: Long) {
        scope.launch { repository.removeFromPlaylist(playlistId, trackId) }
    }

    fun movePlaylistTrack(playlistId: Long, trackId: Long, offset: Int) {
        scope.launch { repository.movePlaylistTrack(playlistId, trackId, offset) }
    }

    fun importFolder(context: Context, treeUri: String) {
        ImportStatus.begin()
        val request = OneTimeWorkRequestBuilder<LocalImportWorker>()
            .setInputData(workDataOf(LocalImportWorker.KEY_TREE_URI to treeUri))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            LocalImportWorker.UNIQUE_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun importFile(context: Context, documentUri: String) {
        ImportStatus.begin()
        val request = OneTimeWorkRequestBuilder<LocalImportWorker>()
            .setInputData(workDataOf(LocalImportWorker.KEY_DOCUMENT_URI to documentUri))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            LocalImportWorker.UNIQUE_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancelImport(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(LocalImportWorker.UNIQUE_NAME)
        ImportStatus.finish(appContext.getString(R.string.pm_import_cancelled))
    }

    fun clearImportStatus() {
        ImportStatus.clear()
    }

    fun serviceLabel(serviceId: Int): String =
        services.firstOrNull { it.id == serviceId }?.shortName
            ?: MusicGraph.search.serviceName(serviceId)

    fun playlist(id: Long): Flow<PlaylistEntity?> = repository.playlist(id)

    fun playlistTracks(id: Long): Flow<List<TrackEntity>> = repository.playlistTracks(id)

    fun track(id: Long): Flow<TrackEntity?> = repository.track(id)

    fun deleteSearchHistory(query: String, serviceId: Int) {
        scope.launch { repository.deleteSearchHistory(query, serviceId) }
    }

    fun clearSearchHistory() {
        scope.launch { repository.clearSearchHistory() }
    }

    fun clearPlayHistory() {
        scope.launch { repository.clearPlayHistory() }
    }

    private fun StreamInfoItem.toSearchSong(serviceId: Int): SearchSong = SearchSong(
        serviceId = serviceId,
        url = url,
        title = name.orEmpty().ifBlank { appContext.getString(R.string.pm_untitled_song) },
        artist = uploaderName.orEmpty(),
        durationMs = if (duration > 0) duration * 1000 else 0,
        thumbnailUrl = thumbnailUrl
    )

    private fun friendlyMessage(error: Throwable): String {
        val text = error.message.orEmpty()
        return when {
            error is org.schabi.newpipe.extractor.exceptions.AntiBotException ||
                text.contains("not a bot", true) ->
                appContext.getString(R.string.pm_error_antibot)
            error is org.schabi.newpipe.extractor.exceptions.ContentNotAvailableException ->
                appContext.getString(R.string.pm_error_unavailable)
            text.contains("Bilibili", true) ->
                appContext.getString(R.string.pm_error_bilibili)
            text.contains("Unable to resolve host", true) ||
                text.contains("timeout", true) ||
                text.contains("network", true) ||
                error is java.net.UnknownHostException ->
                appContext.getString(R.string.pm_error_network)
            text.contains("ReCaptcha", true) ->
                appContext.getString(R.string.pm_error_recaptcha)
            text.isNotBlank() -> text
            else -> appContext.getString(R.string.pm_error_search_failed)
        }
    }

    override fun onCleared() {
        searchJob?.cancel()
        scope.cancel()
        super.onCleared()
    }
}
