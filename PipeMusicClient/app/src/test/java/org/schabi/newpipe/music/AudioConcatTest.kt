package org.schabi.newpipe.music

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.schabi.newpipe.music.stream.AudioConcat

class AudioConcatTest {

    @Test
    fun `list file quotes every path and escapes single quotes`() {
        val list = AudioConcat.listFile(listOf("/music/a.m4a", "/music/it's_1.m4a"))

        assertEquals("file '/music/a.m4a'\nfile '/music/it'\\''s_1.m4a'\n", list)
    }

    @Test
    fun `parts share a suffix only when every extension matches`() {
        assertEquals("m4a", AudioConcat.commonSuffix(listOf("P1_3.m4a", "P2_4.M4A")))
        assertNull(AudioConcat.commonSuffix(listOf("P1_3.m4a", "P2_4.webm")))
        assertNull(AudioConcat.commonSuffix(listOf("noextension", "other")))
    }

    @Test
    fun `merged identity depends on the parts and their order`() {
        assertEquals("merged|3,4,5", AudioConcat.identity(listOf(3, 4, 5)))
        assertEquals(
            AudioConcat.baseName(listOf(3, 4, 5)),
            AudioConcat.baseName(listOf(3, 4, 5))
        )
        assertNotEquals(
            AudioConcat.baseName(listOf(3, 4, 5)),
            AudioConcat.baseName(listOf(3, 5, 4))
        )
    }
}
