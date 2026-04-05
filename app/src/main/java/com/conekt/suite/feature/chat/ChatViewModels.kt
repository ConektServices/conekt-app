package com.conekt.suite.feature.chat

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.conekt.suite.data.repository.ChatRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// Chat list
// ─────────────────────────────────────────────────────────────────────────────

class ChatListViewModel(
    private val repo: ChatRepository = ChatRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(ChatListUiState())
    val state: StateFlow<ChatListUiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val convs   = safe { repo.fetchConversations() } ?: emptyList()
            val stories = safe { repo.fetchStories() }       ?: emptyList()
            _state.value = _state.value.copy(isLoading = false, conversations = convs, stories = stories)
        }
    }

    fun onSearchQueryChange(q: String) {
        _state.value = _state.value.copy(searchQuery = q, searchResults = emptyList())
        if (q.isBlank()) return
        viewModelScope.launch {
            _state.value = _state.value.copy(isSearching = true)
            val results = safe { repo.searchUsers(q) } ?: emptyList()
            _state.value = _state.value.copy(isSearching = false, searchResults = results)
        }
    }

    fun clearSearch() {
        _state.value = _state.value.copy(searchQuery = "", searchResults = emptyList())
    }

    /** Returns the conversation ID for a direct chat with userId (creating if needed). */
    suspend fun openOrCreateDm(userId: String): String =
        safe { repo.getOrCreateDirectConversation(userId) } ?: ""

    private suspend fun <T> safe(block: suspend () -> T): T? =
        runCatching { block() }.getOrNull()
}

// ─────────────────────────────────────────────────────────────────────────────
// Chat thread
// ─────────────────────────────────────────────────────────────────────────────

class ChatThreadViewModel(
    private val repo: ChatRepository = ChatRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(ChatThreadUiState())
    val state: StateFlow<ChatThreadUiState> = _state.asStateFlow()

    private var convId:      String = ""
    private var otherUserId: String = ""
    private var pollJob:     Job?   = null

    fun init(conversationId: String, otherId: String) {
        convId      = conversationId
        otherUserId = otherId
        loadMessages()
        startPolling()
    }

    private fun loadMessages() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val msgs = safe { repo.fetchMessages(convId, otherUserId) } ?: emptyList()
            _state.value = _state.value.copy(isLoading = false, messages = msgs)
        }
    }

    private fun startPolling() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (true) {
                delay(3_000)
                val msgs = safe { repo.fetchMessages(convId, otherUserId) } ?: continue
                _state.value = _state.value.copy(messages = msgs)
            }
        }
    }

    fun onDraftChange(text: String) {
        _state.value = _state.value.copy(draft = text)
    }

    fun sendText() {
        val text = _state.value.draft.trim()
        if (text.isBlank() || convId.isBlank()) return
        _state.value = _state.value.copy(draft = "", isSending = true)
        viewModelScope.launch {
            safe { repo.sendText(convId, text, otherUserId) }
            val msgs = safe { repo.fetchMessages(convId, otherUserId) } ?: emptyList()
            _state.value = _state.value.copy(isSending = false, messages = msgs)
        }
    }

    fun sendFile(context: Context, uri: Uri) {
        if (convId.isBlank()) return
        _state.value = _state.value.copy(isSending = true)
        viewModelScope.launch {
            safe { repo.sendFile(context, convId, uri, otherUserId) }
            val msgs = safe { repo.fetchMessages(convId, otherUserId) } ?: emptyList()
            _state.value = _state.value.copy(isSending = false, messages = msgs)
        }
    }

    fun sendMusicTrack(trackId: String, title: String, artist: String, coverUrl: String?, fileUrl: String) {
        if (convId.isBlank()) return
        viewModelScope.launch {
            safe { repo.sendMusicTrack(convId, trackId, title, artist, coverUrl, fileUrl) }
            val msgs = safe { repo.fetchMessages(convId, otherUserId) } ?: emptyList()
            _state.value = _state.value.copy(messages = msgs)
        }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            safe { repo.deleteMessage(messageId) }
            val msgs = safe { repo.fetchMessages(convId, otherUserId) } ?: emptyList()
            _state.value = _state.value.copy(messages = msgs)
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollJob?.cancel()
    }

    private suspend fun <T> safe(block: suspend () -> T): T? =
        runCatching { block() }.getOrNull()
}

// ─────────────────────────────────────────────────────────────────────────────
// User profile
// ─────────────────────────────────────────────────────────────────────────────

class UserProfileViewModel(
    private val repo: ChatRepository = ChatRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(UserProfileUiState())
    val state: StateFlow<UserProfileUiState> = _state.asStateFlow()

    fun load(userId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val p = safe { repo.fetchUserProfile(userId) }
            _state.value = _state.value.copy(isLoading = false, profile = p, isFollowing = p?.isFollowing ?: false)
        }
    }

    fun toggleFollow(userId: String) {
        val was = _state.value.isFollowing
        // Optimistic update
        _state.value = _state.value.copy(isFollowing = !was)
        viewModelScope.launch {
            if (was) safe { repo.unfollowUser(userId) }
            else     safe { repo.followUser(userId) }
        }
    }

    suspend fun getOrCreateDm(userId: String): String =
        safe { repo.getOrCreateDirectConversation(userId) } ?: ""

    private suspend fun <T> safe(block: suspend () -> T): T? =
        runCatching { block() }.getOrNull()
}