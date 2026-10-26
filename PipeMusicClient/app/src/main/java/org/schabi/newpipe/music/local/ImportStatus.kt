package org.schabi.newpipe.music.local

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ImportState(
    val running: Boolean = false,
    val current: Int = 0,
    val total: Int = 0,
    val currentName: String = "",
    val added: Int = 0,
    val skipped: Int = 0,
    val failed: Int = 0,
    val finished: Boolean = false,
    val error: String? = null
) {
    val fraction: Float
        get() = if (total > 0) (current.toFloat() / total).coerceIn(0f, 1f) else 0f
}

object ImportStatus {

    private val _state = MutableStateFlow(ImportState())
    val state: StateFlow<ImportState> = _state.asStateFlow()

    fun begin() {
        _state.value = ImportState(running = true)
    }

    fun update(transform: (ImportState) -> ImportState) {
        _state.update(transform)
    }

    fun finish(error: String? = null) {
        _state.update { it.copy(running = false, finished = true, error = error) }
    }

    fun clear() {
        _state.value = ImportState()
    }
}
