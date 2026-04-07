package com.conekt.suite.feature.library

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class RepeatMode { NONE, ONE, ALL }

object ConektPlayer {

    data class State(
        val isPlaying:    Boolean    = false,
        val durationMs:   Long       = 0L,
        val positionMs:   Long       = 0L,
        val currentUri:   String?    = null,
        val trackTitle:   String     = "",
        val trackArtist:  String     = "",
        val trackEnded:   Boolean    = false   // pulses true when a track finishes naturally
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    // Called by MusicViewModel when it has consumed the trackEnded signal
    fun clearTrackEnded() {
        if (_state.value.trackEnded) _state.value = _state.value.copy(trackEnded = false)
    }

    // ── Player ref ────────────────────────────────────────────────────────────

    private var player: ExoPlayer? = null
    private var ownedByService = false

    // ── Service integration ───────────────────────────────────────────────────

    fun attachServicePlayer(servicePlayer: ExoPlayer) {
        if (!ownedByService) player?.release()
        player = servicePlayer
        ownedByService = true
        attachListener(servicePlayer)
    }

    fun detachServicePlayer() {
        player = null
        ownedByService = false
    }

    fun syncFromService(isPlaying: Boolean, positionMs: Long, durationMs: Long) {
        _state.value = _state.value.copy(
            isPlaying  = isPlaying,
            positionMs = positionMs.coerceAtLeast(0L),
            durationMs = durationMs.coerceAtLeast(0L)
        )
    }

    fun syncDuration(durationMs: Long) {
        _state.value = _state.value.copy(durationMs = durationMs.coerceAtLeast(0L))
    }

    // ── Standalone init ───────────────────────────────────────────────────────

    fun init(context: Context) {
        if (player != null) return
        val exo = ExoPlayer.Builder(context.applicationContext).build()
        attachListener(exo)
        player = exo
        ownedByService = false
    }

    // ── Shared listener (standalone + service) ────────────────────────────────

    private fun attachListener(exo: ExoPlayer) {
        exo.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _state.value = _state.value.copy(isPlaying = isPlaying)
            }

            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_READY -> {
                        _state.value = _state.value.copy(
                            durationMs = exo.duration.coerceAtLeast(0L)
                        )
                    }
                    Player.STATE_ENDED -> {
                        // Signal the ViewModel to advance the queue
                        _state.value = _state.value.copy(
                            isPlaying  = false,
                            trackEnded = true
                        )
                    }
                    else -> Unit
                }
            }
        })
    }

    // ── Playback controls ─────────────────────────────────────────────────────

    fun play(uri: String, title: String = "", artist: String = "") {
        val p = player ?: return
        // Clear any previous ended signal before starting new track
        _state.value = _state.value.copy(trackEnded = false)
        val mediaItem = MediaItem.Builder()
            .setUri(Uri.parse(uri))
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .build()
            )
            .build()
        p.setMediaItem(mediaItem)
        p.prepare()
        p.play()
        _state.value = _state.value.copy(
            currentUri  = uri,
            trackTitle  = title,
            trackArtist = artist,
            isPlaying   = true,
            trackEnded  = false
        )
    }

    fun togglePlayPause() {
        val p = player ?: return
        if (p.isPlaying) {
            p.pause()
            _state.value = _state.value.copy(isPlaying = false)
        } else {
            p.play()
            _state.value = _state.value.copy(isPlaying = true)
        }
    }

    val isActuallyPlaying: Boolean get() = player?.isPlaying == true

    fun seekTo(fraction: Float) {
        val p   = player ?: return
        val dur = p.duration.takeIf { it > 0 } ?: return
        val pos = (fraction * dur).toLong().coerceAtLeast(0L)
        p.seekTo(pos)
        _state.value = _state.value.copy(positionMs = pos)
    }

    fun seekToMs(ms: Long) {
        player?.seekTo(ms)
        _state.value = _state.value.copy(positionMs = ms)
    }

    fun currentPositionFraction(): Float {
        val p   = player ?: return 0f
        val dur = p.duration
        return if (dur <= 0L) 0f else (p.currentPosition.toFloat() / dur).coerceIn(0f, 1f)
    }

    fun currentPositionMs(): Long = player?.currentPosition ?: 0L

    fun release() {
        if (!ownedByService) player?.release()
        player = null
    }

    // Legacy queue helpers kept for compatibility
    fun next(items: List<Pair<String, String>>, currentUri: String) {
        val idx = items.indexOfFirst { it.first == currentUri }
        if (idx >= 0 && idx + 1 < items.size) {
            val (uri, title) = items[idx + 1]
            play(uri, title, "")
        }
    }

    fun previous(items: List<Pair<String, String>>, currentUri: String) {
        val idx = items.indexOfFirst { it.first == currentUri }
        if (idx > 0) {
            val (uri, title) = items[idx - 1]
            play(uri, title, "")
        }
    }
}