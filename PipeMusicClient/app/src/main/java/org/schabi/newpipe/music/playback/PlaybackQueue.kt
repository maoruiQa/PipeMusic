package org.schabi.newpipe.music.playback

import org.schabi.newpipe.music.data.db.TrackEntity
import kotlin.random.Random

/**
 * Pure playback queue logic, independent from ExoPlayer and Android so that every repeat and
 * shuffle edge case can be unit tested.
 */
class PlaybackQueue(initial: List<TrackEntity> = emptyList(), startIndex: Int = 0) {

    private val items = ArrayList<TrackEntity>(initial)
    private var order: List<Int> = emptyList()
    private var orderPosition = 0

    var repeatMode: Int = REPEAT_ALL
        private set
    var shuffle: Boolean = false
        private set

    var currentIndex: Int = if (items.isEmpty()) -1 else startIndex.coerceIn(0, items.lastIndex)
        private set

    init {
        rebuildOrder()
    }

    val size: Int get() = items.size
    val tracks: List<TrackEntity> get() = items.toList()
    val current: TrackEntity? get() = items.getOrNull(currentIndex)
    val isEmpty: Boolean get() = items.isEmpty()

    fun setRepeatMode(mode: Int) {
        repeatMode = mode
    }

    fun setShuffle(enabled: Boolean) {
        if (shuffle == enabled) {
            return
        }
        shuffle = enabled
        rebuildOrder()
    }

    fun replaceAll(newItems: List<TrackEntity>, startIndex: Int) {
        items.clear()
        items.addAll(newItems)
        currentIndex = if (items.isEmpty()) -1 else startIndex.coerceIn(0, items.lastIndex)
        rebuildOrder()
    }

    fun jumpTo(index: Int): TrackEntity? {
        if (index !in items.indices) {
            return null
        }
        currentIndex = index
        if (shuffle) {
            orderPosition = order.indexOf(index).coerceAtLeast(0)
        }
        return current
    }

    /**
     * Advances the queue.
     *
     * [automatic] is true when a song reached its end. A one-song repeat then replays the same
     * song, while a manual "next" still moves on, matching the behaviour of mainstream players.
     */
    fun next(automatic: Boolean = false): TrackEntity? {
        if (items.isEmpty()) {
            return null
        }
        if (automatic && repeatMode == REPEAT_ONE) {
            return current
        }
        if (shuffle) {
            if (orderPosition < order.lastIndex) {
                orderPosition += 1
                currentIndex = order[orderPosition]
                return current
            }
        } else if (currentIndex < items.lastIndex) {
            currentIndex += 1
            return current
        }
        if (repeatMode == REPEAT_ALL) {
            if (shuffle) {
                rebuildOrder(startWithCurrent = false)
                orderPosition = 0
                currentIndex = order.firstOrNull() ?: 0
            } else {
                currentIndex = 0
            }
            return current
        }
        return null
    }

    fun previous(): TrackEntity? {
        if (items.isEmpty()) {
            return null
        }
        if (shuffle) {
            if (orderPosition > 0) {
                orderPosition -= 1
                currentIndex = order[orderPosition]
            } else if (repeatMode == REPEAT_ALL) {
                orderPosition = order.lastIndex.coerceAtLeast(0)
                currentIndex = order.getOrElse(orderPosition) { 0 }
            }
            return current
        }
        if (currentIndex > 0) {
            currentIndex -= 1
        } else if (repeatMode == REPEAT_ALL) {
            currentIndex = items.lastIndex
        }
        return current
    }

    fun removeAt(index: Int): TrackEntity? {
        if (index !in items.indices) {
            return null
        }
        val removingCurrent = index == currentIndex
        items.removeAt(index)
        if (items.isEmpty()) {
            currentIndex = -1
            order = emptyList()
            orderPosition = 0
            return null
        }
        if (index < currentIndex) {
            currentIndex -= 1
        } else if (removingCurrent) {
            currentIndex = currentIndex.coerceAtMost(items.lastIndex)
        }
        rebuildOrder()
        return current
    }

    fun move(from: Int, to: Int) {
        if (from !in items.indices || to !in items.indices || from == to) {
            return
        }
        val currentTrack = current
        val moved = items.removeAt(from)
        items.add(to, moved)
        if (currentTrack != null) {
            currentIndex = items.indexOfFirst { it.id == currentTrack.id }
        }
        rebuildOrder()
    }

    fun addNext(track: TrackEntity) {
        if (items.isEmpty() || currentIndex < 0) {
            items.add(track)
            currentIndex = 0
            rebuildOrder()
            return
        }
        items.add(currentIndex + 1, track)
        rebuildOrder()
    }

    fun append(track: TrackEntity) {
        items.add(track)
        if (currentIndex < 0) {
            currentIndex = 0
        }
        rebuildOrder()
    }

    /**
     * Replaces the current song with every part of the multi-part video it belongs to, keeping
     * the current song playing. Parts already queued elsewhere are moved instead of duplicated.
     * Does nothing when the current song has changed since [anchorId] was looked up.
     */
    fun expandCurrent(anchorId: Long, parts: List<TrackEntity>): Boolean {
        val anchor = current ?: return false
        if (anchor.id != anchorId || parts.size < 2 || parts.none { it.id == anchorId }) {
            return false
        }
        val partIds = parts.mapTo(HashSet()) { it.id }
        val expanded = ArrayList<TrackEntity>(items.size + parts.size)
        items.forEachIndexed { index, track ->
            when {
                index == currentIndex -> expanded.addAll(parts)
                track.id !in partIds -> expanded.add(track)
            }
        }
        items.clear()
        items.addAll(expanded)
        currentIndex = items.indexOfFirst { it.id == anchorId }
        rebuildOrder()
        return true
    }

    fun peekNext(): TrackEntity? {
        if (items.isEmpty()) {
            return null
        }
        if (repeatMode == REPEAT_ONE) {
            return current
        }
        if (shuffle) {
            val nextOrder = orderPosition + 1
            if (nextOrder <= order.lastIndex) {
                return items.getOrNull(order[nextOrder])
            }
            return if (repeatMode == REPEAT_ALL) items.getOrNull(order.firstOrNull() ?: 0) else null
        }
        if (currentIndex < items.lastIndex) {
            return items[currentIndex + 1]
        }
        return if (repeatMode == REPEAT_ALL) items.firstOrNull() else null
    }

    private fun rebuildOrder(startWithCurrent: Boolean = true) {
        order = items.indices.toMutableList()
        if (!shuffle) {
            orderPosition = 0
            return
        }
        val current = if (startWithCurrent) currentIndex else -1
        val rest = items.indices.filter { it != current }.shuffled(Random.Default)
        order = if (current in items.indices) {
            (listOf(current) + rest).toMutableList()
        } else {
            rest.toMutableList()
        }
        orderPosition = 0
    }

    companion object {
        const val REPEAT_OFF = 0
        const val REPEAT_ONE = 1
        const val REPEAT_ALL = 2
    }
}
