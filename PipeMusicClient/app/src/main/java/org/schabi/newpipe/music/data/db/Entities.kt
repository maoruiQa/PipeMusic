package org.schabi.newpipe.music.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

const val TRACK_TYPE_LOCAL = 0
const val TRACK_TYPE_STREAM = 1

const val DOWNLOAD_NONE = 0
const val DOWNLOAD_QUEUED = 1
const val DOWNLOADING = 2
const val DOWNLOAD_COMPLETED = 3
const val DOWNLOAD_FAILED = 4

/**
 * One playable song. Local files and streamed songs share this table so that playlists,
 * favourites and the play queue can mix both kinds transparently.
 */
@Entity(
    tableName = "music_tracks",
    indices = [
        Index(value = ["identity"], unique = true),
        Index("lastPlayedAt"),
        Index("addedAt"),
        Index("isFavorite"),
        Index("downloadState")
    ]
)
data class TrackEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val identity: String,
    val type: Int,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    /** Local artwork file path, or an online artwork URL for streamed songs. */
    val artworkUri: String? = null,
    /** Absolute local file path when the song is available offline. */
    val mediaUri: String? = null,
    /** Canonical page URL of the remote song. */
    val remoteUrl: String? = null,
    val serviceId: Int = 0,
    val thumbnailUrl: String? = null,
    /** SAF document uri used for de-duplicating imports. */
    val sourceUri: String? = null,
    val mimeType: String? = null,
    val fileSize: Long = 0,
    val downloadState: Int = DOWNLOAD_NONE,
    val downloadProgress: Int = 0,
    val downloadError: String? = null,
    val addedAt: Long = System.currentTimeMillis(),
    val lastPlayedAt: Long = 0,
    val playCount: Int = 0,
    val isFavorite: Boolean = false
)

@Entity(tableName = "music_playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "music_playlist_tracks",
    primaryKeys = ["playlistId", "trackId"],
    indices = [Index("playlistId"), Index("trackId")]
)
data class PlaylistTrackEntity(
    val playlistId: Long,
    val trackId: Long,
    val position: Int,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "music_play_history", indices = [Index("playedAt"), Index("trackId")])
data class PlayHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trackId: Long,
    val playedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "music_search_history",
    indices = [Index(value = ["query", "serviceId"], unique = true)]
)
data class SearchHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val query: String,
    val serviceId: Int,
    val createdAt: Long = System.currentTimeMillis()
)

/** Projection used by the playlist grid, including its song count and a cover candidate. */
data class PlaylistWithCount(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val trackCount: Int,
    val coverUrl: String?
)

data class LibraryStats(
    val trackCount: Int,
    val totalDurationMs: Long,
    val totalBytes: Long
)
