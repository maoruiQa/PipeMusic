package org.schabi.newpipe.music.ui

import android.content.Context
import org.schabi.newpipe.R
import java.util.Locale
import kotlin.math.max

fun formatDuration(durationMs: Long): String {
    if (durationMs <= 0) {
        return "--:--"
    }
    val totalSeconds = durationMs / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
    }
}

fun formatTotalDuration(context: Context, durationMs: Long): String {
    if (durationMs <= 0) {
        return context.getString(R.string.pm_duration_zero)
    }
    val minutes = max(1, durationMs / 60_000)
    return if (minutes >= 60) {
        val hours = minutes / 60
        val remain = minutes % 60
        if (remain == 0L) {
            context.getString(R.string.pm_duration_hours, hours)
        } else {
            context.getString(R.string.pm_duration_hours_minutes, hours, remain)
        }
    } else {
        context.getString(R.string.pm_duration_minutes, minutes)
    }
}

fun formatBytes(bytes: Long): String {
    if (bytes <= 0) {
        return "0 MB"
    }
    val mb = bytes / 1024.0 / 1024.0
    return if (mb >= 1024) {
        String.format(Locale.getDefault(), "%.2f GB", mb / 1024)
    } else if (mb >= 10) {
        String.format(Locale.getDefault(), "%.0f MB", mb)
    } else {
        String.format(Locale.getDefault(), "%.1f MB", mb)
    }
}
