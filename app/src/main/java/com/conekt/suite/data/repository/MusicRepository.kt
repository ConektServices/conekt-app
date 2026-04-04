package com.conekt.suite.data.repository

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.conekt.suite.core.supabase.SupabaseProvider
import com.conekt.suite.data.model.MusicCommentRecord
import com.conekt.suite.data.model.MusicCommentWithAuthor
import com.conekt.suite.data.model.MusicListenerInsert
import com.conekt.suite.data.model.MusicPlayInsert
import com.conekt.suite.data.model.MusicStats
import com.conekt.suite.data.model.MusicTrackRecord
import com.conekt.suite.feature.library.LocalMusicScanner
import com.conekt.suite.data.model.LocalTrack
import com.conekt.suite.feature.library.LiveListenerEntry
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.ByteArrayOutputStream
import java.net.URL

// ─────────────────────────────────────────────────────────────────────────────
// ALL @Serializable DTOs must be at file (top) level.
// The Kotlin serialization compiler plugin cannot generate serializers for
// classes declared inside function bodies or companion objects.
// ─────────────────────────────────────────────────────────────────────────────

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

// ── Live-listener join DTOs ───────────────────────────────────────────────────

@Serializable
private data class ListenerProfile(
    val id: String,
    val username: String,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("avatar_url")   val avatarUrl: String?   = null
)

@Serializable
private data class ListenerTrack(
    val id: String,
    val title: String,
    val artist: String,
    @SerialName("cover_url") val coverUrl: String? = null,
    @SerialName("file_url")  val fileUrl: String,
    @SerialName("is_public") val isPublic: Boolean = true
)

@Serializable
private data class ListenerRow(
    @SerialName("track_id")   val trackId: String,
    @SerialName("started_at") val startedAt: String,
    val listener: ListenerProfile,
    val track: ListenerTrack
)

// ─────────────────────────────────────────────────────────────────────────────

class MusicRepository(
    private val authRepository: AuthRepository = AuthRepository()
) {
    private val supabase = SupabaseProvider.client

    private fun uid() = authRepository.currentUserId() ?: error("Not authenticated")

    // ── Local music ───────────────────────────────────────────────────────────

    fun scanLocalTracks(context: Context): List<LocalTrack> =
        LocalMusicScanner(context).scan()

    // ── Online tracks ─────────────────────────────────────────────────────────

    suspend fun fetchPublicTracks(limit: Int = 30): List<MusicTrackRecord> =
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
                        ilike("title",  "%$query%")
                        ilike("artist", "%$query%")
                        ilike("album",  "%$query%")
                        ilike("genre",  "%$query%")
                    }
                }
                limit(40)
            }
            .decodeList<MusicTrackRecord>()

    // ── Live listeners ────────────────────────────────────────────────────────

    /**
     * Returns who is currently streaming, by joining music_listeners with
     * profiles and music_tracks. Only public tracks are included.
     */
    suspend fun fetchLiveListeners(): List<LiveListenerEntry> {
        val columns = Columns.raw(
            """
            track_id,
            started_at,
            listener:profiles!music_listeners_listener_id_fkey (
                id, username, display_name, avatar_url
            ),
            track:music_tracks!music_listeners_track_id_fkey (
                id, title, artist, cover_url, file_url, is_public
            )
            """.trimIndent()
        )
        return try {
            supabase.from("music_listeners")
                .select(columns = columns) {}
                .decodeList<ListenerRow>()           // ListenerRow is file-level → OK
                .filter { it.track.isPublic }
                .map { row ->
                    LiveListenerEntry(
                        userId        = row.listener.id,
                        username      = row.listener.username,
                        displayName   = row.listener.displayName,
                        avatarUrl     = row.listener.avatarUrl,
                        trackId       = row.track.id,
                        trackTitle    = row.track.title,
                        artistName    = row.track.artist,
                        trackCoverUrl = row.track.coverUrl,
                        trackFileUrl  = row.track.fileUrl,
                        isLocal       = false
                    )
                }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /** Best-effort: mark user as playing a local track for friends to see. */
    suspend fun broadcastLocalPlay(title: String, artist: String) {
        // Wire up once music_local_broadcasts table is confirmed in Supabase.
    }

    // ── Upload music ──────────────────────────────────────────────────────────

    suspend fun uploadTrack(
        context:    Context,
        audioUri:   Uri,
        coverUri:   Uri?,
        title:      String,
        artist:     String,
        album:      String?,
        genre:      String?,
        durationMs: Int,
        isPublic:   Boolean
    ): MusicTrackRecord {
        val myUid     = uid()
        val ts        = System.currentTimeMillis()
        val audioPath = "$myUid/$ts.mp3"

        val audioBytes = context.contentResolver.openInputStream(audioUri)
            ?.readBytes() ?: error("Cannot read audio file")

        supabase.storage.from("conekt-music").upload(audioPath, audioBytes) { upsert = true }
        val audioUrl = supabase.storage.from("conekt-music").publicUrl(audioPath)

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

    // ── Download track to device ──────────────────────────────────────────────

    suspend fun downloadTrackToLocal(
        context:  Context,
        trackId:  String,
        fileUrl:  String,
        title:    String,
        artist:   String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val bytes = URL(fileUrl).readBytes()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Audio.Media.DISPLAY_NAME, "$title - $artist.mp3")
                    put(MediaStore.Audio.Media.MIME_TYPE, "audio/mpeg")
                    put(MediaStore.Audio.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MUSIC}/Conekt")
                    put(MediaStore.Audio.Media.IS_PENDING, 1)
                }
                val uri = context.contentResolver
                    .insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
                    ?: return@withContext false
                context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                values.clear()
                values.put(MediaStore.Audio.Media.IS_PENDING, 0)
                context.contentResolver.update(uri, values, null, null)
            } else {
                @Suppress("DEPRECATION")
                val dir = java.io.File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
                    "Conekt"
                )
                dir.mkdirs()
                java.io.File(dir, "$title - $artist.mp3").writeBytes(bytes)
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    // ── Play tracking ─────────────────────────────────────────────────────────

    suspend fun recordPlay(
        trackId:   String,
        durationS: Int,
        completed: Boolean,
        source:    String = "stream"
    ) {
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
            supabase.postgrest.rpc("increment_play_count", mapOf("track_id" to trackId))
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
                eq("track_id",    trackId)
                eq("listener_id", uid())
            }
        }
    }

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
            .insert(
                CommentInsert(trackId = trackId, authorId = uid(), body = body.trim())
            ) { select() }
            .decodeSingle<MusicCommentRecord>()
        runCatching {
            supabase.postgrest.rpc("increment_comment_count", mapOf("track_id" to trackId))
        }
        return record
    }

    // ── Stats ─────────────────────────────────────────────────────────────────

    suspend fun getMyStats(): MusicStats {
        val myUid      = uid()
        val today      = java.time.LocalDate.now().toString()
        val monthStart = java.time.LocalDate.now().withDayOfMonth(1).toString()

        val todayPlays = runCatching {
            supabase.from("music_plays")
                .select {
                    filter {
                        eq("listener_id", myUid)
                        gte("played_at", "${today}T00:00:00Z")
                    }
                }
                .decodeList<MusicPlayInsert>().size
        }.getOrDefault(0)

        val monthlyPlays = runCatching {
            supabase.from("music_plays")
                .select {
                    filter {
                        eq("listener_id", myUid)
                        gte("played_at", "${monthStart}T00:00:00Z")
                    }
                }
                .decodeList<MusicPlayInsert>().size
        }.getOrDefault(0)

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
            totalPlays   = monthlyPlays,
            topTrack     = topTrack
        )
    }
}