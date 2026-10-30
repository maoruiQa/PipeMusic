package org.schabi.newpipe.music

import org.junit.Assert.assertEquals
import org.junit.Test
import org.schabi.newpipe.music.data.PlaylistOrder
import org.schabi.newpipe.music.data.PlaylistSort
import org.schabi.newpipe.music.data.db.TrackEntity

class PlaylistOrderTest {

    private fun track(
        id: Long,
        title: String,
        artist: String = "Artist",
        album: String = "",
        durationMs: Long = 1_000,
        playCount: Int = 0
    ) = TrackEntity(
        id = id,
        identity = "track-$id",
        type = 1,
        title = title,
        artist = artist,
        album = album,
        durationMs = durationMs,
        playCount = playCount,
        addedAt = 0
    )

    private val tracks = listOf(
        track(1, "banana", artist = "Zed", durationMs = 3_000, playCount = 1),
        track(2, "Apple", artist = "Amy", album = "Fruit", durationMs = 1_000, playCount = 5),
        track(3, "cherry", artist = "Amy", durationMs = 2_000, playCount = 3)
    )

    private fun ids(sort: PlaylistSort, query: String = "", reversed: Boolean = false) =
        PlaylistOrder.apply(tracks, query, sort, reversed).map { it.id }

    @Test
    fun `custom order keeps the playlist order and reverses to newest first`() {
        assertEquals(listOf(1L, 2L, 3L), ids(PlaylistSort.CUSTOM))
        assertEquals(listOf(3L, 2L, 1L), ids(PlaylistSort.CUSTOM, reversed = true))
    }

    @Test
    fun `sorts ignore case and break ties by title`() {
        assertEquals(listOf(2L, 1L, 3L), ids(PlaylistSort.TITLE))
        assertEquals(listOf(2L, 3L, 1L), ids(PlaylistSort.ARTIST))
        assertEquals(listOf(1L, 3L, 2L), ids(PlaylistSort.DURATION))
        assertEquals(listOf(2L, 3L, 1L), ids(PlaylistSort.MOST_PLAYED))
    }

    @Test
    fun `search matches title, artist and album`() {
        assertEquals(listOf(2L, 3L), ids(PlaylistSort.CUSTOM, query = " amy "))
        assertEquals(listOf(2L), ids(PlaylistSort.CUSTOM, query = "fruit"))
        assertEquals(listOf(3L), ids(PlaylistSort.TITLE, query = "CHER"))
        assertEquals(emptyList<Long>(), ids(PlaylistSort.TITLE, query = "kiwi"))
    }
}
