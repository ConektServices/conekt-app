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

    // Track whether we've loaded at least once so we don't blank out the screen
    // on every recomposition — only do a background refresh instead.
    private var hasLoadedOnce = false

    init {
        // Load immediately when ViewModel is first created.
        // Because the ViewModel lives as long as the nav backstack entry, this
        // only runs once per session, not on every screen visit.
        load(showLoadingSpinner = true)
    }

    /**
     * Called by the UI whenever the ChatList screen becomes visible.
     * Does a silent background refresh so existing data stays visible while
     * fresh data loads — no full loading spinner after the first load.
     */
    fun refresh() {
        load(showLoadingSpinner = !hasLoadedOnce)
    }

    fun load(showLoadingSpinner: Boolean = true) {
        viewModelScope.launch {
            if (showLoadingSpinner) {
                _state.value = _state.value.copy(isLoading = true, error = null)
            }
            val convs   = safe { repo.fetchConversations() } ?: emptyList()
            val stories = safe { repo.fetchStories() }       ?: emptyList()
            _state.value = _state.value.copy(
                isLoading = false,
                conversations = convs,
                stories = stories
            )
            hasLoadedOnce = true
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

    fun deleteConversation(convId: String) {
        viewModelScope.launch {
            safe { repo.deleteConversationForMe(convId) }
            load(showLoadingSpinner = false)
        }
    }

    suspend fun openOrCreateDm(userId: String): String {
        return try {
            repo.getOrCreateDm(userId)
        } catch (e: Exception) {
            android.util.Log.e("ChatDebug", "openOrCreateDm FAILED: ${e.message}")
            _state.value = _state.value.copy(error = e.message)
            ""
        }
    }

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
        android.util.Log.d("ChatVM", "init: convId=$conversationId otherId=$otherId alreadyInit=$initialized")
        if (initialized && convId == conversationId) {
            android.util.Log.d("ChatVM", "Already initialized for this conversation, skipping")
            return
        }
        initialized  = true
        convId       = conversationId
        otherUserId  = otherId
        pollJob?.cancel()
        loadMessages()
        startPolling()
        viewModelScope.launch { safe { repo.markAsRead(conversationId) } }
    }

    private fun loadMessages() {
        if (convId.isBlank()) {
            android.util.Log.e("ChatVM", "loadMessages called with blank convId!")
            return
        }
        android.util.Log.d("ChatVM", "loadMessages: convId=$convId otherUserId=$otherUserId")
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val msgs = safe { repo.fetchMessages(convId, otherUserId) } ?: emptyList()
            android.util.Log.d("ChatVM", "loadMessages done: ${msgs.size} messages")
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

    // ── Draft ─────────────────────────────────────────────────────────────────

    fun onDraftChange(text: String) { _state.value = _state.value.copy(draft = text) }
    fun toggleEmoji()  { _state.value = _state.value.copy(showEmoji = !_state.value.showEmoji, showAttach = false, showPhotoPicker = false) }
    fun toggleAttach() { _state.value = _state.value.copy(showAttach = !_state.value.showAttach, showEmoji = false, showPhotoPicker = false) }
    fun togglePhotoPicker() { _state.value = _state.value.copy(showPhotoPicker = !_state.value.showPhotoPicker, showAttach = false, showEmoji = false) }
    fun appendEmoji(emoji: String) { _state.value = _state.value.copy(draft = _state.value.draft + emoji, showEmoji = false) }

    // ── Reply ─────────────────────────────────────────────────────────────────

    fun setReply(msg: MessageItem, myName: String) {
        val preview = when (msg.type) {
            MsgType.IMAGE -> "📷 Photo"
            MsgType.AUDIO -> "🎵 Audio"
            MsgType.FILE  -> "📎 ${msg.fileName ?: "File"}"
            MsgType.MUSIC -> "🎵 ${msg.musicTitle ?: "Track"}"
            else          -> msg.body?.take(60) ?: ""
        }
        _state.value = _state.value.copy(
            replyingTo = ReplyPreview(
                messageId  = msg.id,
                senderName = if (msg.isMe) myName else "them",
                preview    = preview,
                isMe       = msg.isMe
            ),
            contextMessage = null
        )
    }

    fun clearReply() { _state.value = _state.value.copy(replyingTo = null) }

    // ── Context menu (long press) ─────────────────────────────────────────────

    fun showContext(msg: MessageItem) { _state.value = _state.value.copy(contextMessage = msg) }
    fun dismissContext()              { _state.value = _state.value.copy(contextMessage = null) }

    // ── Image viewer ──────────────────────────────────────────────────────────

    fun openImageViewer(images: List<String>, startIndex: Int) {
        _state.value = _state.value.copy(viewerImages = images, viewerIndex = startIndex, showImageViewer = true)
    }
    fun closeImageViewer() { _state.value = _state.value.copy(showImageViewer = false) }

    // ── Send ──────────────────────────────────────────────────────────────────

    fun sendText() {
        val text = _state.value.draft.trim()
        if (text.isBlank() || convId.isBlank()) return
        val reply = _state.value.replyingTo

        // Optimistic: clear draft immediately so the user sees a responsive UI
        _state.value = _state.value.copy(draft = "", isSending = true, showEmoji = false, replyingTo = null)

        viewModelScope.launch {
            safe { repo.sendText(convId, text, otherUserId, reply) }
            // Immediately refresh after sending so the sent message appears
            refreshMessages()
            _state.value = _state.value.copy(isSending = false)
        }
    }

    fun sendFile(context: Context, uri: Uri) {
        if (convId.isBlank()) return
        _state.value = _state.value.copy(isSending = true, showAttach = false, showPhotoPicker = false)
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

    // ── Delete ────────────────────────────────────────────────────────────────

    fun deleteMessageForEveryone(msgId: String) {
        _state.value = _state.value.copy(contextMessage = null)
        viewModelScope.launch {
            safe { repo.deleteMessageForEveryone(msgId) }
            refreshMessages()
        }
    }

    fun deleteMessageForMe(msgId: String) {
        _state.value = _state.value.copy(
            contextMessage = null,
            messages = _state.value.messages.filter { it.id != msgId }
        )
        viewModelScope.launch { safe { repo.deleteMessageForMe(msgId) } }
    }

    // ── React ─────────────────────────────────────────────────────────────────

    fun react(msg: MessageItem, emoji: String) {
        _state.value = _state.value.copy(contextMessage = null)
        viewModelScope.launch {
            safe { repo.addReaction(msg.id, emoji, msg.reactions) }
            refreshMessages()
        }
    }

    // ── Block ─────────────────────────────────────────────────────────────────

    fun blockUser(convId: String, block: Boolean) {
        viewModelScope.launch { safe { repo.blockUser(convId, block) } }
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

    fun load(userId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val profile = safe { chatRepo.fetchProfile(userId) }
            val posts   = safe { profileRepo.getPublicProfilePosts(userId) } ?: emptyList()
            _state.value = _state.value.copy(isLoading = false, profile = profile, posts = posts, isFollowing = profile?.isFollowing ?: false, error = if (profile == null) "Could not load profile." else null)
        }
    }

    fun toggleFollow(userId: String) {
        val wasFollowing = _state.value.isFollowing
        _state.value = _state.value.copy(isFollowing = !wasFollowing, profile = _state.value.profile?.copy(followerCount = _state.value.profile!!.followerCount + if (wasFollowing) -1 else 1))
        viewModelScope.launch { if (wasFollowing) safe { chatRepo.unfollow(userId) } else safe { chatRepo.follow(userId) } }
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