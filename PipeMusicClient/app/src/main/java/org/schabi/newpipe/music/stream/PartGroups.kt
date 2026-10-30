package org.schabi.newpipe.music.stream

import org.schabi.newpipe.music.data.db.TRACK_TYPE_STREAM
import org.schabi.newpipe.music.data.db.TrackEntity

/** One row of a song list: a song on its own, or the parts of one multi-part video. */
sealed interface TrackEntry {
    val tracks: List<TrackEntity>

    data class Single(val track: TrackEntity) : TrackEntry {
        override val tracks: List<TrackEntity> get() = listOf(track)
    }

    /** Parts of the video [key], ordered by part number. */
    data class Parts(
        val key: String,
        val title: String,
        override val tracks: List<TrackEntity>
    ) : TrackEntry
}

/** Where a song sits in its multi-part video; [count] is null while the parts are unknown. */
data class PartPosition(val number: Int, val count: Int?)

/** Groups the parts of multi-part videos in song lists. */
object PartGroups {

    /**
     * The video [track] is a part of, or null for songs of services without parts. The parts of
     * one video only differ in the `p` query parameter of their URL.
     */
    fun key(track: TrackEntity, supportsParts: (Int) -> Boolean): String? {
        val url = track.remoteUrl ?: return null
        if (track.type != TRACK_TYPE_STREAM || !supportsParts(track.serviceId)) {
            return null
        }
        return key(track.serviceId, url)
    }

    fun key(serviceId: Int, url: String): String = "$serviceId|${videoUrl(url)}"

    /** [url] without its part number. */
    fun videoUrl(url: String): String {
        val query = url.indexOf('?')
        if (query < 0) {
            return url
        }
        val fragment = url.indexOf('#', query).let { if (it < 0) url.length else it }
        val params = url.substring(query + 1, fragment).split('&')
            .filter { it.isNotEmpty() && !it.startsWith("p=") }
        val base = url.substring(0, query)
        val rest = url.substring(fragment)
        return if (params.isEmpty()) base + rest else base + "?" + params.joinToString("&") + rest
    }

    /** Part number of [track]; a URL without one is the first part. */
    fun partNumber(track: TrackEntity): Int =
        track.remoteUrl?.let { StreamParts.partNumber(it) } ?: 1

    /**
     * [tracks] with the parts of each video folded into one entry, placed where its first part
     * is. A video with a single song in the list stays a plain song. When [enabled] is false
     * every song stays on its own.
     */
    fun group(
        tracks: List<TrackEntity>,
        enabled: Boolean,
        supportsParts: (Int) -> Boolean
    ): List<TrackEntry> {
        if (!enabled) {
            return tracks.map { TrackEntry.Single(it) }
        }
        val keys = tracks.map { key(it, supportsParts) }
        val members = HashMap<String, MutableList<TrackEntity>>()
        tracks.forEachIndexed { index, track ->
            keys[index]?.let { members.getOrPut(it) { ArrayList() }.add(track) }
        }
        val placed = HashSet<String>()
        val entries = ArrayList<TrackEntry>()
        tracks.forEachIndexed { index, track ->
            val key = keys[index]
            val parts = key?.let { members[it] }
            when {
                key == null || parts == null || parts.size < 2 ->
                    entries.add(TrackEntry.Single(track))
                placed.add(key) -> {
                    val ordered = parts.sortedBy { partNumber(it) }
                    entries.add(TrackEntry.Parts(key, title(ordered), ordered))
                }
            }
        }
        return entries
    }

    /**
     * The part [track] is of its video, or null when it is not a part of a multi-part video.
     * [counts] holds the part counts looked up so far; without one the queued parts count.
     */
    fun position(
        track: TrackEntity,
        queue: List<TrackEntity>,
        counts: Map<String, Int>,
        supportsParts: (Int) -> Boolean
    ): PartPosition? {
        val key = key(track, supportsParts) ?: return null
        val number = track.remoteUrl?.let { StreamParts.partNumber(it) }
        val count = counts[key]
            ?: queue.count { key(it, supportsParts) == key }.takeIf { it >= 2 }
        return when {
            count != null && count >= 2 -> PartPosition(number ?: 1, count)
            count == null && number != null -> PartPosition(number, null)
            else -> null
        }
    }

    /** The video's title, stored as each part's album; older parts fall back to a song title. */
    fun title(parts: List<TrackEntity>): String =
        parts.firstNotNullOfOrNull { part -> part.album.takeIf { it.isNotBlank() } }
            ?: parts.first().title
}
