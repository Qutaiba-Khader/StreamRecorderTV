package com.streamrecorder.tv.di

import android.content.Context
import com.streamrecorder.core.cache.CacheManager
import com.streamrecorder.core.player.PlayerLauncher
import com.streamrecorder.core.repository.StreamerRepository

class AppContainer(context: Context) {
    val cache = CacheManager(context)
    val repository = StreamerRepository(cache)
    val playerLauncher = PlayerLauncher(context)
}
