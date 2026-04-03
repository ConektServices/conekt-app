package com.conekt.suite.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MusicTrackRecord(
    val id: String,
    @SerialName("uploader_id")     val uploaderId: String,
    val title: String,
    val artist: String,
    val album: String?             = null,
    val genre: String?             = null,
    @SerialName("duration_ms")     val durationMs: Int = 0,
    @SerialName("file_url")        val fileUrl: String,
    @SerialName("cover_url")       val coverUrl: String? = null,
    @SerialName("is_public")       val isPublic: Boolean = true,
    @SerialName("play_count")      val playCount: Int = 0,
    @SerialName("like_count")      val likeCount: Int = 0,
    @SerialName("comment_count")   val commentCount: Int = 0,
    @SerialName("file_size_bytes") val fileSizeBytes: Long = 0L,
    @SerialName("created_at")      val createdAt: String
)

@Serializable
data class MusicTrackWithUploader(
    val id: String,
    val title: String,
    val artist: String,
    val album: String?             = null,
    val genre: String?             = null,
    @SerialName("duration_ms")     val durationMs: Int = 0,
    @SerialName("file_url")        val fileUrl: String,
    @SerialName("cover_url")       val coverUrl: String? = null,
    @SerialName("play_count")      val playCount: Int = 0,
    @SerialName("like_count")      val likeCount: Int = 0,
    @SerialName("comment_count")   val commentCount: Int = 0,
    @SerialName("created_at")      val createdAt: String,
    val uploader: ProfileSnippet
)

@Serializable
data class MusicCommentRecord(
    val id: String,
    @SerialName("track_id")  val trackId: String,
    @SerialName("author_id") val authorId: String,
    val body: String,
    @SerialName("like_count") val likeCount: Int = 0,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class MusicCommentWithAuthor(
    val id: String,
    @SerialName("track_id")   val trackId: String,
    val body: String,
    @SerialName("like_count") val likeCount: Int = 0,
    @SerialName("created_at") val createdAt: String,
    val author: ProfileSnippet
)

@Serializable
data class MusicPlayInsert(
    @SerialName("track_id")    val trackId: String,
    @SerialName("listener_id") val listenerId: String,
    @SerialName("duration_s")  val durationS: Int = 0,
    val completed: Boolean     = false,
    val source: String         = "stream"
)

@Serializable
data class MusicListenerInsert(
    @SerialName("track_id")    val trackId: String,
    @SerialName("listener_id") val listenerId: String
)

// ── Local device track (not serializable — from MediaStore) ──────────────────

data class LocalTrack(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,     // ms
    val uri: String,        // content:// URI
    val albumArtUri: String?
)

// ── Stats aggregates ──────────────────────────────────────────────────────────

data class MusicStats(
    val todayPlays: Int     = 0,
    val monthlyPlays: Int   = 0,
    val totalPlays: Int     = 0,
    val topTrack: MusicTrackRecord? = null,
    val activeListeners: Int = 0
)