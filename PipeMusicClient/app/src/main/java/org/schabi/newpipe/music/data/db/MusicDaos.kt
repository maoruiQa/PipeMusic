package org.schabi.newpipe.music.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {

    @Query("SELECT * FROM music_tracks ORDER BY title COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM music_tracks WHERE type = 0 ORDER BY title COLLATE NOCASE ASC")
    fun observeLocal(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM music_tracks WHERE downloadState = 3 ORDER BY addedAt DESC")
    fun observeDownloaded(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM music_tracks WHERE isFavorite = 1 ORDER BY title COLLATE NOCASE ASC")
    fun observeFavorites(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM music_tracks WHERE lastPlayedAt > 0 ORDER BY lastPlayedAt DESC LIMIT :limit")
    fun observeRecentlyPlayed(limit: Int): Flow<List<TrackEntity>>

    @Query(
        "SELECT * FROM music_tracks WHERE type = 0 OR downloadState = 3 " +
            "ORDER BY addedAt DESC LIMIT :limit"
    )
    fun observeRecentlyAdded(limit: Int): Flow<List<TrackEntity>>

    @Query("SELECT * FROM music_tracks WHERE id = :id")
    fun observeById(id: Long): Flow<TrackEntity?>

    @Query("SELECT * FROM music_tracks WHERE id = :id")
    suspend fun getById(id: Long): TrackEntity?

    @Query("SELECT * FROM music_tracks WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Long>): List<TrackEntity>

    @Query("SELECT * FROM music_tracks WHERE identity = :identity LIMIT 1")
    suspend fun findByIdentity(identity: String): TrackEntity?

    @Query("SELECT * FROM music_tracks WHERE serviceId = :serviceId AND remoteUrl = :url LIMIT 1")
    suspend fun findRemote(serviceId: Int, url: String): TrackEntity?

    @Query("SELECT * FROM music_tracks WHERE sourceUri = :sourceUri LIMIT 1")
    suspend fun findBySourceUri(sourceUri: String): TrackEntity?

    @Query(
        "SELECT * FROM music_tracks WHERE title LIKE '%' || :query || '%' " +
            "OR artist LIKE '%' || :query || '%' OR album LIKE '%' || :query || '%' " +
            "ORDER BY title COLLATE NOCASE ASC"
    )
    fun search(query: String): Flow<List<TrackEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(track: TrackEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tracks: List<TrackEntity>)

    @Update
    suspend fun update(track: TrackEntity)

    @Query("UPDATE music_tracks SET isFavorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean)

    @Query("UPDATE music_tracks SET isFavorite = :favorite WHERE id IN (:ids)")
    suspend fun setFavorites(ids: List<Long>, favorite: Boolean)

    @Query(
        "UPDATE music_tracks SET lastPlayedAt = :time, playCount = playCount + 1 " +
            "WHERE id = :id"
    )
    suspend fun markPlayed(id: Long, time: Long)

    @Query(
        "UPDATE music_tracks SET downloadState = :state, downloadProgress = :progress, " +
            "downloadError = :error WHERE id = :id"
    )
    suspend fun updateDownloadState(id: Long, state: Int, progress: Int, error: String?)

    @Query(
        "UPDATE music_tracks SET downloadState = 3, downloadProgress = 100, " +
            "downloadError = NULL, mediaUri = :path, fileSize = :size, mimeType = :mime, " +
            "artworkUri = COALESCE(:artwork, artworkUri) WHERE id = :id"
    )
    suspend fun completeDownload(
        id: Long,
        path: String,
        size: Long,
        mime: String?,
        artwork: String?
    )

    @Query("SELECT id FROM music_tracks")
    suspend fun allIds(): List<Long>

    @Query("UPDATE music_tracks SET lastPlayedAt = 0, playCount = 0")
    suspend fun resetPlayStats()

    @Query("DELETE FROM music_tracks WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query(
        "SELECT COUNT(*) AS trackCount, COALESCE(SUM(durationMs), 0) AS totalDurationMs, " +
            "COALESCE(SUM(fileSize), 0) AS totalBytes FROM music_tracks " +
            "WHERE type = 0 OR downloadState = 3"
    )
    fun observeStats(): Flow<LibraryStats>
}

@Dao
interface PlaylistDao {

    @Query(
        "SELECT p.id, p.name, p.createdAt, p.updatedAt, COUNT(pt.trackId) AS trackCount, " +
            "(SELECT t.artworkUri FROM music_playlist_tracks pt2 " +
            " INNER JOIN music_tracks t ON t.id = pt2.trackId " +
            " WHERE pt2.playlistId = p.id ORDER BY pt2.position LIMIT 1) AS coverUrl " +
            "FROM music_playlists p " +
            "LEFT JOIN music_playlist_tracks pt ON pt.playlistId = p.id " +
            "GROUP BY p.id ORDER BY p.updatedAt DESC"
    )
    fun observeAllWithCount(): Flow<List<PlaylistWithCount>>

    @Query("SELECT * FROM music_playlists WHERE id = :id")
    fun observeById(id: Long): Flow<PlaylistEntity?>

    @Query("SELECT * FROM music_playlists WHERE id = :id")
    suspend fun getById(id: Long): PlaylistEntity?

    @Insert
    suspend fun insert(playlist: PlaylistEntity): Long

    @Query("UPDATE music_playlists SET name = :name, updatedAt = :time WHERE id = :id")
    suspend fun rename(id: Long, name: String, time: Long)

    @Query("UPDATE music_playlists SET updatedAt = :time WHERE id = :id")
    suspend fun touch(id: Long, time: Long)

    @Query("DELETE FROM music_playlists WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM music_playlist_tracks WHERE playlistId = :playlistId")
    suspend fun clearTracks(playlistId: Long)

    @Query(
        "SELECT t.* FROM music_tracks t INNER JOIN music_playlist_tracks pt " +
            "ON t.id = pt.trackId WHERE pt.playlistId = :playlistId ORDER BY pt.position ASC"
    )
    fun observeTracks(playlistId: Long): Flow<List<TrackEntity>>

    @Query(
        "SELECT t.* FROM music_tracks t INNER JOIN music_playlist_tracks pt " +
            "ON t.id = pt.trackId WHERE pt.playlistId = :playlistId ORDER BY pt.position ASC"
    )
    suspend fun getTracks(playlistId: Long): List<TrackEntity>

    @Query("SELECT COUNT(*) FROM music_playlist_tracks WHERE playlistId = :playlistId")
    suspend fun countTracks(playlistId: Long): Int

    @Query(
        "SELECT COALESCE(MAX(position), -1) FROM music_playlist_tracks " +
            "WHERE playlistId = :playlistId"
    )
    suspend fun maxPosition(playlistId: Long): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addTrack(entry: PlaylistTrackEntity): Long

    @Query("DELETE FROM music_playlist_tracks WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun removeTrack(playlistId: Long, trackId: Long)

    @Query("DELETE FROM music_playlist_tracks WHERE trackId = :trackId")
    suspend fun deleteEntriesForTrack(trackId: Long)

    @Query("UPDATE music_playlist_tracks SET position = :position WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun setPosition(playlistId: Long, trackId: Long, position: Int)

    @Query("SELECT trackId FROM music_playlist_tracks WHERE playlistId = :playlistId ORDER BY position ASC")
    suspend fun trackIds(playlistId: Long): List<Long>
}

@Dao
interface HistoryDao {

    @Insert
    suspend fun insert(history: PlayHistoryEntity)

    @Query("SELECT * FROM music_play_history ORDER BY playedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<PlayHistoryEntity>>

    @Query("DELETE FROM music_play_history")
    suspend fun clear()
}

@Dao
interface SearchHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: SearchHistoryEntity)

    @Query("SELECT * FROM music_search_history ORDER BY createdAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<SearchHistoryEntity>>

    @Query("DELETE FROM music_search_history WHERE query = :query AND serviceId = :serviceId")
    suspend fun delete(query: String, serviceId: Int)

    @Query("DELETE FROM music_search_history")
    suspend fun clear()
}
