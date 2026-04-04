package com.conekt.suite.feature.library

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.conekt.suite.MainActivity

/**
 * Foreground service that keeps ExoPlayer alive and exposes a MediaSession
 * so the system notification bar shows playback controls.
 *
 * Register in AndroidManifest.xml:
 *
 * <service
 *     android:name=".feature.library.MusicPlaybackService"
 *     android:foregroundServiceType="mediaPlayback"
 *     android:exported="true">
 *   <intent-filter>
 *     <action android:name="androidx.media3.session.MediaSessionService" />
 *   </intent-filter>
 * </service>
 *
 * Also add permissions:
 * <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
 * <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
 */
class MusicPlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var player: ExoPlayer? = null

    companion object {
        const val CHANNEL_ID = "conekt_music_channel"
        const val NOTIFICATION_ID = 1001

        // Actions for notification buttons
        const val ACTION_PLAY_PAUSE = "com.conekt.suite.PLAY_PAUSE"
        const val ACTION_NEXT       = "com.conekt.suite.NEXT"
        const val ACTION_PREV       = "com.conekt.suite.PREV"
        const val ACTION_STOP       = "com.conekt.suite.STOP"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                /* handleAudioFocus= */ true
            )
            .setHandleAudioBecomingNoisy(true) // auto-pause on headphone unplug
            .build().also { exo ->
                exo.addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        ConektPlayer.syncFromService(isPlaying, exo.currentPosition, exo.duration)
                    }
                    override fun onPlaybackStateChanged(state: Int) {
                        if (state == Player.STATE_READY) {
                            ConektPlayer.syncDuration(exo.duration)
                        }
                    }
                })
            }

        // Hand our player to the singleton so the ViewModel can control it
        ConektPlayer.attachServicePlayer(player!!)

        val activityIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        mediaSession = MediaSession.Builder(this, player!!)
            .setSessionActivity(activityIntent)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onDestroy() {
        ConektPlayer.detachServicePlayer()
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        player = null
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Conekt Music",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Music playback controls"
                setShowBadge(false)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }
}
