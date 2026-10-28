package org.schabi.newpipe.music

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.schabi.newpipe.music.update.AppVersion
import org.schabi.newpipe.music.update.ReleaseAsset

class AppVersionTest {

    @Test
    fun `newer tags are detected`() {
        assertTrue(AppVersion.isNewer("v1.0.1", "1.0.0"))
        assertTrue(AppVersion.isNewer("v1.1", "1.0.9"))
        assertTrue(AppVersion.isNewer("2.0.0", "1.99.99"))
        assertTrue(AppVersion.isNewer("v1.0.10", "1.0.9"))
        assertTrue(AppVersion.isNewer("v1.0.0.1", "1.0.0"))
    }

    @Test
    fun `same or older tags are not newer`() {
        assertFalse(AppVersion.isNewer("v1.0.0", "1.0.0"))
        assertFalse(AppVersion.isNewer("v1.0", "1.0.0"))
        assertFalse(AppVersion.isNewer("v0.9.9", "1.0.0"))
        assertFalse(AppVersion.isNewer("v1.0.0-beta", "1.0.0-debug"))
    }

    @Test
    fun `malformed remote tags never prompt`() {
        assertFalse(AppVersion.isNewer("nightly", "1.0.0"))
        assertFalse(AppVersion.isNewer("", "1.0.0"))
        assertFalse(AppVersion.isNewer("v1..2", "1.0.0"))
    }

    @Test
    fun `normalize strips prefix and suffixes`() {
        assertEquals("1.2.3", AppVersion.normalize(" v1.2.3-rc1 "))
        assertEquals("1.2.3", AppVersion.normalize("V1.2.3+45"))
    }

    private val assets = listOf(
        ReleaseAsset("PipeMusic_1.1.0-armeabi-v7a-release.apk", "https://x/v7a"),
        ReleaseAsset("PipeMusic_1.1.0-arm64-v8a-debug.apk", "https://x/v8a-debug"),
        ReleaseAsset("PipeMusic_1.1.0-arm64-v8a-release.apk", "https://x/v8a"),
        ReleaseAsset("PipeMusic_1.1.0-universal-release.apk", "https://x/universal"),
        ReleaseAsset("checksums.txt", "https://x/sums")
    )

    @Test
    fun `picks the preferred abi and release build`() {
        val picked = AppVersion.pickApk(assets, listOf("arm64-v8a", "armeabi-v7a"))
        assertEquals("https://x/v8a", picked?.url)
    }

    @Test
    fun `falls back to universal then any apk`() {
        assertEquals("https://x/universal", AppVersion.pickApk(assets, listOf("x86"))?.url)
        val noUniversal = listOf(ReleaseAsset("PipeMusic.apk", "https://x/any"))
        assertEquals("https://x/any", AppVersion.pickApk(noUniversal, listOf("x86"))?.url)
        assertNull(AppVersion.pickApk(listOf(ReleaseAsset("notes.md", "https://x")), listOf("x86")))
    }
}
