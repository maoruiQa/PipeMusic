package org.schabi.newpipe.music.playback

import org.schabi.newpipe.music.data.db.TrackEntity

data class PlayerUiState(
    val queue: List<TrackEntity> = emptyList(),
    val index: Int = -1,
    val current: TrackEntity? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val isResolving: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val repeatMode: Int = PlaybackQueue.REPEAT_ALL,
    val shuffle: Boolean = false,
    val sleepTimerEndsAt: Long = 0,
    val error: String? = null
) {
    val hasTrack: Boolean get() = current != null
}
