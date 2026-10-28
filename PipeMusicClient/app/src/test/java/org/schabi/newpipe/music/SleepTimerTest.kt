package org.schabi.newpipe.music

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.schabi.newpipe.music.playback.SleepTimer

class SleepTimerTest {

    @Test
    fun `combines hours and minutes`() {
        assertEquals(95, SleepTimer.customMinutes("1", "35"))
        assertEquals(120, SleepTimer.customMinutes("2", ""))
        assertEquals(7, SleepTimer.customMinutes("", " 7 "))
    }

    @Test
    fun `minutes may exceed sixty`() {
        assertEquals(150, SleepTimer.customMinutes("", "150"))
    }

    @Test
    fun `rejects empty, zero and non numeric input`() {
        assertNull(SleepTimer.customMinutes("", ""))
        assertNull(SleepTimer.customMinutes("0", "0"))
        assertNull(SleepTimer.customMinutes("1", "-5"))
        assertNull(SleepTimer.customMinutes("a", "10"))
        assertNull(SleepTimer.customMinutes("1.5", ""))
    }

    @Test
    fun `rejects totals above one day`() {
        assertEquals(SleepTimer.MAX_MINUTES, SleepTimer.customMinutes("24", "0"))
        assertNull(SleepTimer.customMinutes("24", "1"))
        assertNull(SleepTimer.customMinutes("", "99999"))
    }
}
