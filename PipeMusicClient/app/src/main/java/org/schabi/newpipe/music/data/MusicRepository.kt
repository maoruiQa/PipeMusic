package org.schabi.newpipe.music.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.Flow
import org.schabi.newpipe.music.data.db.DOWNLOAD_COMPLETED
import org.schabi.newpipe.music.data.db.DOWNLOAD_NONE
import org.schabi.newpipe.music.data.db.MusicDatabase
import org.schabi.newpipe.music.data.db.PlayHistoryEntity
import org.schabi.newpipe.music.data.db.PlaylistEntity
import org.schabi.newpipe.music.data.db.PlaylistTrackEntity
import org.schabi.newpipe.music.data.db.PlaylistWithCount
import org.schabi.newpipe.music.data.db.SearchHistoryEntity
import org.schabi.newpipe.music.data.db.TRACK_TYPE_STREAM
import org.schabi.newpipe.music.data.db.TrackEntity
import java.io.File

class MusicRepository(context: Context, private val db: MusicDatabase) {

    private val appContext = context.applicationContext
    private val trackDao = db.trackDao()
    private val playlistDao = db.playlistDao()
    private val historyDao = db.historyDao()
    private val searchHistoryDao = db.searchHistoryDao()

    val musicDir: File = File(appContext.filesDir, "music").apply { mkdirs() }
    val artworkDir: File = File(appContext.filesDir, "artwork").apply { mkdirs() }

    val allTracks: Flow<List<TrackEntity>> = trackDao.observeAll()
    val localTracks: Flow<List<TrackEntity>> = trackDao.observeLocal()
    val downloadedTracks: Flow<List<TrackEntity>> = trackDao.observeDownloaded()
    val favoriteTracks: Flow<List<TrackEntity>> = trackDao.observeFavorites()
    val recentlyPlayed: Flow<List<TrackEntity>> = trackDao.observeRecentlyPlayed(12)
    val recentlyAdded: Flow<List<TrackEntity>> = trackDao.observeRecentlyAdded(10)
    val stats = trackDao.observeStats()
    val playlists: Flow<List<PlaylistWithCount>> = playlistDao.observeAllWithCount()
    val recentSearches: Flow<List<SearchHistoryEntity>> = searchHistoryDao.observeRecent(12)

    fun searchTracks(query: String): Flow<List<TrackEntity>> = trackDao.search(query)

    fun track(id: Long): Flow<TrackEntity?> = trackDao.observeById(id)

    fun playlist(id: Long): Flow<PlaylistEntity?> = playlistDao.observeById(id)

    fun playlistTracks(playlistId: Long): Flow<List<TrackEntity>> =
        playlistDao.observeTracks(playlistId)

    suspend fun getTrack(id: Long): TrackEntity? = trackDao.getById(id)

    suspend fun getTracks(ids: List<Long>): List<TrackEntity> {
        if (ids.isEmpty()) {
            return emptyList()
        }
        val found = trackDao.getByIds(ids).associateBy { it.id }
        return ids.mapNotNull { found[it] }
    }

    fun streamIdentity(serviceId: Int, url: String): String = "stream|$serviceId|$url"

    /**
     * Returns the database row for a remote song, inserting it when it is seen for the first time.
     */
    suspend fun ensureStreamTrack(
        serviceId: Int,
        url: String,
        title: String,
        artist: String,
        durationMs: Long,
        thumbnailUrl: String?
    ): TrackEntity {
        val identity = streamIdentity(serviceId, url)
        trackDao.findByIdentity(identity)?.let { existing ->
            if (existing.title != title || existing.artworkUri == null && thumbnailUrl != null) {
                val updated = existing.copy(
                    title = title,
                    artist = artist.ifBlank { existing.artist },
                    durationMs = if (durationMs > 0) durationMs else existing.durationMs,
                    thumbnailUrl = thumbnailUrl ?: existing.thumbnailUrl,
                    artworkUri = existing.artworkUri ?: thumbnailUrl
                )
                trackDao.update(updated)
                return updated
            }
            return existing
        }
        val inserted = TrackEntity(
            identity = identity,
            type = TRACK_TYPE_STREAM,
            title = title,
            artist = artist,
            album = "",
            durationMs = durationMs,
            artworkUri = thumbnailUrl,
            remoteUrl = url,
            serviceId = serviceId,
            thumbnailUrl = thumbnailUrl,
            addedAt = System.currentTimeMillis()
        )
        val id = trackDao.insertIgnore(inserted)
        return if (id > 0) {
            inserted.copy(id = id)
        } else {
            trackDao.findByIdentity(identity) ?: inserted
        }
    }

