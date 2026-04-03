package com.conekt.suite.feature.profile

import android.net.Uri

data class EditProfileUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,

    // form fields — pre-populated from the fetched profile
    val displayName: String = "",
    val username: String = "",
    val bio: String = "",
    val location: String = "",
    val website: String = "",
    val phone: String = "",
    val isPrivate: Boolean = false,

    // current remote URLs
    val avatarUrl: String? = null,
    val bannerUrl: String? = null,

    // pending local picks (before upload)
    val pendingAvatarUri: Uri? = null,
    val pendingBannerUri: Uri? = null,

    val errorMessage: String? = null,
    val isSaveSuccess: Boolean = false
)
