package com.streamrecorder.core.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class ServerDetector(private val client: OkHttpClient) {

    companion object {
        const val LAN_URL = "http://192.168.1.31:5001/"
        const val PUBLIC_URL = "https://strm-rec-h.websnake.org/"
        private const val PING_TIMEOUT_MS = 2000L
    }

    enum class Mode { AUTO, LAN_ONLY, PUBLIC_ONLY }

    var mode: Mode = Mode.AUTO
    var detectedBaseUrl: String? = null
        private set

    suspend fun detect(): String {
        return when (mode) {
            Mode.LAN_ONLY -> LAN_URL.also { detectedBaseUrl = it }
            Mode.PUBLIC_ONLY -> PUBLIC_URL.also { detectedBaseUrl = it }
            Mode.AUTO -> autoDetect()
        }
    }

    private suspend fun autoDetect(): String {
        val lanReachable = withContext(Dispatchers.IO) {
            withTimeoutOrNull(PING_TIMEOUT_MS) {
                try {
                    val pingClient = client.newBuilder()
                        .connectTimeout(PING_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                        .readTimeout(PING_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                        .build()
                    val request = Request.Builder()
                        .url("${LAN_URL}api/targets")
                        .head()
                        .build()
                    pingClient.newCall(request).execute().use { it.isSuccessful }
                } catch (_: Exception) {
                    false
                }
            } ?: false
        }
        val url = if (lanReachable) LAN_URL else PUBLIC_URL
        detectedBaseUrl = url
        return url
    }
}
