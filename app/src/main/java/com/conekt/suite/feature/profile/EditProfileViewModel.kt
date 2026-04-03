package com.conekt.suite.feature.profile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.conekt.suite.core.supabase.SupabaseProvider
import com.conekt.suite.data.repository.AuthRepository
import com.conekt.suite.data.repository.ProfileRepository
import com.conekt.suite.data.repository.UpdateProfileRequest
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

class EditProfileViewModel(
    private val appContext: Context
) : ViewModel() {

    private val repository    = ProfileRepository()
    private val authRepository = AuthRepository()
    private val supabase      = SupabaseProvider.client

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    init { loadProfile() }

    // ── Load ──────────────────────────────────────────────────────────────────

    private fun loadProfile() {
        viewModelScope.launch {
            update { copy(isLoading = true) }
            runCatching { repository.getMyProfile() }
                .onSuccess { p ->
                    update {
                        copy(
                            isLoading    = false,
                            displayName  = p.displayName.orEmpty(),
                            username     = p.username,
                            bio          = p.bio.orEmpty(),
                            location     = p.location.orEmpty(),
                            website      = p.website.orEmpty(),
                            phone        = p.phone.orEmpty(),
                            isPrivate    = p.isPrivate,
                            avatarUrl    = p.avatarUrl,
                            bannerUrl    = p.bannerUrl
                        )
                    }
                }
                .onFailure { e ->
                    update { copy(isLoading = false, errorMessage = e.message ?: "Failed to load profile.") }
                }
        }
    }

    // ── Field updates ─────────────────────────────────────────────────────────

    fun onDisplayNameChange(v: String) = update { copy(displayName = v) }
    fun onUsernameChange(v: String)    = update { copy(username = v.trim().lowercase()) }
    fun onBioChange(v: String)         = update { copy(bio = v) }
    fun onLocationChange(v: String)    = update { copy(location = v) }
    fun onWebsiteChange(v: String)     = update { copy(website = v.trim()) }
    fun onPhoneChange(v: String)       = update { copy(phone = v.filter { it.isDigit() || it in "+() -" }) }
    fun onPrivateToggle()              = update { copy(isPrivate = !isPrivate) }
    fun onAvatarPicked(uri: Uri)       = update { copy(pendingAvatarUri = uri) }
    fun onBannerPicked(uri: Uri)       = update { copy(pendingBannerUri = uri) }
    fun clearError()                   = update { copy(errorMessage = null) }

    // ── Save ──────────────────────────────────────────────────────────────────

    fun save() {
        val state = _uiState.value

        val error = when {
            state.displayName.isBlank()  -> "Display name is required."
            state.username.isBlank()     -> "Username is required."
            state.username.length < 3    -> "Username must be at least 3 characters."
            !state.username.matches(Regex("^[a-z0-9._]+$")) ->
                "Username may only contain letters, numbers, dots and underscores."
            else -> null
        }
        if (error != null) { update { copy(errorMessage = error) }; return }

        viewModelScope.launch {
            update { copy(isSaving = true, errorMessage = null) }

            runCatching {
                val uid = authRepository.currentUserId() ?: error("Not authenticated")

                val newAvatarUrl = state.pendingAvatarUri
                    ?.let { uploadImage(it, "$uid/avatar.jpg", "conekt-avatars") }
                    ?: state.avatarUrl

                val newBannerUrl = state.pendingBannerUri
                    ?.let { uploadImage(it, "$uid/banner.jpg", "conekt-banners") }
                    ?: state.bannerUrl

                val phone = state.phone.trim()
                if (phone.isNotBlank()) repository.updatePhone(phone)

                repository.updateMyProfile(
                    UpdateProfileRequest(
                        displayName = state.displayName.trim(),
                        bio         = state.bio.trim(),
                        location    = state.location.trim(),
                        website     = state.website.trim(),
                        isPrivate   = state.isPrivate,
                        avatarUrl   = newAvatarUrl,
                        bannerUrl   = newBannerUrl
                    )
                )
            }.onSuccess {
                update {
                    copy(
                        isSaving         = false,
                        isSaveSuccess    = true,
                        pendingAvatarUri = null,
                        pendingBannerUri = null
                    )
                }
            }.onFailure { e ->
                update {
                    copy(
                        isSaving      = false,
                        errorMessage  = friendlyError(e.message)
                    )
                }
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private suspend fun uploadImage(uri: Uri, path: String, bucket: String): String {
        val stream = appContext.contentResolver.openInputStream(uri)
            ?: error("Cannot read selected image.")

        val original   = BitmapFactory.decodeStream(stream)
        val maxDim     = 1024
        val (w, h) = if (original.width > original.height) {
            maxDim to (maxDim * original.height / original.width)
        } else {
            (maxDim * original.width / original.height) to maxDim
        }
        val scaled = Bitmap.createScaledBitmap(original, w, h, true)
        original.recycle()

        val bytes = ByteArrayOutputStream().also { out ->
            scaled.compress(Bitmap.CompressFormat.JPEG, 88, out)
            scaled.recycle()
        }.toByteArray()

        supabase.storage.from(bucket).upload(path, bytes) { upsert = true }
        return supabase.storage.from(bucket).publicUrl(path)
    }

    private fun update(block: EditProfileUiState.() -> EditProfileUiState) {
        _uiState.value = _uiState.value.block()
    }

    private fun friendlyError(msg: String?): String = when {
        msg == null -> "Failed to save profile."
        msg.contains("duplicate key", ignoreCase = true) &&
                msg.contains("username", ignoreCase = true) -> "That username is already taken."
        msg.contains("Bucket not found", ignoreCase = true) ->
            "Storage bucket missing. Create 'conekt-avatars' and 'conekt-banners' in Supabase Storage."
        msg.contains("network", ignoreCase = true) -> "No connection. Try again."
        else -> msg
    }

    // ── Factory ───────────────────────────────────────────────────────────────

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            EditProfileViewModel(context.applicationContext) as T
    }
}