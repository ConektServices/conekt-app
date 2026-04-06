package com.conekt.suite.feature.chat

import com.conekt.suite.data.model.PostRecord

// ── Domain models ─────────────────────────────────────────────────────────────

data class ConversationItem(
    val id:            String,
    val name:          String,
    val avatarUrl:     String?,
    val lastMessage:   String?,
    val lastMessageAt: String?,
    val unreadCount:   Int     = 0,
    val isOnline:      Boolean = false,
    val otherUserId:   String  = ""
)

data class MessageItem(
    val id:          String,
    val senderId:    String,
    val body:        String?,
    val type:        MsgType,
    val fileUrl:     String?,
    val fileName:    String?,
    val isMe:        Boolean,
    val createdAt:   String,
    val isDeleted:   Boolean = false,
    val musicTitle:  String? = null,
    val musicArtist: String? = null,
    val musicCover:  String? = null,
    val musicFile:   String? = null
)

enum class MsgType { TEXT, IMAGE, FILE, AUDIO, MUSIC, EMOJI }

data class UserSearchResult(
    val id:          String,
    val username:    String,
    val displayName: String?,
    val avatarUrl:   String?
)

data class OtherUserProfile(
    val id:             String,
    val username:       String,
    val displayName:    String?,
    val avatarUrl:      String?,
    val bannerUrl:      String?,
    val bio:            String?,
    val followerCount:  Int     = 0,
    val followingCount: Int     = 0,
    val isVerified:     Boolean = false,
    val isFollowing:    Boolean = false
)

data class StoryThumb(
    val id:         String,
    val authorId:   String,
    val authorName: String,
    val avatarUrl:  String?,
    val mediaUrl:   String,
    val caption:    String?
)

// ── UI states ─────────────────────────────────────────────────────────────────

data class ChatListState(
    val isLoading:     Boolean                = true,
    val conversations: List<ConversationItem> = emptyList(),
    val stories:       List<StoryThumb>       = emptyList(),
    val query:         String                 = "",
    val searchResults: List<UserSearchResult> = emptyList(),
    val isSearching:   Boolean                = false,
    val error:         String?                = null
)

data class ChatThreadState(
    val isLoading: Boolean           = true,
    val messages:  List<MessageItem> = emptyList(),
    val draft:     String            = "",
    val isSending: Boolean           = false,
    val showEmoji: Boolean           = false,
    val showAttach: Boolean          = false,
    val error:     String?           = null
)

// UserProfileState now includes the person's public posts so we can display them
data class UserProfileState(
    val isLoading:    Boolean           = true,
    val profile:      OtherUserProfile? = null,
    val posts:        List<PostRecord>  = emptyList(),  // ← their public posts
    val isFollowing:  Boolean           = false,
    val isDmLoading:  Boolean           = false,
    val dmConvId:     String            = "",
    val error:        String?           = null
)