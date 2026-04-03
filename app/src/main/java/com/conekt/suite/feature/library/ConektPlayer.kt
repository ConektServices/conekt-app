package com.conekt.suite.feature.library

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * App-scoped singleton music player wrapping Media3 ExoPlayer.
 * Call [init] once (e.g. in Application.onCreate or before first use).
 */
object ConektPlayer {

    data class State(
        val isPlaying: Boolean    = false,
        val durationMs: Long      = 0L,
        val positionMs: Long      = 0L,
        val currentUri: String?   = null,
        val trackTitle: String    = "",
        val trackArtist: String   = ""
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private var player: ExoPlayer? = null
    private var progressJob: kotlinx.coroutines.Job? = null

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
    }

    /** Play a URI (content:// for local, https:// for remote). */
    fun play(uri: String, title: String = "", artist: String = "") {
        val p = player ?: return
        val item = MediaItem.fromUri(Uri.parse(uri))
        p.setMediaItem(item)
        p.prepare()
        p.play()
        _state.value = _state.value.copy(
            currentUri  = uri,
            trackTitle  = title,
            trackArtist = artist,
            isPlaying   = true
        )
    }

    fun togglePlayPause() {
        val p = player ?: return
        if (p.isPlaying) p.pause() else p.play()
    }

    fun seekTo(fraction: Float) {
        val p = player ?: return
        val pos = (fraction * p.duration).toLong().coerceAtLeast(0L)
        p.seekTo(pos)
    }

    fun next(items: List<Pair<String, String>>, currentUri: String) {
        val idx = items.indexOfFirst { it.first == currentUri }
        if (idx >= 0 && idx + 1 < items.size) {
            val next = items[idx + 1]
            play(next.first, next.second, "")
        }
    }

    fun previous(items: List<Pair<String, String>>, currentUri: String) {
        val idx = items.indexOfFirst { it.first == currentUri }
        if (idx > 0) {
            val prev = items[idx - 1]
            play(prev.first, prev.second, "")
        }
    }

    fun currentPositionFraction(): Float {
        val p = player ?: return 0f
        val dur = p.duration
        return if (dur <= 0L) 0f else (p.currentPosition.toFloat() / dur).coerceIn(0f, 1f)
    }

    fun currentPositionMs(): Long = player?.currentPosition ?: 0L

    fun release() {
        player?.release()
        player = null
    }
}