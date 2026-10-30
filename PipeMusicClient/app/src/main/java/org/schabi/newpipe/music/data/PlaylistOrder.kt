package org.schabi.newpipe.music.data

import org.schabi.newpipe.music.data.db.TrackEntity

/** How the songs of a playlist are listed. [CUSTOM] keeps the order the user arranged. */
enum class PlaylistSort {
    CUSTOM,
    TITLE,
    ARTIST,
    ALBUM,
    DURATION,
    RECENTLY_PLAYED,
    MOST_PLAYED
}

object PlaylistOrder {

    /**
     * Filters [tracks] by [query] (title, artist or album) and sorts them. Each sort has a natural
     * direction (A to Z, longest, most recent, most played first); [reversed] flips it, which for
     * [PlaylistSort.CUSTOM] lists the most recently added songs first. Ties keep playlist order.
     */
    fun apply(
        tracks: List<TrackEntity>,
        query: String,
        sort: PlaylistSort,
        reversed: Boolean
    ): List<TrackEntity> {
        val needle = query.trim()
        val matching = if (needle.isEmpty()) {
            tracks
        } else {
            tracks.filter {
                it.title.contains(needle, true) ||
                    it.artist.contains(needle, true) ||
                    it.album.contains(needle, true)
            }
        }
        val sorted = when (sort) {
            PlaylistSort.CUSTOM -> matching
            PlaylistSort.TITLE -> matching.sortedBy { it.title.lowercase() }
            PlaylistSort.ARTIST -> matching.sortedWith(
                compareBy<TrackEntity> { it.artist.lowercase() }.thenBy { it.title.lowercase() }
            )
            PlaylistSort.ALBUM -> matching.sortedWith(
                compareBy<TrackEntity> { it.album.lowercase() }.thenBy { it.title.lowercase() }
            )
            PlaylistSort.DURATION -> matching.sortedByDescending { it.durationMs }
            PlaylistSort.RECENTLY_PLAYED -> matching.sortedByDescending { it.lastPlayedAt }
            PlaylistSort.MOST_PLAYED -> matching.sortedByDescending { it.playCount }
        }
        return if (reversed) sorted.asReversed() else sorted
    }
}
