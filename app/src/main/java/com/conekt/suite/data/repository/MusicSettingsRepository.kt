package com.conekt.suite.data.repository

import android.content.Context
import com.conekt.suite.feature.library.MusicSettings

class MusicSettingsRepository(private val context: Context) {

    private val prefs by lazy {
        context.getSharedPreferences("conekt_music_settings", Context.MODE_PRIVATE)
    }

    fun load(): MusicSettings = MusicSettings(
        shareListeningActivity = prefs.getBoolean("share_listening",    true),
        shareLocalActivity     = prefs.getBoolean("share_local",        false),
        allowDownloads         = prefs.getBoolean("allow_downloads",    true),
        autoplay               = prefs.getBoolean("autoplay",           true),
        highQualityStream      = prefs.getBoolean("hq_stream",          false),
        crossfadeSeconds       = prefs.getInt("crossfade",              0),
        showEqualizer          = prefs.getBoolean("show_equalizer",     true),
        liveNotifications      = prefs.getBoolean("live_notifications", true)
    )

    fun save(s: MusicSettings) {
        prefs.edit()
            .putBoolean("share_listening",    s.shareListeningActivity)
            .putBoolean("share_local",        s.shareLocalActivity)
            .putBoolean("allow_downloads",    s.allowDownloads)
            .putBoolean("autoplay",           s.autoplay)
            .putBoolean("hq_stream",          s.highQualityStream)
            .putInt("crossfade",              s.crossfadeSeconds)
            .putBoolean("show_equalizer",     s.showEqualizer)
            .putBoolean("live_notifications", s.liveNotifications)
            .apply()
    }
}
