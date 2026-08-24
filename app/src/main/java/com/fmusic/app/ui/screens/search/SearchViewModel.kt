package com.fmusic.app.ui.screens.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fmusic.app.data.local.entity.RecentlyPlayedEntity
import com.fmusic.app.data.local.entity.SearchHistoryEntity
import com.fmusic.app.data.model.MoodCategory
import com.fmusic.app.data.model.SectionItem
import com.fmusic.app.data.model.TrackItem
import com.fmusic.app.data.repository.MusicRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val selectedFilter: String? = null,
    val isSearching: Boolean = false,
    val searchResults: List<SectionItem> = emptyList(),
    val suggestions: List<String> = emptyList(),
    val categories: List<MoodCategory> = emptyList(),
    val recentSearches: List<SearchHistoryEntity> = emptyList(),
    val recentlyPlayed: List<RecentlyPlayedEntity> = emptyList(),
    val errorMessage: String? = null
)

class SearchViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MusicRepository(application)

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadInitialData()
        observeLocalHistory()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            val moodResult = repository.getMoods()
            moodResult.onSuccess { response ->
                _uiState.update { it.copy(categories = response.categories ?: emptyList()) }
            }
        }
    }

    private fun observeLocalHistory() {
        viewModelScope.launch {
            repository.getSearchHistory().collect { history ->
                _uiState.update { it.copy(recentSearches = history) }
            }
        }
        viewModelScope.launch {
            repository.getRecentlyPlayed().collect { played ->
                _uiState.update { it.copy(recentlyPlayed = played) }
            }
        }
    }

    fun onQueryChange(newQuery: String) {
        _uiState.update { it.copy(query = newQuery) }
        searchJob?.cancel()

        if (newQuery.isBlank()) {
            _uiState.update { it.copy(searchResults = emptyList(), suggestions = emptyList(), isSearching = false) }
            return
        }

        searchJob = viewModelScope.launch {
            delay(300) // Debounce
            val suggs = repository.getSuggestions(newQuery)
            _uiState.update { it.copy(suggestions = suggs) }

            performSearch(newQuery, _uiState.value.selectedFilter)
        }
    }

    fun onFilterSelect(filter: String?) {
        val nextFilter = if (_uiState.value.selectedFilter == filter) null else filter
        _uiState.update { it.copy(selectedFilter = nextFilter) }
        if (_uiState.value.query.isNotBlank()) {
            performSearch(_uiState.value.query, nextFilter)
        }
    }

    fun performSearch(query: String, filter: String? = null) {
        if (query.isBlank()) return
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true, errorMessage = null) }
            val filterParam = when (filter) {
                "Songs" -> "songs"
                "Videos" -> "videos"
                "Albums" -> "albums"
                "Artists" -> "artists"
                "Playlists" -> "playlists"
                else -> null
            }
            val res = repository.search(query, filterParam)
            res.onSuccess { response ->
                _uiState.update {
                    it.copy(
                        isSearching = false,
                        searchResults = response.sections ?: emptyList(),
                        suggestions = emptyList()
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isSearching = false,
                        errorMessage = error.message ?: "Failed to find results"
                    )
                }
            }
        }
    }

    fun deleteRecentSearch(query: String) {
        viewModelScope.launch {
            repository.deleteSearchHistory(query)
        }
    }

    fun clearAllRecentSearches() {
        viewModelScope.launch {
            repository.clearSearchHistory()
        }
    }

    fun deleteRecentPlayed(videoId: String) {
        viewModelScope.launch {
            repository.deleteRecentlyPlayed(videoId)
        }
    }

    fun clearQuery() {
        _uiState.update { it.copy(query = "", searchResults = emptyList(), suggestions = emptyList(), isSearching = false) }
    }
}
