package com.conekt.suite.feature.library

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.conekt.suite.data.model.LocalTrack
import com.conekt.suite.data.model.MusicStats
import com.conekt.suite.data.model.MusicTrackRecord
import com.conekt.suite.data.repository.MusicRepository
import com.conekt.suite.data.repository.MusicSettingsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MusicViewModel(app: Application) : AndroidViewModel(app) {

    private val repo         = MusicRepository()
    private val settingsRepo = MusicSettingsRepository(app)

    private val _uiState = MutableStateFlow(MusicUiState())
    val uiState: StateFlow<MusicUiState> = _uiState.asStateFlow()

    private var progressJob: Job? = null
    private var playStartMs: Long = 0L
    private var livePresenceJob: Job? = null

    init {
        ConektPlayer.init(app)
        loadOnlineTracks()
        loadStats()
        scanLocalTracks()
        observePlayerState()
        loadSettings()
        startLiveListenersPoll()
    }

    // ── Settings ──────────────────────────────────────────────────────────────

    private fun loadSettings() {
        viewModelScope.launch {
            val s = settingsRepo.load()
            update { copy(settings = s) }
        }
    }

    fun onSettingsChange(settings: MusicSettings) {
        update { copy(settings = settings) }
        viewModelScope.launch { settingsRepo.save(settings) }
    }

    // ── Init / load ───────────────────────────────────────────────────────────

    private fun loadOnlineTracks() {
        viewModelScope.launch {
            update { copy(isLoadingOnline = true) }
            val tracks  = safeList { repo.fetchPublicTracks() }
            val uploads = safeList { repo.fetchMyUploads() }
            update { copy(isLoadingOnline = false, onlineTracks = tracks, myUploads = uploads) }
        }
    }

    private fun loadStats() {
        viewModelScope.launch {
            update { copy(isLoadingStats = true) }
            val stats = safeGet { repo.getMyStats() }
            update { copy(isLoadingStats = false, stats = stats ?: MusicStats()) }
        }
    }

    fun scanLocalTracks() {
        viewModelScope.launch {
            update { copy(isLoadingLocal = true) }
            val tracks = try {
                repo.scanLocalTracks(getApplication())
            } catch (e: Exception) {
                update { copy(needsStoragePermission = true, isLoadingLocal = false) }
                return@launch
            }
            update { copy(localTracks = tracks, isLoadingLocal = false) }
        }
    }

    private fun observePlayerState() {
        viewModelScope.launch {
            ConektPlayer.state.collect { ps ->
                update { copy(isPlaying = ps.isPlaying) }
            }
        }
    }

    // ── Live listeners poll ───────────────────────────────────────────────────

    private fun startLiveListenersPoll() {
        livePresenceJob?.cancel()
        livePresenceJob = viewModelScope.launch {
            while (true) {
                val listeners = safeList { repo.fetchLiveListeners() }
                update { copy(liveListeners = listeners) }
                delay(15_000)
            }
        }
    }

    // ── Playback ──────────────────────────────────────────────────────────────

    fun playLocal(track: LocalTrack) {
        val active = ActiveTrack(
            id         = null,
            title      = track.title,
            artist     = track.artist,
            album      = track.album,
            uri        = track.uri,
            coverUri   = track.albumArtUri,
            durationMs = track.duration,
            source     = MusicSource.LOCAL
        )
        ConektPlayer.play(track.uri, track.title, track.artist)
        update { copy(activeTrack = active, isPlaying = true, progressFraction = 0f) }
        startProgressTracking()
        playStartMs = System.currentTimeMillis()
        val settings = _uiState.value.settings
        if (settings.shareLocalActivity) {
            viewModelScope.launch { safeGet { repo.broadcastLocalPlay(track.title, track.artist) } }
        }
    }

    fun playOnline(track: MusicTrackRecord) {
        val active = ActiveTrack(
            id         = track.id,
            title      = track.title,
            artist     = track.artist,
            album      = track.album ?: "",
            uri        = track.fileUrl,
            coverUri   = track.coverUrl,
            durationMs = track.durationMs.toLong(),
            source     = MusicSource.ONLINE
        )
        ConektPlayer.play(track.fileUrl, track.title, track.artist)
        update { copy(activeTrack = active, isPlaying = true, progressFraction = 0f) }
        startProgressTracking()
        playStartMs = System.currentTimeMillis()
        viewModelScope.launch { repo.joinListener(track.id) }
    }

    fun togglePlayPause() {
        ConektPlayer.togglePlayPause()
        val nowPlaying = ConektPlayer.isActuallyPlaying
        update { copy(isPlaying = nowPlaying) }
        if (!nowPlaying) recordCurrentPlay(completed = false)
    }

    fun seekTo(fraction: Float) {
        ConektPlayer.seekTo(fraction)
        update { copy(progressFraction = fraction) }
    }

    fun skipNext(queue: List<ActiveTrack>) {
        val current = _uiState.value.activeTrack ?: return
        val idx = queue.indexOfFirst { it.uri == current.uri }
        if (idx >= 0 && idx + 1 < queue.size) {
            val next = queue[idx + 1]
            if (next.source == MusicSource.LOCAL) {
                playLocal(LocalTrack(0, next.title, next.artist, next.album, next.durationMs, next.uri, next.coverUri))
            } else {
                _uiState.value.onlineTracks.find { it.id == next.id }?.let { playOnline(it) }
            }
        }
    }

    fun skipPrevious(queue: List<ActiveTrack>) {
        val current = _uiState.value.activeTrack ?: return
        val idx = queue.indexOfFirst { it.uri == current.uri }
        if (idx > 0) {
            val prev = queue[idx - 1]
            if (prev.source == MusicSource.LOCAL) {
                playLocal(LocalTrack(0, prev.title, prev.artist, prev.album, prev.durationMs, prev.uri, prev.coverUri))
            } else {
                _uiState.value.onlineTracks.find { it.id == prev.id }?.let { playOnline(it) }
            }
        }
    }

    fun buildQueue(): List<ActiveTrack> {
        val online = _uiState.value.onlineTracks.map { t ->
            ActiveTrack(t.id, t.title, t.artist, t.album ?: "", t.fileUrl, t.coverUrl, t.durationMs.toLong(), MusicSource.ONLINE)
        }
        val local = _uiState.value.localTracks.map { t ->
            ActiveTrack(null, t.title, t.artist, t.album, t.uri, t.albumArtUri, t.duration, MusicSource.LOCAL)
        }
        return online + local
    }

    private fun startProgressTracking() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (true) {
                delay(500)
                val fraction = ConektPlayer.currentPositionFraction()
                val posMs    = ConektPlayer.currentPositionMs()
                update { copy(progressFraction = fraction, positionMs = posMs) }
            }
        }
    }

    private fun recordCurrentPlay(completed: Boolean) {
        val track = _uiState.value.activeTrack ?: return
        if (track.id == null || track.source != MusicSource.ONLINE) return
        val durationS = ((System.currentTimeMillis() - playStartMs) / 1000).toInt()
        viewModelScope.launch { repo.recordPlay(track.id, durationS, completed, "stream") }
    }

    // ── Download ──────────────────────────────────────────────────────────────

    fun downloadTrack(liveEntry: LiveListenerEntry) {
        viewModelScope.launch {
            update { copy(downloadingTrackId = liveEntry.trackId) }
            val result = safeGet {
                repo.downloadTrackToLocal(
                    context  = getApplication(),
                    trackId  = liveEntry.trackId,
                    fileUrl  = liveEntry.trackFileUrl,
                    title    = liveEntry.trackTitle,
                    artist   = liveEntry.artistName
                )
            }
            update { copy(downloadingTrackId = null, downloadToastMessage = if (result == true) "Downloaded to your library!" else "Download failed") }
        }
    }

    fun clearDownloadToast() = update { copy(downloadToastMessage = null) }

    // ── Search ────────────────────────────────────────────────────────────────

    fun onSearchQueryChange(q: String) {
        update { copy(searchQuery = q) }
        if (q.isBlank()) { update { copy(searchResults = emptyList()) }; return }
        viewModelScope.launch {
            val results = safeList { repo.searchTracks(q) }
            update { copy(searchResults = results) }
        }
    }

    // ── Share sheet ───────────────────────────────────────────────────────────

    fun openShareSheet(localTrack: LocalTrack) {
        update { copy(shareSheet = ShareMusicState(visible = true, localTrack = localTrack, title = localTrack.title, artist = localTrack.artist)) }
    }

    fun closeShareSheet()              = update { copy(shareSheet = ShareMusicState()) }
    fun onShareTitleChange(v: String)  = updateShare { copy(title = v) }
    fun onShareArtistChange(v: String) = updateShare { copy(artist = v) }
    fun onShareGenreChange(v: String)  = updateShare { copy(genre = v) }
    fun onShareCoverPicked(uri: Uri)   = updateShare { copy(coverUri = uri) }
    fun onSharePublicToggle()          = updateShare { copy(isPublic = !isPublic) }

    fun submitShare() {
        val sheet = _uiState.value.shareSheet
        val local = sheet.localTrack ?: return
        if (sheet.title.isBlank()) { updateShare { copy(errorMessage = "Title is required.") }; return }
        updateShare { copy(isUploading = true, errorMessage = null) }
        viewModelScope.launch {
            runCatching {
                repo.uploadTrack(
                    context    = getApplication(),
                    audioUri   = Uri.parse(local.uri),
                    coverUri   = sheet.coverUri,
                    title      = sheet.title,
                    artist     = sheet.artist,
                    album      = null,
                    genre      = sheet.genre.ifBlank { null },
                    durationMs = local.duration.toInt(),
                    isPublic   = sheet.isPublic
                )
            }.onSuccess { loadOnlineTracks(); updateShare { copy(isUploading = false, isSuccess = true) } }
                .onFailure { e -> updateShare { copy(isUploading = false, errorMessage = e.message ?: "Upload failed.") } }
        }
    }

    // ── Comments sheet ────────────────────────────────────────────────────────

    fun openComments(trackId: String) {
        update { copy(commentsSheet = CommentsSheetState(visible = true, trackId = trackId, isLoading = true)) }
        viewModelScope.launch {
            val comments = safeList { repo.fetchComments(trackId) }
            update { copy(commentsSheet = _uiState.value.commentsSheet.copy(comments = comments, isLoading = false)) }
        }
    }

    fun closeComments() = update { copy(commentsSheet = CommentsSheetState()) }

    fun onCommentDraftChange(v: String) {
        update { copy(commentsSheet = _uiState.value.commentsSheet.copy(draftText = v)) }
    }

    fun postComment() {
        val sheet   = _uiState.value.commentsSheet
        val trackId = sheet.trackId ?: return
        val text    = sheet.draftText.trim()
        if (text.isBlank()) return
        update { copy(commentsSheet = sheet.copy(isPosting = true)) }
        viewModelScope.launch {
            runCatching { repo.postComment(trackId, text) }
                .onSuccess {
                    val updated = safeList { repo.fetchComments(trackId) }
                    update { copy(commentsSheet = _uiState.value.commentsSheet.copy(isPosting = false, draftText = "", comments = updated)) }
                }
                .onFailure { update { copy(commentsSheet = _uiState.value.commentsSheet.copy(isPosting = false)) } }
        }
    }

    // ── Permission ────────────────────────────────────────────────────────────

    fun onPermissionGranted() {
        update { copy(needsStoragePermission = false) }
        scanLocalTracks()
    }

    // ── Settings ──────────────────────────────────────────────────────────────

    fun openSettings()  = update { copy(showSettings = true) }
    fun closeSettings() = update { copy(showSettings = false) }

    // ── Refresh ───────────────────────────────────────────────────────────────

    fun refresh() {
        loadOnlineTracks()
        loadStats()
        scanLocalTracks()
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun update(block: MusicUiState.() -> MusicUiState) {
        _uiState.value = _uiState.value.block()
    }

    private fun updateShare(block: ShareMusicState.() -> ShareMusicState) {
        _uiState.value = _uiState.value.copy(shareSheet = _uiState.value.shareSheet.block())
    }

    private suspend fun <T> safeGet(block: suspend () -> T): T? = try { block() } catch (_: Exception) { null }
    private suspend fun <T> safeList(block: suspend () -> List<T>): List<T> = try { block() } catch (_: Exception) { emptyList() }

    override fun onCleared() {
        super.onCleared()
        progressJob?.cancel()
        livePresenceJob?.cancel()
        val track = _uiState.value.activeTrack
        if (track?.id != null && track.source == MusicSource.ONLINE) {
            viewModelScope.launch { repo.leaveListener(track.id) }
        }
    }

    // ── Factory — required for AndroidViewModel with by viewModels() ──────────
    companion object {
        val Factory: ViewModelProvider.Factory = ViewModelProvider.AndroidViewModelFactory()
    }
}