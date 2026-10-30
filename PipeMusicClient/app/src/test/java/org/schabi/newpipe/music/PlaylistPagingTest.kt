package org.schabi.newpipe.music

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.schabi.newpipe.music.stream.PlaylistPaging

class PlaylistPagingTest {

    @Test
    fun `pages are appended in order without repeating songs`() {
        val merged = PlaylistPaging.append(listOf("a", "b"), listOf("b", "c", "d")) { it }

        assertEquals(listOf("a", "b", "c", "d"), merged)
    }

    @Test
    fun `appending stops at the limit`() {
        val merged = PlaylistPaging.append(listOf("a"), listOf("b", "c", "d"), limit = 3) { it }

        assertEquals(listOf("a", "b", "c"), merged)
        assertEquals(merged, PlaylistPaging.append(merged, listOf("e"), limit = 3) { it })
    }

    @Test
    fun `more pages load only while below the limit`() {
        assertTrue(PlaylistPaging.hasMore(loadedCount = 2, hasNextPage = true, limit = 3))
        assertFalse(PlaylistPaging.hasMore(loadedCount = 3, hasNextPage = true, limit = 3))
        assertFalse(PlaylistPaging.hasMore(loadedCount = 2, hasNextPage = false, limit = 3))
    }
}
