package org.schabi.newpipe.music

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.schabi.newpipe.music.data.db.TRACK_TYPE_LOCAL
import org.schabi.newpipe.music.data.db.TRACK_TYPE_STREAM
import org.schabi.newpipe.music.data.db.TrackEntity
import org.schabi.newpipe.music.stream.PartGroups
import org.schabi.newpipe.music.stream.PartPosition
import org.schabi.newpipe.music.stream.TrackEntry

class PartGroupsTest {

    private val bili = 5
    private val supportsParts: (Int) -> Boolean = { it == bili }

    private fun track(
        id: Long,
        url: String?,
        title: String = "Song $id",
        album: String = "",
        serviceId: Int = bili,
        type: Int = TRACK_TYPE_STREAM
    ) = TrackEntity(
        id = id,
        identity = "track-$id",
        type = type,
        title = title,
        artist = "Artist",
        album = album,
        durationMs = 1_000,
        remoteUrl = url,
        serviceId = serviceId
    )

    private val video = "https://www.bilibili.com/video/BV1xx411c7mD"

    @Test
    fun `video URL drops only the part number`() {
        assertEquals(video, PartGroups.videoUrl("$video?p=3"))
        assertEquals("$video?spm=1&t=30", PartGroups.videoUrl("$video?spm=1&p=12&t=30"))
        assertEquals("$video#reply", PartGroups.videoUrl("$video?p=2#reply"))
        assertEquals(video, PartGroups.videoUrl(video))
    }

    @Test
    fun `only stream songs of services with parts have a key`() {
        assertEquals("$bili|$video", PartGroups.key(track(1, "$video?p=2"), supportsParts))
        assertNull(PartGroups.key(track(2, "$video?p=2", serviceId = 0), supportsParts))
        assertNull(PartGroups.key(track(3, null, type = TRACK_TYPE_LOCAL), supportsParts))
    }

    @Test
    fun `parts fold into one entry where the first part is, in part order`() {
        val tracks = listOf(
            track(1, "https://www.bilibili.com/video/BVother"),
            track(3, "$video?p=3", album = "Concert"),
            track(2, "$video?p=2"),
            track(4, "https://example.com/song", serviceId = 0),
            track(5, video)
        )
        val entries = PartGroups.group(tracks, enabled = true, supportsParts)

        assertEquals(3, entries.size)
        assertEquals(1L, (entries[0] as TrackEntry.Single).track.id)
        val parts = entries[1] as TrackEntry.Parts
        assertEquals("Concert", parts.title)
        assertEquals(listOf(5L, 2L, 3L), parts.tracks.map { it.id })
        assertEquals(4L, (entries[2] as TrackEntry.Single).track.id)
    }

    @Test
    fun `a lone part and a disabled setting keep songs on their own`() {
        val tracks = listOf(track(1, "$video?p=1"), track(2, "$video?p=2"))
        assertEquals(
            listOf(1L),
            PartGroups.group(tracks.take(1), enabled = true, supportsParts)
                .map { (it as TrackEntry.Single).track.id }
        )
        assertEquals(
            listOf(1L, 2L),
            PartGroups.group(tracks, enabled = false, supportsParts)
                .map { (it as TrackEntry.Single).track.id }
        )
    }

    @Test
    fun `position uses the looked up count, then the queued parts`() {
        val current = track(2, "$video?p=2")
        val queue = listOf(track(1, "$video?p=1"), current, track(9, "https://www.bilibili.com/video/BVother"))
        val key = PartGroups.key(current, supportsParts)!!

        assertEquals(PartPosition(2, 12), PartGroups.position(current, queue, mapOf(key to 12), supportsParts))
        assertEquals(PartPosition(2, 2), PartGroups.position(current, queue, emptyMap(), supportsParts))
        assertEquals(PartPosition(2, null), PartGroups.position(current, listOf(current), emptyMap(), supportsParts))
    }

    @Test
    fun `a video known to have one part has no position`() {
        val single = track(1, video)
        val key = PartGroups.key(single, supportsParts)!!
        assertNull(PartGroups.position(single, listOf(single), mapOf(key to 1), supportsParts))
        assertNull(PartGroups.position(single, listOf(single), emptyMap(), supportsParts))
    }

    @Test
    fun `title falls back to the first part's song title`() {
        val parts = listOf(track(1, "$video?p=1", title = "Intro"), track(2, "$video?p=2"))
        assertEquals("Intro", PartGroups.title(parts))
    }
}
