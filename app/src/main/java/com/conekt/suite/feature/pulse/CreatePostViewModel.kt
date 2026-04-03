package com.conekt.suite.feature.pulse

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.conekt.suite.core.supabase.SupabaseProvider
import com.conekt.suite.data.repository.AuthRepository
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.ByteArrayOutputStream

// ── Insert DTOs ───────────────────────────────────────────────────────────────

@Serializable
private data class PostInsert(
    @SerialName("author_id")  val authorId: String,
    val body: String? = null,
    @SerialName("media_urls") val mediaUrls: List<String> = emptyList(),
    @SerialName("post_type")  val postType: String = "post",
    val visibility: String = "public",
    val location: String? = null
)

@Serializable
private data class StoryInsert(
    @SerialName("author_id") val authorId: String,
    @SerialName("media_url") val mediaUrl: String,
    @SerialName("media_type") val mediaType: String = "image",
    val caption: String? = null
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

class CreatePostViewModel(private val appContext: Context) : ViewModel() {

    private val supabase        = SupabaseProvider.client
    private val authRepository  = AuthRepository()

    private val _uiState = MutableStateFlow(CreatePostUiState())
    val uiState: StateFlow<CreatePostUiState> = _uiState.asStateFlow()

    // ── Field updates ─────────────────────────────────────────────────────────

    fun onTypeChange(type: CreatePostType)       = update { copy(type = type) }
    fun onBodyChange(v: String)                  = update { copy(body = v) }
    fun onVisibilityChange(v: PostVisibility)    = update { copy(visibility = v) }
    fun onLocationChange(v: String)              = update { copy(location = v) }
    fun onCaptionChange(v: String)               = update { copy(caption = v) }
    fun onMediaPicked(uris: List<Uri>)           = update { copy(mediaUris = (mediaUris + uris).take(4)) }
    fun onStoryMediaPicked(uri: Uri)             = update { copy(storyMediaUri = uri) }
    fun removeMedia(uri: Uri)                    = update { copy(mediaUris = mediaUris - uri) }
    fun clearError()                             = update { copy(errorMessage = null) }

    // ── Submit ────────────────────────────────────────────────────────────────

    fun submit() {
        when (_uiState.value.type) {
            CreatePostType.POST  -> createPost()
            CreatePostType.STORY -> createStory()
        }
    }

    private fun createPost() {
        val state = _uiState.value
        if (state.body.isBlank() && state.mediaUris.isEmpty()) {
            update { copy(errorMessage = "Write something or add a photo.") }
            return
        }

        viewModelScope.launch {
            update { copy(isPosting = true, errorMessage = null) }
            runCatching {
                val uid      = authRepository.currentUserId() ?: error("Not authenticated")
                val ts       = System.currentTimeMillis()
                val urls     = state.mediaUris.mapIndexed { i, uri ->
                    uploadImage(uri, "$uid/posts/${ts}_$i.jpg", "conekt-posts")
                }
                supabase.from("posts").insert(
                    PostInsert(
                        authorId   = uid,
                        body       = state.body.ifBlank { null },
                        mediaUrls  = urls,
                        visibility = state.visibility.value,
                        location   = state.location.ifBlank { null }
                    )
                )
            }.onSuccess {
                update { copy(isPosting = false, isSuccess = true) }
            }.onFailure { e ->
                update { copy(isPosting = false, errorMessage = friendlyError(e.message)) }
            }
        }
    }

    private fun createStory() {
        val state = _uiState.value
        val uri   = state.storyMediaUri
        if (uri == null) {
            update { copy(errorMessage = "Pick a photo or video for your story.") }
            return
        }

        viewModelScope.launch {
            update { copy(isPosting = true, errorMessage = null) }
            runCatching {
                val uid = authRepository.currentUserId() ?: error("Not authenticated")
                val ts  = System.currentTimeMillis()
                val url = uploadImage(uri, "$uid/stories/$ts.jpg", "conekt-stories")
                supabase.from("stories").insert(
                    StoryInsert(
                        authorId  = uid,
                        mediaUrl  = url,
                        mediaType = "image",
                        caption   = state.caption.ifBlank { null }
                    )
                )
            }.onSuccess {
                update { copy(isPosting = false, isSuccess = true) }
            }.onFailure { e ->
                update { copy(isPosting = false, errorMessage = friendlyError(e.message)) }
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private suspend fun uploadImage(uri: Uri, path: String, bucket: String): String {
        val stream   = appContext.contentResolver.openInputStream(uri)
            ?: error("Cannot read selected image.")
        val original = BitmapFactory.decodeStream(stream)
        val maxDim   = 1280
        val (w, h)   = if (original.width > original.height)
            maxDim to (maxDim * original.height / original.width)
        else
            (maxDim * original.width / original.height) to maxDim
        val scaled = Bitmap.createScaledBitmap(original, w, h, true)
        original.recycle()
        val bytes = ByteArrayOutputStream().also { out ->
            scaled.compress(Bitmap.CompressFormat.JPEG, 88, out)
            scaled.recycle()
        }.toByteArray()
        supabase.storage.from(bucket).upload(path, bytes) { upsert = true }
        return supabase.storage.from(bucket).publicUrl(path)
    }

    private fun friendlyError(msg: String?): String = when {
        msg == null                                          -> "Something went wrong."
        msg.contains("Bucket not found", ignoreCase = true) ->
            "Storage not set up. Create 'conekt-posts' and 'conekt-stories' buckets in Supabase."
        msg.contains("network", ignoreCase = true)          -> "No connection. Try again."
        else -> msg
    }

    private fun update(block: CreatePostUiState.() -> CreatePostUiState) {
        _uiState.value = _uiState.value.block()
    }

    // ── Factory ───────────────────────────────────────────────────────────────

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            CreatePostViewModel(context.applicationContext) as T
    }
}