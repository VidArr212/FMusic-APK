package com.fmusic.app.player

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.fmusic.app.data.local.FMusicDatabase
import com.fmusic.app.data.model.TrackItem
import com.fmusic.app.data.repository.MusicRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class PlayerManager private constructor(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val repository = MusicRepository(context)

    private var exoPlayer: ExoPlayer = ExoPlayer.Builder(context).build()

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private val sleepTimerManager = SleepTimerManager {
        pause()
    }

    private var progressJob: Job? = null

    init {
        setupPlayerListener()
        observeSleepTimer()
    }

    private fun setupPlayerListener() {
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> {
                        _state.update { it.copy(isBuffering = true) }
                    }
                    Player.STATE_READY -> {
                        _state.update {
                            it.copy(
                                isBuffering = false,
                                durationMs = exoPlayer.duration.coerceAtLeast(0L),
                                isPlaying = exoPlayer.isPlaying
                            )
                        }
                        startProgressTicker()
                    }
                    Player.STATE_ENDED -> {
                        handleTrackEnded()
                    }
                    Player.STATE_IDLE -> {
                        _state.update { it.copy(isBuffering = false) }
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _state.update { it.copy(isPlaying = isPlaying) }
                if (isPlaying) {
                    startProgressTicker()
                } else {
                    stopProgressTicker()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                _state.update {
                    it.copy(
                        isBuffering = false,
                        isPlaying = false,
                        errorMessage = "Playback error: ${error.localizedMessage}"
                    )
                }
            }
        })
    }

    private fun observeSleepTimer() {
        scope.launch {
            sleepTimerManager.remainingSeconds.collect { sec ->
                _state.update { it.copy(sleepTimerRemainingSeconds = sec) }
            }
        }
        scope.launch {
            sleepTimerManager.isActive.collect { active ->
                _state.update { it.copy(isSleepTimerActive = active) }
            }
        }
    }

    private fun startProgressTicker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                if (exoPlayer.isPlaying) {
                    _state.update {
                        it.copy(
                            currentPositionMs = exoPlayer.currentPosition.coerceAtLeast(0L),
                            durationMs = exoPlayer.duration.coerceAtLeast(0L)
                        )
                    }
                }
                delay(200)
            }
        }
    }

    private fun stopProgressTicker() {
        progressJob?.cancel()
    }

    fun playTrack(track: TrackItem, queue: List<TrackItem> = listOf(track)) {
        val index = queue.indexOfFirst { it.videoId == track.videoId }.let { if (it >= 0) it else 0 }
        _state.update {
            it.copy(
                currentTrack = track,
                queue = queue,
                currentIndex = index,
                isBuffering = true,
                errorMessage = null
            )
        }

        // Record to recently played
        scope.launch(Dispatchers.IO) {
            repository.recordRecentlyPlayed(track)
        }

        // Resolve stream URL and play
        scope.launch {
            val streamUrl = AudioStreamResolver.resolveStreamUrl(context, track)
            if (streamUrl.isNullOrBlank()) {
                _state.update {
                    it.copy(
                        isBuffering = false,
                        errorMessage = "Cannot stream this track. Check network."
                    )
                }
                return@launch
            }

            val mediaMetadata = MediaMetadata.Builder()
                .setTitle(track.title ?: "Unknown")
                .setArtist(track.getDisplayArtist())
                .setArtworkUri(if (track.thumbnail != null) Uri.parse(track.thumbnail) else null)
                .build()

            val mediaItem = MediaItem.Builder()
                .setUri(streamUrl)
                .setMediaMetadata(mediaMetadata)
                .build()

            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.play()
        }
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            if (_state.value.currentTrack != null) {
                exoPlayer.play()
            }
        }
    }

    fun play() {
        exoPlayer.play()
    }

    fun pause() {
        exoPlayer.pause()
    }

    fun seekTo(positionMs: Long) {
        exoPlayer.seekTo(positionMs)
        _state.update { it.copy(currentPositionMs = positionMs) }
    }

    fun seekToProgress(progress: Float) {
        val targetMs = (_state.value.durationMs * progress).toLong()
        seekTo(targetMs)
    }

    fun skipNext() {
        val currentQueue = _state.value.queue
        if (currentQueue.isEmpty()) return

        var nextIndex = _state.value.currentIndex + 1
        if (nextIndex >= currentQueue.size) {
            if (_state.value.repeatMode == RepeatMode.ALL) {
                nextIndex = 0
            } else {
                return
            }
        }
        val nextTrack = currentQueue[nextIndex]
        playTrack(nextTrack, currentQueue)
    }

    fun skipPrevious() {
        if (exoPlayer.currentPosition > 3000L) {
            exoPlayer.seekTo(0)
            return
        }
        val currentQueue = _state.value.queue
        if (currentQueue.isEmpty()) return

        var prevIndex = _state.value.currentIndex - 1
        if (prevIndex < 0) {
            prevIndex = if (_state.value.repeatMode == RepeatMode.ALL) currentQueue.size - 1 else 0
        }
        val prevTrack = currentQueue[prevIndex]
        playTrack(prevTrack, currentQueue)
    }

    fun toggleShuffle() {
        _state.update { it.copy(isShuffle = !it.isShuffle) }
    }

    fun toggleRepeat() {
        val nextMode = when (_state.value.repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        _state.update { it.copy(repeatMode = nextMode) }
    }

    private fun handleTrackEnded() {
        when (_state.value.repeatMode) {
            RepeatMode.ONE -> {
                exoPlayer.seekTo(0)
                exoPlayer.play()
            }
            RepeatMode.ALL -> {
                skipNext()
            }
            RepeatMode.OFF -> {
                val currentQueue = _state.value.queue
                val nextIndex = _state.value.currentIndex + 1
                if (nextIndex < currentQueue.size) {
                    skipNext()
                } else {
                    _state.update { it.copy(isPlaying = false) }
                }
            }
        }
    }

    // Sleep timer controls (5 to 30 mins)
    fun setSleepTimer(minutes: Int) {
        sleepTimerManager.startTimer(minutes)
    }

    fun cancelSleepTimer() {
        sleepTimerManager.cancelTimer()
    }

    fun getExoPlayer(): ExoPlayer = exoPlayer

    companion object {
        @Volatile
        private var instance: PlayerManager? = null

        fun getInstance(context: Context): PlayerManager {
            return instance ?: synchronized(this) {
                instance ?: PlayerManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
