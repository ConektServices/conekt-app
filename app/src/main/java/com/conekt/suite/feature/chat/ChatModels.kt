package com.conekt.suite.feature.chat

import android.net.Uri

// ── Domain models ─────────────────────────────────────────────────────────────

data class ConversationItem(
    val id:            String,
    val type:          String      = "direct",
    val name:          String?,
    val avatarUrl:     String?,
    val lastMessage:   String?,
    val lastMessageAt: String?,
    val unreadCount:   Int         = 0,
    val isOnline:      Boolean     = false,
    val otherUserId:   String      = ""
)

data class MessageItem(
    val id:             String,
    val conversationId: String,
    val senderId:       String,
    val body:           String?,
    val messageType:    String,         // "text"|"image"|"file"|"audio"|"music"
    val fileUrl:        String?,
    val fileName:       String?,
    val isMe:           Boolean,
    val createdAt:      String,
    val isDeleted:      Boolean = false
)

data class UserSearchResult(
    val id:          String,
    val username:    String,
    val displayName: String?,
    val avatarUrl:   String?,
    val bio:         String?
)

data class FullUserProfile(
    val id:            String,
    val username:      String,
    val displayName:   String?,
    val avatarUrl:     String?,
    val bannerUrl:     String?,
    val bio:           String?,
    val isVerified:    Boolean = false,
    val isPrivate:     Boolean = false,
    val followerCount: Int     = 0,
    val followingCount: Int    = 0,
    val isFollowing:   Boolean = false
)

data class StoryPreview(
    val id:         String,
    val authorId:   String,
    val authorName: String,
    val avatarUrl:  String?,
    val mediaUrl:   String,
    val caption:    String?
)

// ── UI states ─────────────────────────────────────────────────────────────────

data class ChatListUiState(
    val isLoading:     Boolean                 = true,
    val conversations: List<ConversationItem>  = emptyList(),
    val stories:       List<StoryPreview>      = emptyList(),
    val searchQuery:   String                  = "",
    val searchResults: List<UserSearchResult>  = emptyList(),
    val isSearching:   Boolean                 = false,
    val error:         String?                 = null
)

data class ChatThreadUiState(
    val isLoading:  Boolean           = true,
    val messages:   List<MessageItem> = emptyList(),
    val draft:      String            = "",
    val isSending:  Boolean           = false,
    val error:      String?           = null
)

data class UserProfileUiState(
    val isLoading:   Boolean          = true,
    val profile:     FullUserProfile? = null,
    val isFollowing: Boolean          = false,
    val error:       String?          = null
)