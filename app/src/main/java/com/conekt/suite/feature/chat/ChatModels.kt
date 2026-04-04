package com.conekt.suite.feature.chat

import android.net.Uri

// ── Domain models ─────────────────────────────────────────────────────────────

data class ConversationItem(
    val id:            String,
    val type:          String,          // "direct" | "group"
    val name:          String?,
    val avatarUrl:     String?,
    val lastMessage:   String?,
    val lastMessageAt: String?,
    val unreadCount:   Int = 0,
    val isOnline:      Boolean = false,
    val otherUserId:   String? = null   // for direct conversations
)

data class MessageItem(
    val id:           String,
    val conversationId: String,
    val senderId:     String,
    val body:         String?,           // decrypted text
    val messageType:  String,            // "text"|"image"|"file"|"audio"|"music"
    val fileUrl:      String?,
    val fileName:     String?,
    val isMe:         Boolean,
    val createdAt:    String,
    val isDeleted:    Boolean = false,
    // For shared music tracks
    val musicTrackId:    String? = null,
    val musicTrackTitle: String? = null,
    val musicArtist:     String? = null,
    val musicCoverUrl:   String? = null,
    val musicFileUrl:    String? = null
)

data class UserProfile(
    val id:           String,
    val username:     String,
    val displayName:  String?,
    val avatarUrl:    String?,
    val bannerUrl:    String?,
    val bio:          String?,
    val isVerified:   Boolean = false,
    val isPrivate:    Boolean = false,
    val followerCount: Int    = 0,
    val followingCount: Int   = 0,
    val isFollowing:  Boolean = false,
    val isFollowedBy: Boolean = false
)

data class StoryItem(
    val id:         String,
    val authorId:   String,
    val authorName: String,
    val avatarUrl:  String?,
    val mediaUrl:   String,
    val mediaType:  String,
    val caption:    String?,
    val expiresAt:  String,
    val isViewed:   Boolean = false
)

// ── UI state ──────────────────────────────────────────────────────────────────

data class ChatListUiState(
    val isLoading:       Boolean                   = true,
    val conversations:   List<ConversationItem>    = emptyList(),
    val stories:         List<StoryItem>           = emptyList(),
    val searchQuery:     String                    = "",
    val searchResults:   List<UserProfile>         = emptyList(),
    val errorMessage:    String?                   = null
)

data class ChatThreadUiState(
    val isLoading:       Boolean              = true,
    val conversation:    ConversationItem?    = null,
    val messages:        List<MessageItem>    = emptyList(),
    val draft:           String               = "",
    val isSending:       Boolean              = false,
    val pendingMedia:    Uri?                 = null,
    val errorMessage:    String?              = null
)

data class UserProfileUiState(
    val isLoading:   Boolean       = true,
    val profile:     UserProfile?  = null,
    val posts:       List<com.conekt.suite.data.model.PostRecord> = emptyList(),
    val isFollowing: Boolean       = false,
    val errorMessage: String?      = null
)
