package org.schabi.newpipe.music.update

/**
 * Pure version and release asset helpers for [UpdateChecker], kept free of Android types so they
 * can be unit tested.
 */
object AppVersion {

    /**
     * Returns true when [remote] (a release tag such as `v1.2.0`) is strictly newer than
     * [current] (the app's version name). Only the numeric `major.minor.patch...` part is compared;
     * a leading `v` and any `-suffix` / `+build` part are ignored. Unparseable versions are never
     * reported as newer, so a malformed tag cannot trigger an update prompt.
     */
    fun isNewer(remote: String, current: String): Boolean {
        val remoteParts = numericParts(remote) ?: return false
        val currentParts = numericParts(current) ?: return true
        val length = maxOf(remoteParts.size, currentParts.size)
        for (i in 0 until length) {
            val r = remoteParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (r != c) {
                return r > c
            }
        }
        return false
    }

    /** Strips a leading `v` and anything after `-`, `+` or whitespace. */
    fun normalize(version: String): String = version.trim()
        .removePrefix("v")
        .removePrefix("V")
        .split('-', '+', ' ')
        .first()

    private fun numericParts(version: String): List<Int>? {
        val parts = normalize(version).split('.')
        if (parts.isEmpty() || parts.any { it.isEmpty() || it.length > 9 || !it.all(Char::isDigit) }) {
            return null
        }
        return parts.map { it.toInt() }
    }

    /**
     * Picks the APK asset best suited to this device: one built for a supported ABI (in the
     * device's preference order), then the universal build, then any APK. Release builds win over
     * debug builds at the same rank.
     */
    fun pickApk(assets: List<ReleaseAsset>, supportedAbis: List<String>): ReleaseAsset? {
        val apks = assets.filter { it.name.endsWith(".apk", ignoreCase = true) }
            .sortedBy { it.name.contains("debug", ignoreCase = true) }
        if (apks.isEmpty()) {
            return null
        }
        for (abi in supportedAbis) {
            apks.firstOrNull { it.name.contains("-$abi-") }?.let { return it }
        }
        return apks.firstOrNull { it.name.contains("universal", ignoreCase = true) }
            ?: apks.first()
    }
}

data class ReleaseAsset(val name: String, val url: String)

data class ReleaseInfo(
    val version: String,
    val notes: String,
    val pageUrl: String,
    val downloadUrl: String?
)
