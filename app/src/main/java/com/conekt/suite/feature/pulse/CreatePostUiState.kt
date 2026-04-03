package com.conekt.suite.feature.pulse

import android.net.Uri

enum class CreatePostType { POST, STORY }

enum class PostVisibility(val label: String, val value: String) {
    PUBLIC("Public", "public"),
    FOLLOWERS("Followers", "followers"),
    PRIVATE("Private", "private")
}

data class CreatePostUiState(
    val type: CreatePostType = CreatePostType.POST,

    // Post fields
    val body: String = "",
    val visibility: PostVisibility = PostVisibility.PUBLIC,
    val location: String = "",
    val mediaUris: List<Uri> = emptyList(),

    // Story fields
    val storyMediaUri: Uri? = null,
    val caption: String = "",

    // Async state
    val isPosting: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
)