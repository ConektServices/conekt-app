package com.conekt.suite.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.conekt.suite.data.model.FileRecord
import com.conekt.suite.data.model.NoteRecord
import com.conekt.suite.data.model.PostRecord
import com.conekt.suite.data.model.ProfileRecord
import com.conekt.suite.data.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {

    // No constructor params — avoids ViewModelProvider factory issues
    private val repository = ProfileRepository()

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun refresh() = load()

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            // Fetch each independently — one failure won't block the others
            val profile: ProfileRecord? = safeGet { repository.getMyProfile() }
            val posts:   List<PostRecord>  = safeList { repository.getMyPosts() }
            val files:   List<FileRecord>  = safeList { repository.getMyFiles() }
            val notes:   List<NoteRecord>  = safeList { repository.getMyNotes() }

            _uiState.value = ProfileUiState(
                isLoading    = false,
                profile      = profile,
                posts        = posts,
                files        = files,
                notes        = notes,
                errorMessage = if (profile == null) "Could not load profile." else null
            )
        }
    }

    // Helpers — never throw, always return a safe value
    private suspend fun <T> safeGet(block: suspend () -> T): T? = try {
        block()
    } catch (e: Exception) {
        null
    }

    private suspend fun <T> safeList(block: suspend () -> List<T>): List<T> = try {
        block()
    } catch (e: Exception) {
        emptyList()
    }
}