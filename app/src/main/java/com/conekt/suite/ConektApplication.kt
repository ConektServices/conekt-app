package com.conekt.suite

import android.app.Application
import android.content.Intent
import com.conekt.suite.feature.library.ConektPlayer
import com.conekt.suite.feature.library.MusicPlaybackService

class ConektApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize the standalone player as a fallback.
        // The MusicPlaybackService will call attachServicePlayer() when it starts,
        // taking over from the standalone instance.
        ConektPlayer.init(this)
    }
}
