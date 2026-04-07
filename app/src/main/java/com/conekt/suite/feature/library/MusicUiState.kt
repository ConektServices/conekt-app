package com.conekt.suite.feature.library

import android.net.Uri
import com.conekt.suite.data.model.LocalTrack
import com.conekt.suite.data.model.MusicCommentWithAuthor
import com.conekt.suite.data.model.MusicStats
import com.conekt.suite.data.model.MusicTrackRecord

enum class MusicSource { LOCAL, ONLINE }

data class ActiveTrack(
    val id:         String?,
    val title:      String,
    val artist:     String,
    val album:      String       = "",
    val uri:        String,
    val coverUri:   String?      = null,
    val durationMs: Long         = 0L,
    val source:     MusicSource  = MusicSource.LOCAL
)

data class ShareMusicState(
    val visible:    Boolean      = false,
    val localTrack: LocalTrack?  = null,
    val title:      String       = "",
    val artist:     String       = "",
    val genre:      String       = "",
    val coverUri:   Uri?         = null,
    val isPublic:   Boolean      = true,
    val isUploading: Boolean     = false,
    val errorMessage: String?    = null,
    val isSuccess:  Boolean      = false
)

data class CommentsSheetState(
    val visible:   Boolean                      = false,
    val trackId:   String?                      = null,
    val comments:  List<MusicCommentWithAuthor> = emptyList(),
    val isLoading: Boolean                      = false,
    val draftText: String                       = "",
    val isPosting: Boolean                      = false
)

/** Represents a user who is currently listening (live presence) */
data class LiveListenerEntry(
    val userId:       String,
    val username:     String,
    val displayName:  String?,
    val avatarUrl:    String?,
    val trackId:      String,
    val trackTitle:   String,
    val artistName:   String,
    val trackCoverUrl: String?,
    val trackFileUrl: String,
    val isLocal:      Boolean = false
)

/** Music privacy / feature settings */
data class MusicSettings(
    val shareListeningActivity: Boolean  = true,
    val shareLocalActivity: Boolean      = false,
    val allowDownloads: Boolean          = true,
    val autoplay: Boolean                = true,
    val highQualityStream: Boolean       = false,
    val crossfadeSeconds: Int            = 0,
    val showEqualizer: Boolean           = true,
    val liveNotifications: Boolean       = true
)

data class MusicUiState(
    // Loading
    val isLoadingOnline:  Boolean    = true,
    val isLoadingLocal:   Boolean    = true,
    val isLoadingStats:   Boolean    = true,

    // Track lists
    val onlineTracks:     List<MusicTrackRecord>  = emptyList(),
    val localTracks:      List<LocalTrack>        = emptyList(),
    val myUploads:        List<MusicTrackRecord>  = emptyList(),
    val searchResults:    List<MusicTrackRecord>  = emptyList(),
    val searchQuery:      String                  = "",

    // Live listeners (stream tab)
    val liveListeners:    List<LiveListenerEntry> = emptyList(),

    // Playback
    val activeTrack:      ActiveTrack?  = null,
    val isPlaying:        Boolean       = false,
    val progressFraction: Float         = 0f,
    val positionMs:       Long          = 0L,

    // Playback modes
    val isShuffle:        Boolean       = false,
    val repeatMode:       RepeatMode    = RepeatMode.NONE,

    // Stats
    val stats:            MusicStats    = MusicStats(),

    // Sheets
    val shareSheet:       ShareMusicState     = ShareMusicState(),
    val commentsSheet:    CommentsSheetState  = CommentsSheetState(),

    // Settings
    val settings:         MusicSettings  = MusicSettings(),
    val showSettings:     Boolean        = false,

    // Download
    val downloadingTrackId:   String? = null,
    val downloadToastMessage: String? = null,

    // Permission
    val needsStoragePermission: Boolean = false,

    // Error
    val errorMessage: String? = null
)