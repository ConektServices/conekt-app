package com.conekt.suite.feature.pulse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.conekt.suite.core.supabase.SupabaseProvider
import com.conekt.suite.data.repository.PulseRepository
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class PostIdParam(@SerialName("post_id") val postId: String)

class PulseViewModel(
    private val repository: PulseRepository = PulseRepository()
) : ViewModel() {

    private val supabase = SupabaseProvider.client

    private val _uiState = MutableStateFlow(PulseUiState())
    val uiState: StateFlow<PulseUiState> = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            runCatching { repository.fetchHomeData() }
                .onSuccess { home ->
                    _uiState.value = PulseUiState(
                        isLoading   = false,
                        stories     = home.stories,
                        posts       = home.posts,
                        recentFiles = home.recentFiles,
                        recentNotes = home.recentNotes
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading    = false,
                        errorMessage = e.message ?: "Failed to load"
                    )
                }
        }
    }

    /** Like a post — optimistic update + DB RPC */
    fun likePost(postId: String) {
        // Optimistic update so the UI feels instant
        _uiState.value = _uiState.value.copy(
            posts = _uiState.value.posts.map { p ->
                if (p.id == postId) p.copy(likeCount = p.likeCount + 1) else p
            }
        )
        viewModelScope.launch {
            runCatching {
                supabase.postgrest.rpc("increment_post_like", PostIdParam(postId))
            }
        }
    }

    /** Repost — optimistic update + DB RPC */
    fun repostPost(postId: String) {
        _uiState.value = _uiState.value.copy(
            posts = _uiState.value.posts.map { p ->
                if (p.id == postId) p.copy(shareCount = p.shareCount + 1) else p
            }
        )
        viewModelScope.launch {
            runCatching {
                supabase.postgrest.rpc("increment_post_share", PostIdParam(postId))
            }
        }
    }
}