    /** The existing row of a remote song, without creating one. */
    suspend fun findStreamTrack(serviceId: Int, url: String): TrackEntity? =
        trackDao.findByIdentity(streamIdentity(serviceId, url))

    suspend fun insertLocalTrack(track: TrackEntity): Long = trackDao.insertIgnore(track)

    /**
     * Saves a song merged from several parts. A song merged earlier from the same parts is
     * replaced in place, keeping its playlists, favourite flag and play statistics.
     */
    suspend fun saveMergedTrack(track: TrackEntity): TrackEntity {
        val existing = trackDao.findByIdentity(track.identity)
            ?: return track.copy(id = trackDao.insertIgnore(track))
        if (existing.mediaUri != track.mediaUri) {
            deleteOwnedFile(existing.mediaUri)
        }
        if (existing.artworkUri != track.artworkUri) {
            deleteOwnedFile(existing.artworkUri)
        }
        val updated = track.copy(
            id = existing.id,
            addedAt = existing.addedAt,
            lastPlayedAt = existing.lastPlayedAt,
            playCount = existing.playCount,
            isFavorite = existing.isFavorite
        )
        trackDao.update(updated)
        return updated
    }

    suspend fun updateTrack(track: TrackEntity) = trackDao.update(track)

    suspend fun findBySourceUri(uri: String): TrackEntity? = trackDao.findBySourceUri(uri)

    suspend fun setFavorite(id: Long, favorite: Boolean) = trackDao.setFavorite(id, favorite)

    suspend fun setFavorites(ids: List<Long>, favorite: Boolean) {
        if (ids.isEmpty()) {
            return
        }
        trackDao.setFavorites(ids, favorite)
    }

    suspend fun setDownloadState(id: Long, state: Int, progress: Int, error: String?) =
        trackDao.updateDownloadState(id, state, progress, error)

    suspend fun completeDownload(
        id: Long,
        path: String,
        size: Long,
        mime: String?,
        artwork: String?
    ) = trackDao.completeDownload(id, path, size, mime, artwork)

    suspend fun recordPlayed(track: TrackEntity) {
        val now = System.currentTimeMillis()
        trackDao.markPlayed(track.id, now)
        historyDao.insert(PlayHistoryEntity(trackId = track.id, playedAt = now))
    }

    suspend fun createPlaylist(name: String): Long {
        val clean = name.trim().ifBlank { defaultPlaylistName() }
        return playlistDao.insert(PlaylistEntity(name = clean))
    }

    suspend fun renamePlaylist(id: Long, name: String) {
        playlistDao.rename(id, name.trim().ifBlank { defaultPlaylistName() }, System.currentTimeMillis())
    }

    private fun defaultPlaylistName(): String =
        MusicLocale.localized(appContext)
            .getString(org.schabi.newpipe.R.string.pm_unnamed_playlist)

    suspend fun deletePlaylist(id: Long) {
        playlistDao.clearTracks(id)
        playlistDao.delete(id)
    }

    suspend fun addToPlaylist(playlistId: Long, trackId: Long) {
        val position = playlistDao.maxPosition(playlistId) + 1
        playlistDao.addTrack(
            PlaylistTrackEntity(playlistId = playlistId, trackId = trackId, position = position)
        )
        playlistDao.touch(playlistId, System.currentTimeMillis())
    }

    suspend fun addAllToPlaylist(playlistId: Long, trackIds: List<Long>) {
        var position = playlistDao.maxPosition(playlistId)
        trackIds.forEach { trackId ->
            position += 1
            playlistDao.addTrack(
                PlaylistTrackEntity(
                    playlistId = playlistId,
                    trackId = trackId,
                    position = position
                )
            )
        }
        playlistDao.touch(playlistId, System.currentTimeMillis())
    }

