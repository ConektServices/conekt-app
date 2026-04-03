package com.conekt.suite.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfileSnippet(
    val id: String,
    val username: String,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null
)

@Serializable
data class StoryWithAuthor(
    val id: String,
    @SerialName("media_url") val mediaUrl: String,
    @SerialName("media_type") val mediaType: String,
    val caption: String? = null,
    @SerialName("created_at") val createdAt: String,
    val author: ProfileSnippet
)

@Serializable
data class PostWithAuthor(
    val id: String,
    val body: String? = null,
    @SerialName("media_urls") val mediaUrls: List<String> = emptyList(),
    @SerialName("post_type") val postType: String = "post",
    val visibility: String = "public",
    @SerialName("like_count") val likeCount: Int = 0,
    @SerialName("comment_count") val commentCount: Int = 0,
    @SerialName("share_count") val shareCount: Int = 0,
    @SerialName("created_at") val createdAt: String,
    val author: ProfileSnippet
)

@Serializable
data class FilePreview(
    val id: String,
    val name: String,
    @SerialName("file_type") val fileType: String,
    @SerialName("thumbnail_url") val thumbnailUrl: String? = null,
    @SerialName("mime_type") val mimeType: String,
    @SerialName("size_bytes") val sizeBytes: Long = 0,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class NotePreview(
    val id: String,
    val title: String,
    val body: String = "",
    @SerialName("cover_color") val coverColor: String? = null,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("is_pinned") val isPinned: Boolean = false
)

data class PulseHomeData(
    val stories: List<StoryWithAuthor> = emptyList(),
    val posts: List<PostWithAuthor> = emptyList(),
    val recentFiles: List<FilePreview> = emptyList(),
    val recentNotes: List<NotePreview> = emptyList()
)