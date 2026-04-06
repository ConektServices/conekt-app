package com.conekt.suite.feature.chat

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.conekt.suite.data.repository.ChatRepository
import com.conekt.suite.data.repository.ProfileRepository
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

    /** Creates or opens a DM conversation. Returns the conversation ID, or "" on failure. */
    suspend fun openOrCreateDm(userId: String): String =
        safe { repo.getOrCreateDm(userId) } ?: ""

    private suspend fun <T> safe(block: suspend () -> T): T? = runCatching { block() }.getOrNull()
}

// ── Chat thread ───────────────────────────────────────────────────────────────

class ChatThreadViewModel(private val repo: ChatRepository = ChatRepository()) : ViewModel() {

    private val _state = MutableStateFlow(ChatThreadState())
    val state: StateFlow<ChatThreadState> = _state.asStateFlow()

    private var convId      = ""
    private var otherUserId = ""
    private var pollJob: Job? = null
    private var initialized  = false

    fun init(conversationId: String, otherId: String) {
        // If already initialized for this conversation, skip re-init
        if (initialized && convId == conversationId) return
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
        pollJob?.cancel()
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

    // ── Send ──────────────────────────────────────────────────────────────────

    fun sendText() {
        val text = _state.value.draft.trim()
        if (text.isBlank() || convId.isBlank()) return
        _state.value = _state.value.copy(draft = "", isSending = true, showEmoji = false)
        viewModelScope.launch {
            safe { repo.sendText(convId, text, otherUserId) }
            refreshMessages()
            _state.value = _state.value.copy(isSending = false)
        }
    }

    fun sendFile(context: Context, uri: Uri) {
        if (convId.isBlank()) return
        _state.value = _state.value.copy(isSending = true, showAttach = false)
        viewModelScope.launch {
            safe { repo.sendFile(context, convId, uri, otherUserId) }
            refreshMessages()
            _state.value = _state.value.copy(isSending = false)
        }
    }

    fun sendMusic(trackId: String, title: String, artist: String, coverUrl: String?, fileUrl: String) {
        if (convId.isBlank()) return
        _state.value = _state.value.copy(showAttach = false)
        viewModelScope.launch {
            safe { repo.sendMusic(convId, trackId, title, artist, coverUrl, fileUrl) }
            refreshMessages()
        }
    }

    fun deleteMessage(msgId: String) {
        viewModelScope.launch {
            safe { repo.deleteMessage(msgId) }
            refreshMessages()
        }
    }

    private suspend fun refreshMessages() {
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

class UserProfileViewModel(
    private val chatRepo:    ChatRepository    = ChatRepository(),
    private val profileRepo: ProfileRepository = ProfileRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(UserProfileState())
    val state: StateFlow<UserProfileState> = _state.asStateFlow()

    /** Load a user's profile AND their public posts */
    fun load(userId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            // Load profile info (includes follower/following counts, isVerified, etc.)
            val profile = safe { chatRepo.fetchProfile(userId) }

            // Load their public posts so we can display them
            val posts = safe { profileRepo.getPublicProfilePosts(userId) } ?: emptyList()

            _state.value = _state.value.copy(
                isLoading   = false,
                profile     = profile,
                posts       = posts,
                isFollowing = profile?.isFollowing ?: false,
                error       = if (profile == null) "Could not load profile." else null
            )
        }
    }

    fun toggleFollow(userId: String) {
        val wasFollowing = _state.value.isFollowing
        // Optimistic update
        val currentProfile = _state.value.profile
        _state.value = _state.value.copy(
            isFollowing = !wasFollowing,
            profile = currentProfile?.copy(
                followerCount = if (wasFollowing) currentProfile.followerCount - 1
                else currentProfile.followerCount + 1
            )
        )
        viewModelScope.launch {
            if (wasFollowing) safe { chatRepo.unfollow(userId) }
            else safe { chatRepo.follow(userId) }
        }
    }

    fun startDm(userId: String, onReady: (convId: String) -> Unit) {
        _state.value = _state.value.copy(isDmLoading = true)
        viewModelScope.launch {
            val convId = safe { chatRepo.getOrCreateDm(userId) } ?: ""
            _state.value = _state.value.copy(isDmLoading = false, dmConvId = convId)
            if (convId.isNotBlank()) onReady(convId)
        }
    }

    private suspend fun <T> safe(block: suspend () -> T): T? = runCatching { block() }.getOrNull()
}