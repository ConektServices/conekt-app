package com.conekt.suite.feature.library

import android.net.Uri
import com.conekt.suite.data.model.LocalTrack
import com.conekt.suite.data.model.MusicCommentWithAuthor
import com.conekt.suite.data.model.MusicStats
import com.conekt.suite.data.model.MusicTrackRecord

enum class MusicSource { LOCAL, ONLINE }

data class ActiveTrack(
    val id: String?,             // null for local tracks not yet uploaded
    val title: String,
    val artist: String,
    val album: String           = "",
    val uri: String,             // playback URI (content:// or https://)
    val coverUri: String?       = null,
    val durationMs: Long        = 0L,
    val source: MusicSource     = MusicSource.LOCAL
)

data class ShareMusicState(
    val visible: Boolean        = false,
    val localTrack: LocalTrack? = null,
    val title: String           = "",
    val artist: String          = "",
    val genre: String           = "",
    val coverUri: Uri?          = null,
    val isPublic: Boolean       = true,
    val isUploading: Boolean    = false,
    val errorMessage: String?   = null,
    val isSuccess: Boolean      = false
)

data class CommentsSheetState(
    val visible: Boolean                   = false,
    val trackId: String?                   = null,
    val comments: List<MusicCommentWithAuthor> = emptyList(),
    val isLoading: Boolean                 = false,
    val draftText: String                  = "",
    val isPosting: Boolean                 = false
)

data class MusicUiState(
    // Loading
    val isLoadingOnline: Boolean        = true,
    val isLoadingLocal: Boolean         = true,

    // Track lists
    val onlineTracks: List<MusicTrackRecord> = emptyList(),
    val localTracks: List<LocalTrack>        = emptyList(),
    val myUploads: List<MusicTrackRecord>    = emptyList(),
    val searchResults: List<MusicTrackRecord> = emptyList(),
    val searchQuery: String                   = "",

    // Playback
    val activeTrack: ActiveTrack?       = null,
    val isPlaying: Boolean              = false,
    val progressFraction: Float         = 0f,
    val positionMs: Long                = 0L,

    // Stats
    val stats: MusicStats               = MusicStats(),
    val isLoadingStats: Boolean         = true,

    // Sheets
    val shareSheet: ShareMusicState     = ShareMusicState(),
    val commentsSheet: CommentsSheetState = CommentsSheetState(),

    // Permission
    val needsStoragePermission: Boolean = false,

    // Error
    val errorMessage: String?           = null
)