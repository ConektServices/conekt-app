package com.conekt.suite.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfileRecord(
    val id: String,
    val username: String,
    @SerialName("display_name") val displayName: String? = null,
    val bio: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("banner_url") val bannerUrl: String? = null,
    val website: String? = null,
    val location: String? = null,
    val phone: String? = null,
    @SerialName("is_verified") val isVerified: Boolean = false,
    @SerialName("is_private") val isPrivate: Boolean = false,
    @SerialName("follower_count") val followerCount: Int = 0,
    @SerialName("following_count") val followingCount: Int = 0,
    @SerialName("post_count") val postCount: Int = 0,
    @SerialName("storage_used_bytes") val storageUsedBytes: Long = 0,
    @SerialName("storage_limit_bytes") val storageLimitBytes: Long = 5368709120L
)

@Serializable
data class PostRecord(
    val id: String,
    @SerialName("author_id") val authorId: String,
    val body: String? = null,
    @SerialName("media_urls") val mediaUrls: List<String> = emptyList(),
    @SerialName("post_type") val postType: String = "post",
    val visibility: String = "public",
    @SerialName("like_count") val likeCount: Int = 0,
    @SerialName("comment_count") val commentCount: Int = 0,
    @SerialName("share_count") val shareCount: Int = 0,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class FileRecord(
    val id: String,
    @SerialName("owner_id") val ownerId: String,
    val name: String,
    @SerialName("mime_type") val mimeType: String,
    @SerialName("size_bytes") val sizeBytes: Long,
    @SerialName("file_type") val fileType: String,
    @SerialName("thumbnail_url") val thumbnailUrl: String? = null,
    @SerialName("is_public") val isPublic: Boolean = false,
    @SerialName("is_starred") val isStarred: Boolean = false,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class NoteRecord(
    val id: String,
    val title: String,
    val body: String = "",
    @SerialName("cover_color") val coverColor: String? = null,
    @SerialName("is_pinned") val isPinned: Boolean = false,
    @SerialName("is_shared_post") val isSharedPost: Boolean = false,
    @SerialName("updated_at") val updatedAt: String
)