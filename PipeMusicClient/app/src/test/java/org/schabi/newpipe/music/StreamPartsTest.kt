package org.schabi.newpipe.music

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.schabi.newpipe.music.stream.StreamParts

class StreamPartsTest {

    private val parts = listOf(
        "https://www.bilibili.com/video/BV1xx411c7mD?p=1",
        "https://www.bilibili.com/video/BV1xx411c7mD?p=2",
        "https://www.bilibili.com/video/BV1xx411c7mD?p=3"
    )

    @Test
    fun `part number is read from the p parameter`() {
        assertEquals(3, StreamParts.partNumber("https://www.bilibili.com/video/BV1xx?p=3"))
        assertEquals(12, StreamParts.partNumber("https://b23.tv/x?spm=1&p=12&t=30"))
        assertNull(StreamParts.partNumber("https://www.bilibili.com/video/BV1xx"))
        assertNull(StreamParts.partNumber("https://www.bilibili.com/video/BV1xx?spm=2"))
    }

    @Test
    fun `video title drops the part prefix`() {
        val parts = listOf("Intro | Basics", "Vectors")
        assertEquals("Course | 2016", StreamParts.videoTitle("P1 Intro | Basics | Course | 2016", parts))
        assertEquals("Course", StreamParts.videoTitle("P2 Vectors | Course", parts))
        assertEquals("Course", StreamParts.videoTitle("P9 Other | Course", parts))
        assertEquals("Single | video", StreamParts.videoTitle("Single | video", emptyList()))
    }

    @Test
    fun `exact URLs match their own part`() {
        assertEquals(1, StreamParts.indexOf(parts, parts[1]))
    }

    @Test
    fun `pasted links match by part number`() {
        assertEquals(
            2,
            StreamParts.indexOf(parts, "https://www.bilibili.com/video/BV1xx411c7mD/?p=3&spm=a")
        )
        assertEquals(0, StreamParts.indexOf(parts, "https://www.bilibili.com/video/BV1xx411c7mD"))
        assertEquals(-1, StreamParts.indexOf(parts, "https://www.bilibili.com/video/BV1x?p=9"))
    }

    @Test
    fun `blank part names fall back to the part number`() {
        assertEquals("Intro", StreamParts.title("  Intro ", 1))
        assertEquals("P4", StreamParts.title(" ", 4))
        assertEquals("P2", StreamParts.title(null, 2))
    }
}
