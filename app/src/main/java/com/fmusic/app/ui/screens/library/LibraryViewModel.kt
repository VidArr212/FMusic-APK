package com.fmusic.app.ui.screens.library

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fmusic.app.data.local.entity.FavoriteTrackEntity
import com.fmusic.app.data.local.entity.OfflineTrackEntity
import com.fmusic.app.data.local.entity.PlaylistEntity
import com.fmusic.app.data.local.entity.RecentlyPlayedEntity
import com.fmusic.app.data.model.TrackItem
import com.fmusic.app.data.repository.MusicRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class LibraryUiState(
    val selectedTab: String = "Playlists",
    val playlists: List<PlaylistEntity> = emptyList(),
    val favorites: List<FavoriteTrackEntity> = emptyList(),
    val savedOffline: List<OfflineTrackEntity> = emptyList(),
    val history: List<RecentlyPlayedEntity> = emptyList(),
    val message: String? = null
)

class LibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MusicRepository(application)

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        observeData()
    }

    private fun observeData() {
        viewModelScope.launch {
            repository.getAllPlaylists().collect { list ->
                _uiState.update { it.copy(playlists = list) }
            }
        }
        viewModelScope.launch {
            repository.getAllFavorites().collect { list ->
                _uiState.update { it.copy(favorites = list) }
            }
        }
        viewModelScope.launch {
            repository.getAllOfflineTracks().collect { list ->
                _uiState.update { it.copy(savedOffline = list) }
            }
        }
        viewModelScope.launch {
            repository.getRecentlyPlayed().collect { list ->
                _uiState.update { it.copy(history = list) }
            }
        }
    }

    fun selectTab(tab: String) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun createPlaylist(name: String, description: String?) {
        viewModelScope.launch {
            repository.createPlaylist(name, description)
            _uiState.update { it.copy(message = "Playlist '$name' dibuat!") }
        }
    }

    fun deletePlaylist(id: Long) {
        viewModelScope.launch {
            repository.deletePlaylist(id)
        }
    }

    fun deleteOfflineTrack(videoId: String) {
        viewModelScope.launch {
            repository.deleteOfflineTrack(videoId)
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
