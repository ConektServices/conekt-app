package com.conekt.suite.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.conekt.suite.core.supabase.SupabaseProvider
import com.conekt.suite.data.model.MusicCommentRecord
import com.conekt.suite.data.model.MusicCommentWithAuthor
import com.conekt.suite.data.model.MusicListenerInsert
import com.conekt.suite.data.model.MusicPlayInsert
import com.conekt.suite.data.model.MusicStats
import com.conekt.suite.data.model.MusicTrackRecord
import com.conekt.suite.data.model.MusicTrackWithUploader
import com.conekt.suite.feature.library.LocalMusicScanner
import com.conekt.suite.data.model.LocalTrack
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.storage.storage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.ByteArrayOutputStream

@Serializable
private data class TrackInsert(
    @SerialName("uploader_id")     val uploaderId: String,
    val title: String,
    val artist: String,
    val album: String?             = null,
    val genre: String?             = null,
    @SerialName("duration_ms")     val durationMs: Int = 0,
    @SerialName("file_url")        val fileUrl: String,
    @SerialName("cover_url")       val coverUrl: String? = null,
    @SerialName("is_public")       val isPublic: Boolean = true,
    @SerialName("file_size_bytes") val fileSizeBytes: Long = 0L
)

@Serializable
private data class CommentInsert(
    @SerialName("track_id")  val trackId: String,
    @SerialName("author_id") val authorId: String,
    val body: String
)

