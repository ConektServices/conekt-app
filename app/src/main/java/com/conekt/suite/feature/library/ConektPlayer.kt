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

/**
 * App-scoped singleton music player.
 *
 * Two modes:
 *  1. Standalone (used before the service starts): creates its own ExoPlayer.
 *  2. Service-backed (preferred): MusicPlaybackService creates the ExoPlayer and
 *     calls [attachServicePlayer]; this instance then becomes a thin wrapper.
 *
 * The ViewModel always talks to THIS singleton, never directly to ExoPlayer.
 */
object ConektPlayer {

    data class State(
        val isPlaying:    Boolean = false,
        val durationMs:   Long    = 0L,
        val positionMs:   Long    = 0L,
        val currentUri:   String? = null,
        val trackTitle:   String  = "",
        val trackArtist:  String  = ""
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    // The actual player – may be owned by us or by the service.
    private var player: ExoPlayer? = null
    private var ownedByService = false

    // ── Service integration ───────────────────────────────────────────────────

    /** Called by MusicPlaybackService.onCreate() */
    fun attachServicePlayer(servicePlayer: ExoPlayer) {
        // Release our own player if we had one
        if (!ownedByService) {
            player?.release()
        }
        player = servicePlayer
        ownedByService = true
    }

    /** Called by MusicPlaybackService.onDestroy() */
    fun detachServicePlayer() {
        player = null
        ownedByService = false
    }

    /** Called by the service listener to keep state in sync */
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

    // ── Fallback init (standalone, no service) ────────────────────────────────

    fun init(context: Context) {
        if (player != null) return
        player = ExoPlayer.Builder(context.applicationContext).build().also { p ->
            p.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _state.value = _state.value.copy(isPlaying = isPlaying)
                }
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_READY) {
                        _state.value = _state.value.copy(durationMs = p.duration.coerceAtLeast(0L))
                    }
                }
            })
        }
        ownedByService = false
    }

    // ── Playback controls ─────────────────────────────────────────────────────

    fun play(uri: String, title: String = "", artist: String = "") {
        val p = player ?: return
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
            isPlaying   = true
        )
    }

    /**
     * Reliable toggle: reads the PLAYER'S actual state, not our cached state,
     * to avoid the "button shows wrong icon" bug.
     */
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

    /** Returns the TRUE playing state from the player, not the cached value. */
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

    fun skipToNext(queue: List<String>) {
        val cur = _state.value.currentUri ?: return
        val idx = queue.indexOf(cur)
        if (idx >= 0 && idx + 1 < queue.size) {
            // caller handles the actual play() call with metadata
        }
    }

    fun currentPositionFraction(): Float {
        val p   = player ?: return 0f
        val dur = p.duration
        return if (dur <= 0L) 0f else (p.currentPosition.toFloat() / dur).coerceIn(0f, 1f)
    }

    fun currentPositionMs(): Long = player?.currentPosition ?: 0L

    fun release() {
        if (!ownedByService) {
            player?.release()
        }
        player = null
    }

    // ── Queue support ─────────────────────────────────────────────────────────

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
