package com.fmusic.app.ui.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fmusic.app.data.model.SectionItem
import com.fmusic.app.data.model.TrackItem
import com.fmusic.app.data.repository.MusicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = true,
    val sections: List<SectionItem> = emptyList(),
    val errorMessage: String? = null,
    val isRefreshing: Boolean = false
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MusicRepository(application)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHomeData()
    }

    fun loadHomeData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val result = repository.getHome()
            result.onSuccess { response ->
                val sections = response.sections ?: emptyList()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        sections = sections,
                        errorMessage = if (sections.isEmpty()) "No content found. Ensure API proxy is running." else null
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Failed to connect to music server"
                    )
                }
            }
        }
    }
}
