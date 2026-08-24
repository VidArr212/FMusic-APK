package com.fmusic.app.ui.screens.charts

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fmusic.app.data.model.SectionItem
import com.fmusic.app.data.repository.MusicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChartsUiState(
    val isLoading: Boolean = true,
    val sections: List<SectionItem> = emptyList(),
    val errorMessage: String? = null
)

class ChartsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MusicRepository(application)

    private val _uiState = MutableStateFlow(ChartsUiState())
    val uiState: StateFlow<ChartsUiState> = _uiState.asStateFlow()

    init {
        loadCharts()
    }

    fun loadCharts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = repository.getCharts()
            result.onSuccess { response ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        sections = response.sections ?: emptyList()
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Failed to load charts"
                    )
                }
            }
        }
    }
}