class MusicRepository(
    private val authRepository: AuthRepository = AuthRepository()
) {
    private val supabase = SupabaseProvider.client

    private fun uid() = authRepository.currentUserId() ?: error("Not authenticated")

    // ── Local music ───────────────────────────────────────────────────────────

    fun scanLocalTracks(context: Context): List<LocalTrack> =
        LocalMusicScanner(context).scan()

    // ── Online tracks ─────────────────────────────────────────────────────────

    suspend fun fetchPublicTracks(limit: Int = 20): List<MusicTrackRecord> =
        supabase.from("music_tracks")
            .select {
                filter { eq("is_public", true) }
                order("play_count", order = Order.DESCENDING)
                limit(limit.toLong())
            }
            .decodeList<MusicTrackRecord>()

    suspend fun fetchMyUploads(): List<MusicTrackRecord> =
        supabase.from("music_tracks")
            .select {
                filter { eq("uploader_id", uid()) }
                order("created_at", order = Order.DESCENDING)
            }
            .decodeList<MusicTrackRecord>()

    suspend fun searchTracks(query: String): List<MusicTrackRecord> =
        supabase.from("music_tracks")
            .select {
                filter {
                    eq("is_public", true)
                    or {
                        ilike("title", "%$query%")
                        ilike("artist", "%$query%")
                        ilike("album", "%$query%")
                    }
                }
                limit(30)
            }
            .decodeList<MusicTrackRecord>()

    // ── Upload music ──────────────────────────────────────────────────────────

    suspend fun uploadTrack(
        context: Context,
        audioUri: Uri,
        coverUri: Uri?,
        title: String,
        artist: String,
        album: String?,
        genre: String?,
        durationMs: Int,
        isPublic: Boolean
    ): MusicTrackRecord {
        val myUid    = uid()
        val ts       = System.currentTimeMillis()
        val audioPath = "$myUid/$ts.mp3"

        // Read audio bytes
        val audioBytes = context.contentResolver.openInputStream(audioUri)
            ?.readBytes()
            ?: error("Cannot read audio file")

        // Upload audio
        supabase.storage.from("conekt-music").upload(audioPath, audioBytes) { upsert = true }
        val audioUrl = supabase.storage.from("conekt-music").publicUrl(audioPath)

        // Upload cover if provided
        val coverUrl = coverUri?.let {
            val coverPath = "$myUid/covers/$ts.jpg"
            val stream    = context.contentResolver.openInputStream(it)
                ?: error("Cannot read cover image")
            val original  = BitmapFactory.decodeStream(stream)
            val scaled    = Bitmap.createScaledBitmap(original, 500, 500, true)
            original.recycle()
            val bytes = ByteArrayOutputStream().also { out ->
                scaled.compress(Bitmap.CompressFormat.JPEG, 88, out)
                scaled.recycle()
            }.toByteArray()
            supabase.storage.from("conekt-music").upload(coverPath, bytes) { upsert = true }
            supabase.storage.from("conekt-music").publicUrl(coverPath)
        }

        // Insert record
        return supabase.from("music_tracks")
            .insert(
                TrackInsert(
                    uploaderId    = myUid,
                    title         = title.trim(),
                    artist        = artist.trim(),
                    album         = album?.trim(),
                    genre         = genre?.trim(),
                    durationMs    = durationMs,
                    fileUrl       = audioUrl,
                    coverUrl      = coverUrl,
                    isPublic      = isPublic,
                    fileSizeBytes = audioBytes.size.toLong()
                )
            ) { select() }
            .decodeSingle<MusicTrackRecord>()
    }

    // ── Play tracking ─────────────────────────────────────────────────────────

    suspend fun recordPlay(trackId: String, durationS: Int, completed: Boolean, source: String = "stream") {
        runCatching {
            supabase.from("music_plays").insert(
                MusicPlayInsert(
                    trackId    = trackId,
                    listenerId = uid(),
                    durationS  = durationS,
                    completed  = completed,
                    source     = source
                )
            )
            // Increment play_count via DB function
            supabase.postgrest.rpc(
                "increment_play_count",
                mapOf("track_id" to trackId)
            )
        }
    }

    // ── Listener presence ─────────────────────────────────────────────────────

    suspend fun joinListener(trackId: String) = runCatching {
        supabase.from("music_listeners").upsert(
            MusicListenerInsert(trackId = trackId, listenerId = uid())
        )
    }

    suspend fun leaveListener(trackId: String) = runCatching {
        supabase.from("music_listeners").delete {
            filter {
                eq("track_id", trackId)
                eq("listener_id", uid())
            }
        }
    }

    suspend fun getActiveListenerCount(trackId: String): Int = runCatching {
        supabase.from("music_listeners")
            .select { filter { eq("track_id", trackId) } }
            .decodeList<MusicListenerInsert>()
            .size
    }.getOrDefault(0)

    // ── Comments ──────────────────────────────────────────────────────────────

    suspend fun fetchComments(trackId: String): List<MusicCommentWithAuthor> {
        val columns = Columns.raw(
            """
            id, track_id, body, like_count, created_at,
            author:profiles!music_comments_author_id_fkey (
                id, username, display_name, avatar_url
            )
            """.trimIndent()
        )
        return supabase.from("music_comments")
            .select(columns = columns) {
                filter { eq("track_id", trackId) }
                order("created_at", order = Order.ASCENDING)
            }
            .decodeList<MusicCommentWithAuthor>()
    }

    suspend fun postComment(trackId: String, body: String): MusicCommentRecord {
        val record = supabase.from("music_comments")
            .insert(CommentInsert(trackId = trackId, authorId = uid(), body = body.trim())) {
                select()
            }
            .decodeSingle<MusicCommentRecord>()
        runCatching { supabase.postgrest.rpc("increment_comment_count", mapOf("track_id" to trackId)) }
        return record
    }

    suspend fun deleteComment(commentId: String) {
        supabase.from("music_comments").delete { filter { eq("id", commentId) } }
    }

    // ── Stats ─────────────────────────────────────────────────────────────────

    suspend fun getMyStats(): MusicStats {
        val myUid = uid()
        // Count today's plays
        val today = java.time.LocalDate.now().toString()
        val todayPlays = runCatching {
            supabase.from("music_plays")
                .select {
                    filter {
                        eq("listener_id", myUid)
                        gte("played_at", "${today}T00:00:00Z")
                    }
                }
                .decodeList<MusicPlayInsert>()
                .size
        }.getOrDefault(0)

        // Count this month
        val monthStart = java.time.LocalDate.now().withDayOfMonth(1).toString()
        val monthlyPlays = runCatching {
            supabase.from("music_plays")
                .select {
                    filter {
                        eq("listener_id", myUid)
                        gte("played_at", "${monthStart}T00:00:00Z")
                    }
                }
                .decodeList<MusicPlayInsert>()
                .size
        }.getOrDefault(0)

        // My most played upload
        val topTrack = runCatching {
            supabase.from("music_tracks")
                .select {
                    filter { eq("uploader_id", myUid) }
                    order("play_count", order = Order.DESCENDING)
                    limit(1)
                }
                .decodeList<MusicTrackRecord>()
                .firstOrNull()
        }.getOrNull()

        return MusicStats(
            todayPlays   = todayPlays,
            monthlyPlays = monthlyPlays,
            totalPlays   = monthlyPlays, // simplified
            topTrack     = topTrack
        )
    }
}