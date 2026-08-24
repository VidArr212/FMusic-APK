package com.fmusic.app.player

import com.fmusic.app.data.model.TrackItem

enum class RepeatMode {
    OFF, ALL, ONE
}

data class PlayerState(
    val currentTrack: TrackItem? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isShuffle: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val queue: List<TrackItem> = emptyList(),
    val currentIndex: Int = -1,
    val sleepTimerRemainingSeconds: Int = 0,
    val isSleepTimerActive: Boolean = false,
    val errorMessage: String? = null
) {
    val progress: Float
        get() = if (durationMs > 0) (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    fun formatDuration(ms: Long): String {
        val totalSec = ms / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        return String.format("%d:%02d", min, sec)
    }

    val currentPositionFormatted: String
        get() = formatDuration(currentPositionMs)

    val durationFormatted: String
        get() = formatDuration(durationMs)
}
