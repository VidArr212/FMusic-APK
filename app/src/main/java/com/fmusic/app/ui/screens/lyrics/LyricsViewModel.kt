package com.fmusic.app.ui.screens.lyrics

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fmusic.app.data.model.LyricLine
import com.fmusic.app.data.model.LyricsResponse
import com.fmusic.app.data.model.TrackItem
import com.fmusic.app.data.repository.MusicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LyricsUiState(
    val isLoading: Boolean = true,
    val syncedLines: List<LyricLine> = emptyList(),
    val plainLyrics: String? = null,
    val source: String? = null,
    val errorMessage: String? = null
)

class LyricsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MusicRepository(application)

    private val _uiState = MutableStateFlow(LyricsUiState())
    val uiState: StateFlow<LyricsUiState> = _uiState.asStateFlow()

    fun loadLyrics(track: TrackItem) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val title = track.getCleanTitle()
            val artist = track.getDisplayArtist()

            val res = repository.getLyrics(title, artist, browseId = track.browseId)
            res.onSuccess { response ->
                val lines = response.parseSyncedLines()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        syncedLines = lines,
                        plainLyrics = response.plain,
                        source = response.source,
                        errorMessage = if (lines.isEmpty() && response.plain.isNullOrBlank()) "Lirik tidak ditemukan untuk lagu ini." else null
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Gagal memuat lirik"
                    )
                }
            }
        }
    }
}
