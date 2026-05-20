package com.streamrecorder.tv

import android.app.Application
import com.streamrecorder.tv.di.AppContainer

class StreamRecorderApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
