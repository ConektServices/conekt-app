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

// ── Chat list ViewModel ───────────────────────────────────────────────────────

class ChatListViewModel(
    private val repo: ChatRepository = ChatRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(ChatListUiState())
    val state: StateFlow<ChatListUiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val convs   = safe { repo.fetchConversations() } ?: emptyList()
            val stories = safe { repo.fetchStories() }       ?: emptyList()
            _state.value = _state.value.copy(isLoading = false, conversations = convs, stories = stories)
        }
    }

    fun onSearchQueryChange(q: String) {
        _state.value = _state.value.copy(searchQuery = q)
        if (q.isBlank()) { _state.value = _state.value.copy(searchResults = emptyList()); return }
        viewModelScope.launch {
            val results = safe { repo.searchUsers(q) } ?: emptyList()
            _state.value = _state.value.copy(searchResults = results)
        }
    }

    fun clearSearch() = _state.value.copy(searchQuery = "", searchResults = emptyList()).also { _state.value = it }

    private suspend fun <T> safe(block: suspend () -> T): T? = try { block() } catch (_: Exception) { null }
}

// ── Chat thread ViewModel ─────────────────────────────────────────────────────

class ChatThreadViewModel(
    private val repo: ChatRepository = ChatRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(ChatThreadUiState())
    val state: StateFlow<ChatThreadUiState> = _state.asStateFlow()

    private var conversationId: String = ""
    private var otherUserId:    String = ""
    private var pollJob:        Job?   = null

    fun init(convId: String, otherId: String) {
        conversationId = convId
        otherUserId    = otherId
        loadMessages()
        startPolling()
    }

    /** Open a DM with a user — creates conversation if needed */
    suspend fun openDirectWith(userId: String): String {
        return try { repo.getOrCreateDirectConversation(userId) } catch (_: Exception) { "" }
    }

    private fun loadMessages() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val messages = safe { repo.fetchMessages(conversationId, otherUserId) } ?: emptyList()
            _state.value = _state.value.copy(isLoading = false, messages = messages)
        }
    }

    private fun startPolling() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (true) {
                delay(3_000)
                val messages = safe { repo.fetchMessages(conversationId, otherUserId) } ?: continue
                _state.value = _state.value.copy(messages = messages)
            }
        }
    }

    fun onDraftChange(text: String) {
        _state.value = _state.value.copy(draft = text)
    }

    fun sendText() {
        val text = _state.value.draft.trim()
        if (text.isBlank()) return
        _state.value = _state.value.copy(draft = "", isSending = true)
        viewModelScope.launch {
            safe { repo.sendTextMessage(conversationId, text, otherUserId) }
            loadMessages()
            _state.value = _state.value.copy(isSending = false)
        }
    }

    fun sendFile(context: Context, uri: Uri) {
        _state.value = _state.value.copy(isSending = true)
        viewModelScope.launch {
            safe { repo.sendFileMessage(context, conversationId, uri, otherUserId) }
            loadMessages()
            _state.value = _state.value.copy(isSending = false)
        }
    }

    fun sendMusic(trackId: String, title: String, artist: String, coverUrl: String?, fileUrl: String) {
        viewModelScope.launch {
            safe { repo.sendMusicMessage(conversationId, trackId, title, artist, coverUrl, fileUrl) }
            loadMessages()
        }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            safe { repo.deleteMessage(messageId) }
            loadMessages()
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollJob?.cancel()
    }

    private suspend fun <T> safe(block: suspend () -> T): T? = try { block() } catch (_: Exception) { null }
}

// ── User profile ViewModel ────────────────────────────────────────────────────

class UserProfileViewModel(
    private val repo: ChatRepository = ChatRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(UserProfileUiState())
    val state: StateFlow<UserProfileUiState> = _state.asStateFlow()

    fun load(userId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val profile = safe { repo.fetchUserProfile(userId) }
            _state.value = _state.value.copy(isLoading = false, profile = profile, isFollowing = profile?.isFollowing ?: false)
        }
    }

    fun toggleFollow(userId: String) {
        val currently = _state.value.isFollowing
        _state.value = _state.value.copy(isFollowing = !currently)
        viewModelScope.launch {
            if (currently) safe { repo.unfollowUser(userId) }
            else           safe { repo.followUser(userId) }
        }
    }

    private suspend fun <T> safe(block: suspend () -> T): T? = try { block() } catch (_: Exception) { null }
}
