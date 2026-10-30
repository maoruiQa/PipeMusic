package org.schabi.newpipe.music.stream

/**
 * Paging rules for playlists and albums opened from a link. Every page is loaded up front so
 * "play all" and "download all" cover the whole list, up to [MAX_SONGS].
 */
object PlaylistPaging {

    /** Keeps huge playlists (YouTube allows 5000 videos) from flooding the queue and downloads. */
    const val MAX_SONGS = 500

    /**
     * Appends [page] to [loaded], skipping songs whose [key] is already present (services may
     * repeat an item across page boundaries) and stopping at [limit].
     */
    fun <T> append(
        loaded: List<T>,
        page: List<T>,
        limit: Int = MAX_SONGS,
        key: (T) -> String
    ): List<T> {
        if (loaded.size >= limit) {
            return loaded
        }
        val seen = loaded.mapTo(HashSet(), key)
        val merged = ArrayList(loaded)
        for (item in page) {
            if (merged.size >= limit) {
                break
            }
            if (seen.add(key(item))) {
                merged.add(item)
            }
        }
        return merged
    }

    fun hasMore(loadedCount: Int, hasNextPage: Boolean, limit: Int = MAX_SONGS): Boolean =
        hasNextPage && loadedCount < limit
}
