package com.conekt.suite.feature.pulse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.conekt.suite.data.repository.PulseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PulseViewModel(
    private val repository: PulseRepository = PulseRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(PulseUiState())
    val uiState: StateFlow<PulseUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )

            runCatching {
                repository.fetchHomeData()
            }.onSuccess { home ->
                _uiState.value = PulseUiState(
                    isLoading = false,
                    stories = home.stories,
                    posts = home.posts,
                    recentFiles = home.recentFiles,
                    recentNotes = home.recentNotes
                )
            }.onFailure { throwable ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = throwable.message ?: "Failed to load Pulse"
                )
            }
        }
    }
}