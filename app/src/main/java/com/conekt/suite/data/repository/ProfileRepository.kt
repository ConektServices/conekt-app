package com.conekt.suite.data.repository

import com.conekt.suite.data.model.FileRecord
import com.conekt.suite.data.model.NoteRecord
import com.conekt.suite.data.model.PostRecord
import com.conekt.suite.data.model.ProfileRecord
import com.conekt.suite.data.remote.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order

data class UpdateProfileRequest(
    val displayName: String,
    val bio: String,
    val location: String,
    val website: String,
    val isPrivate: Boolean,
    val avatarUrl: String? = null,
    val bannerUrl: String? = null
)

class ProfileRepository(
    private val authRepository: AuthRepository = AuthRepository()
) {
    private val supabase = SupabaseProvider.client

    private fun requireUid(): String =
        authRepository.currentUserId() ?: error("No authenticated user")

    // ── Profile ──────────────────────────────────────────────────────────────

    suspend fun getMyProfile(): ProfileRecord {
        val uid = requireUid()
        return supabase.from("profiles")
            .select { filter { eq("id", uid) } }
            .decodeSingle<ProfileRecord>()
    }

    suspend fun updateMyProfile(request: UpdateProfileRequest): ProfileRecord {
        val uid = requireUid()
        return supabase.from("profiles")
            .update({
                set("display_name", request.displayName)
                set("bio", request.bio)
                set("location", request.location)
                set("website", request.website)
                set("is_private", request.isPrivate)
                request.avatarUrl?.let { set("avatar_url", it) }
                request.bannerUrl?.let { set("banner_url", it) }
            }) {
                select()
                filter { eq("id", uid) }
            }
            .decodeSingle<ProfileRecord>()
    }

    suspend fun updatePhone(phone: String) {
        val uid = requireUid()
        supabase.from("profiles")
            .update({ set("phone", phone.trim()) }) {
                filter { eq("id", uid) }
            }
    }

    // ── Posts ─────────────────────────────────────────────────────────────────

    suspend fun getMyPosts(): List<PostRecord> {
        val uid = requireUid()
        return supabase.from("posts")
            .select {
                filter { eq("author_id", uid) }
                order("created_at", order = Order.DESCENDING)
                limit(20)
            }
            .decodeList<PostRecord>()
    }

    suspend fun getMyPostedNotes(): List<PostRecord> {
        val uid = requireUid()
        return supabase.from("posts")
            .select {
                filter {
                    eq("author_id", uid)
                    eq("post_type", "note_share")
                }
                order("created_at", order = Order.DESCENDING)
            }
            .decodeList<PostRecord>()
    }

    // ── Files ─────────────────────────────────────────────────────────────────

    suspend fun getMyFiles(): List<FileRecord> {
        val uid = requireUid()
        return supabase.from("files")
            .select {
                filter { eq("owner_id", uid) }
                order("created_at", order = Order.DESCENDING)
                limit(20)
            }
            .decodeList<FileRecord>()
    }

    // ── Notes ─────────────────────────────────────────────────────────────────

    suspend fun getMyNotes(): List<NoteRecord> {
        val uid = requireUid()
        return supabase.from("notes")
            .select {
                filter { eq("owner_id", uid) }
                order("updated_at", order = Order.DESCENDING)
                limit(10)
            }
            .decodeList<NoteRecord>()
    }

    // ── Public profile ────────────────────────────────────────────────────────

    suspend fun getPublicProfile(userId: String): ProfileRecord =
        supabase.from("profiles")
            .select { filter { eq("id", userId) } }
            .decodeSingle<ProfileRecord>()

    suspend fun getPublicProfilePosts(userId: String): List<PostRecord> =
        supabase.from("posts")
            .select {
                filter {
                    eq("author_id", userId)
                    eq("visibility", "public")
                }
                order("created_at", order = Order.DESCENDING)
            }
            .decodeList<PostRecord>()

    suspend fun getPublicProfileFiles(userId: String): List<FileRecord> =
        supabase.from("files")
            .select {
                filter {
                    eq("owner_id", userId)
                    eq("is_public", true)
                }
                order("created_at", order = Order.DESCENDING)
            }
            .decodeList<FileRecord>()
}