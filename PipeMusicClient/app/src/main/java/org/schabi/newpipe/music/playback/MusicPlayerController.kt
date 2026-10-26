package org.schabi.newpipe.music.playback

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.schabi.newpipe.music.data.db.TrackEntity

/**
 * UI facing facade for [MusicPlayerService]. Commands are delivered with intents, playback state
 * is published back through a process wide [StateFlow]. This keeps the service alive across
 * activity recreation and avoids a binding lifecycle.
 */
object MusicPlayerController {

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private val _position = MutableStateFlow(0L)
    val position: StateFlow<Long> = _position.asStateFlow()

    internal fun publish(newState: PlayerUiState) {
        _state.value = newState
        _position.value = newState.positionMs.coerceAtLeast(0)
    }

    internal fun publishPosition(positionMs: Long) {
        _position.value = positionMs.coerceAtLeast(0)
    }

    internal fun update(transform: (PlayerUiState) -> PlayerUiState) {
        _state.update(transform)
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    fun playQueue(
        context: Context,
        tracks: List<TrackEntity>,
        startIndex: Int = 0,
        positionMs: Long = 0L
    ) {
        if (tracks.isEmpty()) {
            return
        }
        val index = startIndex.coerceIn(0, tracks.lastIndex)
        _position.value = positionMs
        _state.update {
            it.copy(
                queue = tracks,
                index = index,
                current = tracks[index],
                positionMs = positionMs,
                durationMs = tracks[index].durationMs,
                isResolving = true,
                isBuffering = true,
                error = null
            )
        }
        send(context, MusicPlayerService.ACTION_PLAY_QUEUE) {
            putExtra(MusicPlayerService.EXTRA_IDS, tracks.map { track -> track.id }.toLongArray())
            putExtra(MusicPlayerService.EXTRA_INDEX, index)
            putExtra(MusicPlayerService.EXTRA_POSITION, positionMs)
        }
    }

    fun playSingle(context: Context, track: TrackEntity, positionMs: Long = 0L) {
        playQueue(context, listOf(track), 0, positionMs)
    }

    fun toggle(context: Context) {
        if (!_state.value.hasTrack) {
            return
        }
        _state.update { it.copy(isPlaying = !it.isPlaying) }
        send(context, MusicPlayerService.ACTION_TOGGLE)
    }

    fun next(context: Context) {
        if (!_state.value.hasTrack) {
            return
        }
        send(context, MusicPlayerService.ACTION_NEXT)
    }

    fun previous(context: Context) {
        if (!_state.value.hasTrack) {
            return
        }
        send(context, MusicPlayerService.ACTION_PREVIOUS)
    }

    fun seekTo(context: Context, positionMs: Long) {
        if (!_state.value.hasTrack) {
            return
        }
        _position.value = positionMs.coerceAtLeast(0)
        _state.update { it.copy(positionMs = positionMs.coerceAtLeast(0)) }
        send(context, MusicPlayerService.ACTION_SEEK) {
            putExtra(MusicPlayerService.EXTRA_POSITION, positionMs)
        }
    }

    fun setRepeatMode(context: Context, mode: Int) {
        _state.update { it.copy(repeatMode = mode) }
        send(context, MusicPlayerService.ACTION_SET_REPEAT) {
            putExtra(MusicPlayerService.EXTRA_REPEAT, mode)
        }
    }

    fun toggleShuffle(context: Context) {
        _state.update { it.copy(shuffle = !it.shuffle) }
        send(context, MusicPlayerService.ACTION_TOGGLE_SHUFFLE)
    }

    fun jumpTo(context: Context, index: Int) {
        send(context, MusicPlayerService.ACTION_JUMP) {
            putExtra(MusicPlayerService.EXTRA_INDEX, index)
        }
    }

    fun removeFromQueue(context: Context, index: Int) {
        send(context, MusicPlayerService.ACTION_REMOVE) {
            putExtra(MusicPlayerService.EXTRA_INDEX, index)
        }
    }

    fun setSleepTimer(context: Context, minutes: Int) {
        if (!_state.value.hasTrack) {
            return
        }
        send(context, MusicPlayerService.ACTION_SLEEP_TIMER) {
            putExtra(MusicPlayerService.EXTRA_MINUTES, minutes)
        }
    }

    fun stop(context: Context) {
        context.stopService(Intent(context, MusicPlayerService::class.java))
        _state.value = PlayerUiState()
        _position.value = 0
    }

    private fun send(context: Context, action: String, configure: Intent.() -> Unit = {}) {
        val intent = Intent(context, MusicPlayerService::class.java).apply {
            this.action = action
            configure()
        }
        runCatching { ContextCompat.startForegroundService(context, intent) }
    }
}
