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

    private val sleepTimerManager = SleepTimerManager { pause() }
    private var progressJob: Job? = null
    private var resolveJob: Job? = null

    init {
        setupPlayerListener()
        observeSleepTimer()
    }

    private fun setupPlayerListener() {
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> _state.update { it.copy(isBuffering = true) }
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
                    Player.STATE_ENDED -> handleTrackEnded()
                    Player.STATE_IDLE -> _state.update { it.copy(isBuffering = false) }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _state.update { it.copy(isPlaying = isPlaying) }
                if (isPlaying) startProgressTicker() else stopProgressTicker()
            }

            override fun onPlayerError(error: PlaybackException) {
                _state.update {
                    it.copy(
                        isBuffering = false,
                        isPlaying = false,
                        errorMessage = "Gagal memutar. Coba lagi atau pilih lagu lain."
                    )
                }
                // Auto-retry with different stream provider
                _state.value.currentTrack?.let { track ->
                    scope.launch {
                        delay(1000)
                        retryPlayback(track)
                    }
                }
            }
        })
    }

    private suspend fun retryPlayback(track: TrackItem) {
        // Simple retry - attempt to re-resolve stream
        _state.update { it.copy(isBuffering = true, errorMessage = null) }
        try {
            val streamUrl = withContext(Dispatchers.IO) {
                AudioStreamResolver.resolveStreamUrl(context, track)
            }
            if (!streamUrl.isNullOrBlank()) {
                val mediaItem = buildMediaItem(track, streamUrl)
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()
                exoPlayer.play()
            } else {
                _state.update { it.copy(isBuffering = false, errorMessage = "Stream tidak tersedia untuk lagu ini.") }
            }
        } catch (e: Exception) {
            _state.update { it.copy(isBuffering = false, errorMessage = "Gagal memuat audio.") }
        }
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

    private fun stopProgressTicker() { progressJob?.cancel() }

    private fun buildMediaItem(track: TrackItem, streamUrl: String): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(track.title ?: "Unknown")
            .setArtist(track.getDisplayArtist())
            .setArtworkUri(track.thumbnail?.let { Uri.parse(it) })
            .build()
        return MediaItem.Builder().setUri(streamUrl).setMediaMetadata(metadata).build()
    }

    fun playTrack(track: TrackItem, queue: List<TrackItem> = listOf(track)) {
        // Only play tracks that have a videoId
        if (track.videoId.isNullOrBlank()) return

        val index = queue.indexOfFirst { it.videoId == track.videoId }.takeIf { it >= 0 } ?: 0

        _state.update {
            it.copy(
                currentTrack = track,
                queue = queue,
                currentIndex = index,
                isBuffering = true,
                errorMessage = null,
                currentPositionMs = 0L
            )
        }

        // Cancel any previous resolve job
        resolveJob?.cancel()

        // Record to recently played
        scope.launch(Dispatchers.IO) {
            try { repository.recordRecentlyPlayed(track) } catch (ignored: Exception) {}
        }

        // Resolve stream URL and play
        resolveJob = scope.launch {
            try {
                val streamUrl = withContext(Dispatchers.IO) {
                    AudioStreamResolver.resolveStreamUrl(context, track)
                }
                if (streamUrl.isNullOrBlank()) {
                    _state.update {
                        it.copy(isBuffering = false, errorMessage = "Tidak bisa memutar lagu ini. Coba lagu lain.")
                    }
                    return@launch
                }

                exoPlayer.setMediaItem(buildMediaItem(track, streamUrl))
                exoPlayer.prepare()
                exoPlayer.play()
            } catch (e: CancellationException) {
                // Normal cancellation, ignore
            } catch (e: Exception) {
                _state.update { it.copy(isBuffering = false, errorMessage = "Error: ${e.message}") }
            }
        }
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) exoPlayer.pause()
        else if (_state.value.currentTrack != null) exoPlayer.play()
    }

    fun play() { exoPlayer.play() }
    fun pause() { exoPlayer.pause() }

    fun seekTo(positionMs: Long) {
        exoPlayer.seekTo(positionMs.coerceAtLeast(0L))
        _state.update { it.copy(currentPositionMs = positionMs) }
    }

    fun seekToProgress(progress: Float) {
        val targetMs = (_state.value.durationMs * progress.coerceIn(0f, 1f)).toLong()
        seekTo(targetMs)
    }

    fun skipNext() {
        val queue = _state.value.queue
        if (queue.isEmpty()) return
        var nextIdx = _state.value.currentIndex + 1
        if (nextIdx >= queue.size) {
            if (_state.value.repeatMode == RepeatMode.ALL) nextIdx = 0
            else return
        }
        playTrack(queue[nextIdx], queue)
    }

    fun skipPrevious() {
        if (exoPlayer.currentPosition > 3000L) { exoPlayer.seekTo(0); return }
        val queue = _state.value.queue
        if (queue.isEmpty()) return
        var prevIdx = _state.value.currentIndex - 1
        if (prevIdx < 0) prevIdx = if (_state.value.repeatMode == RepeatMode.ALL) queue.size - 1 else 0
        playTrack(queue[prevIdx], queue)
    }

    fun toggleShuffle() { _state.update { it.copy(isShuffle = !it.isShuffle) } }

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
            RepeatMode.ONE -> { exoPlayer.seekTo(0); exoPlayer.play() }
            RepeatMode.ALL -> skipNext()
            RepeatMode.OFF -> {
                val queue = _state.value.queue
                val nextIdx = _state.value.currentIndex + 1
                if (nextIdx < queue.size) skipNext()
                else _state.update { it.copy(isPlaying = false) }
            }
        }
    }

    fun setSleepTimer(minutes: Int) { sleepTimerManager.startTimer(minutes) }
    fun cancelSleepTimer() { sleepTimerManager.cancelTimer() }
    fun getExoPlayer(): ExoPlayer = exoPlayer

    companion object {
        @Volatile private var instance: PlayerManager? = null
        fun getInstance(context: Context): PlayerManager {
            return instance ?: synchronized(this) {
                instance ?: PlayerManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
