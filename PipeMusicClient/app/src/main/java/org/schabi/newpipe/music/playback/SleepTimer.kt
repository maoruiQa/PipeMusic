package org.schabi.newpipe.music.playback

/**
 * Pure helpers for the sleep timer sheet, kept out of the composable so they can be unit tested.
 */
object SleepTimer {

    val PRESETS = listOf(15, 30, 45, 60, 90)

    /** Upper bound for a custom timer: one day is plenty and keeps the delay well inside Long. */
    const val MAX_MINUTES = 24 * 60

    /**
     * Converts the hour and minute fields of the custom timer into a total number of minutes.
     * Blank fields count as zero. Returns null for non numeric input, a zero total or a total
     * above [MAX_MINUTES].
     */
    fun customMinutes(hours: String, minutes: String): Int? {
        val h = parseField(hours) ?: return null
        val m = parseField(minutes) ?: return null
        if (h > MAX_MINUTES / 60 || m > MAX_MINUTES) {
            return null
        }
        val total = h * 60 + m
        return total.takeIf { it in 1..MAX_MINUTES }
    }

    private fun parseField(text: String): Int? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            return 0
        }
        if (trimmed.length > 4 || !trimmed.all { it.isDigit() }) {
            return null
        }
        return trimmed.toInt()
    }
}
