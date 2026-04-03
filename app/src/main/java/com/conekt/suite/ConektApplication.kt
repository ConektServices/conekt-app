package com.conekt.suite

import android.app.Application
import com.conekt.suite.feature.library.ConektPlayer

class ConektApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ConektPlayer.init(this)
    }
}