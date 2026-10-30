package org.schabi.newpipe.music

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.schabi.newpipe.music.data.db.TrackEntity
import org.schabi.newpipe.music.playback.PlaybackQueue

class PlaybackQueueTest {

    private fun track(id: Long) = TrackEntity(
        id = id,
        identity = "track-$id",
        type = 1,
        title = "Track $id",
        artist = "Artist",
        album = "Album",
        durationMs = 1_000,
        addedAt = 0
    )

    private fun queue(): PlaybackQueue =
        PlaybackQueue(listOf(track(1), track(2), track(3)), startIndex = 0)

    @Test
    fun `next follows insertion order and stops when repeat is off`() {
        val queue = queue()
        queue.setRepeatMode(PlaybackQueue.REPEAT_OFF)

        assertEquals(2L, queue.next()?.id)
        assertEquals(3L, queue.next()?.id)
        assertNull(queue.next())
    }

    @Test
    fun `repeat all wraps around at the end`() {
        val queue = queue()
        queue.setRepeatMode(PlaybackQueue.REPEAT_ALL)

        assertEquals(2L, queue.next()?.id)
        assertEquals(3L, queue.next()?.id)
        assertEquals(1L, queue.next()?.id)
    }

    @Test
    fun `repeat one replays current on automatic advance but manual next moves on`() {
        val queue = queue()
        queue.jumpTo(1)
        queue.setRepeatMode(PlaybackQueue.REPEAT_ONE)

        assertEquals(2L, queue.next(automatic = true)?.id)
        assertEquals(3L, queue.next(automatic = false)?.id)
    }

    @Test
    fun `previous wraps to the last song when repeating`() {
        val queue = queue()
        queue.setRepeatMode(PlaybackQueue.REPEAT_ALL)

        assertEquals(3L, queue.previous()?.id)
    }

    @Test
    fun `shuffle visits every song exactly once`() {
        val queue = queue()
        queue.jumpTo(1)
        queue.setShuffle(true)
        queue.setRepeatMode(PlaybackQueue.REPEAT_OFF)

        val seen = mutableSetOf(2L)
        repeat(2) {
            queue.next()?.let { seen.add(it.id) }
        }

        assertEquals(setOf(1L, 2L, 3L), seen)
    }

    @Test
    fun `shuffle starts from the current song`() {
        val queue = queue()
        queue.jumpTo(2)
        queue.setShuffle(true)

        assertEquals(3L, queue.current?.id)
    }

    @Test
    fun `removing the current song keeps a valid current item`() {
        val queue = queue()
        queue.jumpTo(1)

        val current = queue.removeAt(1)

        assertEquals(2, queue.size)
        assertTrue(current != null)
        assertEquals(current?.id, queue.current?.id)
    }

    @Test
    fun `removing the last remaining song empties the queue`() {
        val queue = PlaybackQueue(listOf(track(1)), 0)

        assertNull(queue.removeAt(0))
        assertTrue(queue.isEmpty)
        assertNull(queue.current)
    }

    @Test
    fun `move keeps the same current song selected`() {
        val queue = queue()
        queue.jumpTo(0)
        queue.move(0, 2)

        assertEquals(3, queue.size)
        assertEquals(1L, queue.current?.id)
        assertEquals(1L, queue.tracks.last().id)
    }

    @Test
    fun `expanding the current song inserts its parts in place`() {
        val queue = queue()
        queue.jumpTo(1)

        assertTrue(queue.expandCurrent(2, listOf(track(10), track(2), track(11))))

        assertEquals(listOf(1L, 10L, 2L, 11L, 3L), queue.tracks.map { it.id })
        assertEquals(2L, queue.current?.id)
        assertEquals(11L, queue.next()?.id)
    }

    @Test
    fun `expanding moves parts that were already queued instead of duplicating them`() {
        val queue = queue()

        assertTrue(queue.expandCurrent(1, listOf(track(1), track(3))))

        assertEquals(listOf(1L, 3L, 2L), queue.tracks.map { it.id })
        assertEquals(1L, queue.current?.id)
    }

    @Test
    fun `expanding is ignored once another song is playing`() {
        val queue = queue()
        queue.jumpTo(2)

        assertFalse(queue.expandCurrent(1, listOf(track(1), track(10))))
        assertFalse(queue.expandCurrent(3, listOf(track(3))))
        assertFalse(queue.expandCurrent(3, listOf(track(10), track(11))))
        assertEquals(listOf(1L, 2L, 3L), queue.tracks.map { it.id })
    }
}
