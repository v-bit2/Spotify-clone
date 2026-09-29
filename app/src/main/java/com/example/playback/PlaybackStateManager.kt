package com.example.playback

import com.example.data.model.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PlaybackStateData(
    val currentTrack: Track? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val queue: List<Track> = emptyList(),
    val currentIndex: Int = -1,
    val isShuffle: Boolean = false,
    val isRepeat: Boolean = false,
    val errorMessage: String? = null
) {
    val progressFraction: Float
        get() = if (durationMs > 0) (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
}

object PlaybackStateManager {
    private val _playbackState = MutableStateFlow(PlaybackStateData())
    val playbackState: StateFlow<PlaybackStateData> = _playbackState.asStateFlow()

    fun updateState(updater: (PlaybackStateData) -> PlaybackStateData) {
        _playbackState.value = updater(_playbackState.value)
    }

    fun getCurrentState(): PlaybackStateData = _playbackState.value

    fun reset() {
        _playbackState.value = PlaybackStateData()
    }
}
