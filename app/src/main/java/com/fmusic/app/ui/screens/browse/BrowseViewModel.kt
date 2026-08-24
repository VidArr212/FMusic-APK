package com.fmusic.app.ui.screens.browse

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fmusic.app.data.model.BrowseHeader
import com.fmusic.app.data.model.SectionItem
import com.fmusic.app.data.model.TrackItem
import com.fmusic.app.data.repository.MusicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BrowseUiState(
    val isLoading: Boolean = true,
    val header: BrowseHeader? = null,
    val tracks: List<TrackItem> = emptyList(),
    val sections: List<SectionItem> = emptyList(),
    val errorMessage: String? = null
)

class BrowseViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MusicRepository(application)

    private val _uiState = MutableStateFlow(BrowseUiState())
    val uiState: StateFlow<BrowseUiState> = _uiState.asStateFlow()

    fun loadBrowse(browseId: String, params: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val res = repository.browse(browseId, params)
            res.onSuccess { response ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        header = response.header,
                        tracks = response.tracks ?: emptyList(),
                        sections = response.sections ?: emptyList()
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Failed to load album details"
                    )
                }
            }
        }
    }
}
