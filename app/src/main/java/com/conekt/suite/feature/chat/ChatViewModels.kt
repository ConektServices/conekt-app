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

// ── Chat list ─────────────────────────────────────────────────────────────────

class ChatListViewModel(private val repo: ChatRepository = ChatRepository()) : ViewModel() {

    private val _state = MutableStateFlow(ChatListState())
    val state: StateFlow<ChatListState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val convs   = safe { repo.fetchConversations() } ?: emptyList()
            val stories = safe { repo.fetchStories() }       ?: emptyList()
            _state.value = _state.value.copy(isLoading = false, conversations = convs, stories = stories)
        }
    }

    fun onQueryChange(q: String) {
        _state.value = _state.value.copy(query = q, searchResults = emptyList())
        if (q.isBlank()) return
        viewModelScope.launch {
            _state.value = _state.value.copy(isSearching = true)
            val results = safe { repo.searchUsers(q) } ?: emptyList()
            _state.value = _state.value.copy(isSearching = false, searchResults = results)
        }
    }

    fun clearQuery() {
        _state.value = _state.value.copy(query = "", searchResults = emptyList(), isSearching = false)
    }

    /** Open or create a DM with userId. Returns the conversation ID. */
    suspend fun openOrCreateDm(userId: String): String =
        safe { repo.getOrCreateDm(userId) } ?: ""

    private suspend fun <T> safe(block: suspend () -> T): T? = runCatching { block() }.getOrNull()
}

// ── Chat thread ───────────────────────────────────────────────────────────────

class ChatThreadViewModel(private val repo: ChatRepository = ChatRepository()) : ViewModel() {

    private val _state = MutableStateFlow(ChatThreadState())
    val state: StateFlow<ChatThreadState> = _state.asStateFlow()

    private var convId       = ""
    private var otherUserId  = ""
    private var pollJob: Job? = null
    private var initialized  = false

    /** Call once with the conversation and other-user IDs. Safe to call multiple times. */
    fun init(conversationId: String, otherId: String) {
        if (initialized && convId == conversationId) return   // already set up for this conv
        initialized  = true
        convId       = conversationId
        otherUserId  = otherId
        pollJob?.cancel()
        loadMessages()
        startPolling()
    }

    private fun loadMessages() {
        if (convId.isBlank()) return
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val msgs = safe { repo.fetchMessages(convId, otherUserId) } ?: emptyList()
            _state.value = _state.value.copy(isLoading = false, messages = msgs)
        }
    }

    private fun startPolling() {
        pollJob = viewModelScope.launch {
            while (true) {
                delay(3_000)
                if (convId.isBlank()) continue
                val msgs = safe { repo.fetchMessages(convId, otherUserId) } ?: continue
                _state.value = _state.value.copy(messages = msgs)
            }
        }
    }

    // ── Draft / input ─────────────────────────────────────────────────────────

    fun onDraftChange(text: String) {
        _state.value = _state.value.copy(draft = text)
    }

    fun toggleEmoji() {
        _state.value = _state.value.copy(showEmoji = !_state.value.showEmoji, showAttach = false)
    }

    fun toggleAttach() {
        _state.value = _state.value.copy(showAttach = !_state.value.showAttach, showEmoji = false)
    }

    fun appendEmoji(emoji: String) {
        _state.value = _state.value.copy(draft = _state.value.draft + emoji, showEmoji = false)
    }

    // ── Send actions ──────────────────────────────────────────────────────────

    fun sendText() {
        val text = _state.value.draft.trim()
        if (text.isBlank() || convId.isBlank()) return
        _state.value = _state.value.copy(draft = "", isSending = true, showEmoji = false)
        viewModelScope.launch {
            safe { repo.sendText(convId, text, otherUserId) }
            refresh()
            _state.value = _state.value.copy(isSending = false)
        }
    }

    fun sendFile(context: Context, uri: Uri) {
        if (convId.isBlank()) return
        _state.value = _state.value.copy(isSending = true, showAttach = false)
        viewModelScope.launch {
            safe { repo.sendFile(context, convId, uri, otherUserId) }
            refresh()
            _state.value = _state.value.copy(isSending = false)
        }
    }

    fun sendMusic(trackId: String, title: String, artist: String, coverUrl: String?, fileUrl: String) {
        if (convId.isBlank()) return
        _state.value = _state.value.copy(showAttach = false)
        viewModelScope.launch {
            safe { repo.sendMusic(convId, trackId, title, artist, coverUrl, fileUrl) }
            refresh()
        }
    }

    fun deleteMessage(msgId: String) {
        viewModelScope.launch {
            safe { repo.deleteMessage(msgId) }
            refresh()
        }
    }

    private suspend fun refresh() {
        val msgs = safe { repo.fetchMessages(convId, otherUserId) } ?: return
        _state.value = _state.value.copy(messages = msgs)
    }

    override fun onCleared() {
        super.onCleared()
        pollJob?.cancel()
    }

    private suspend fun <T> safe(block: suspend () -> T): T? = runCatching { block() }.getOrNull()
}

// ── User profile ──────────────────────────────────────────────────────────────

class UserProfileViewModel(private val repo: ChatRepository = ChatRepository()) : ViewModel() {

    private val _state = MutableStateFlow(UserProfileState())
    val state: StateFlow<UserProfileState> = _state.asStateFlow()

    fun load(userId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val p = safe { repo.fetchProfile(userId) }
            _state.value = _state.value.copy(isLoading = false, profile = p, isFollowing = p?.isFollowing ?: false)
        }
    }

    fun toggleFollow(userId: String) {
        val was = _state.value.isFollowing
        _state.value = _state.value.copy(isFollowing = !was)
        viewModelScope.launch {
            if (was) safe { repo.unfollow(userId) } else safe { repo.follow(userId) }
        }
    }

    fun startDm(userId: String, onReady: (convId: String) -> Unit) {
        _state.value = _state.value.copy(isDmLoading = true)
        viewModelScope.launch {
            val convId = safe { repo.getOrCreateDm(userId) } ?: ""
            _state.value = _state.value.copy(isDmLoading = false, dmConvId = convId)
            if (convId.isNotBlank()) onReady(convId)
        }
    }

    private suspend fun <T> safe(block: suspend () -> T): T? = runCatching { block() }.getOrNull()
}