    suspend fun removeFromPlaylist(playlistId: Long, trackId: Long) {
        playlistDao.removeTrack(playlistId, trackId)
        playlistDao.touch(playlistId, System.currentTimeMillis())
    }

    suspend fun movePlaylistTrack(playlistId: Long, trackId: Long, offset: Int) {
        val ids = playlistDao.trackIds(playlistId).toMutableList()
        val from = ids.indexOf(trackId)
        if (from < 0) {
            return
        }
        val to = (from + offset).coerceIn(0, ids.lastIndex)
        if (to == from) {
            return
        }
        ids.removeAt(from)
        ids.add(to, trackId)
        ids.forEachIndexed { index, id -> playlistDao.setPosition(playlistId, id, index) }
        playlistDao.touch(playlistId, System.currentTimeMillis())
    }

    suspend fun playlistSongs(playlistId: Long): List<TrackEntity> =
        playlistDao.getTracks(playlistId)

    suspend fun deleteTracks(tracks: List<TrackEntity>) {
        tracks.forEach { track ->
            deleteOwnedFile(track.mediaUri)
            deleteOwnedFile(track.artworkUri)
            playlistDao.deleteEntriesForTrack(track.id)
        }
        trackDao.deleteByIds(tracks.map { it.id })
    }

    /**
     * Deletes the downloaded audio of remote songs but keeps their rows, so they stay in
     * playlists and can be downloaded again. Local songs are left alone.
     */
    suspend fun clearDownloads(ids: List<Long>) {
        getTracks(ids).filter { it.type == TRACK_TYPE_STREAM }.forEach { track ->
            deleteOwnedFile(track.mediaUri)
            if (track.artworkUri != track.thumbnailUrl) {
                deleteOwnedFile(track.artworkUri)
            }
            trackDao.update(
                track.copy(
                    mediaUri = null,
                    fileSize = 0,
                    mimeType = null,
                    artworkUri = track.thumbnailUrl,
                    downloadState = DOWNLOAD_NONE,
                    downloadProgress = 0,
                    downloadError = null
                )
            )
        }
    }

    suspend fun clearPlayHistory() {
        historyDao.clear()
        trackDao.resetPlayStats()
    }

    suspend fun deleteAllSongs() {
        deleteTracks(trackDao.getByIds(trackDao.allIds()))
    }

    suspend fun recordSearch(query: String, serviceId: Int) {
        val clean = query.trim()
        if (clean.isEmpty()) {
            return
        }
        searchHistoryDao.insert(
            SearchHistoryEntity(query = clean, serviceId = serviceId)
        )
    }

    suspend fun clearSearchHistory() = searchHistoryDao.clear()

    suspend fun deleteSearchHistory(query: String, serviceId: Int) =
        searchHistoryDao.delete(query, serviceId)

    fun localFile(track: TrackEntity): File? {
        val path = track.mediaUri ?: return null
        if (!path.startsWith("/")) {
            return null
        }
        val file = File(path)
        return if (file.exists()) file else null
    }

    fun isDownloaded(track: TrackEntity): Boolean =
        track.downloadState == DOWNLOAD_COMPLETED && localFile(track) != null

    /** Deletes a file only when it lives inside the app owned music/artwork directories. */
    private fun deleteOwnedFile(path: String?) {
        if (path.isNullOrBlank() || !path.startsWith("/")) {
            return
        }
        runCatching {
            val file = File(path).canonicalFile
            val allowed = listOf(musicDir.canonicalFile, artworkDir.canonicalFile)
            val inside = allowed.any { file.path.startsWith(it.path + File.separator) }
            if (inside && file.exists()) {
                file.delete()
            }
        }.onFailure { Log.w(TAG, "Could not delete ${fileSuffix(path)}", it) }
    }

    private fun fileSuffix(path: String): String = File(path).name.takeLast(8)

    companion object {
        private const val TAG = "MusicRepository"
    }
}
