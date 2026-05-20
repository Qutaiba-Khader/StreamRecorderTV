package com.streamrecorder.core.api

import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private val gson = GsonBuilder().create()

    val serverDetector = ServerDetector(okHttpClient)

    private var currentBaseUrl: String? = null
    private var api: StreamRecorderApi? = null

    fun getApi(baseUrl: String): StreamRecorderApi {
        if (baseUrl != currentBaseUrl || api == null) {
            api = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
                .create(StreamRecorderApi::class.java)
            currentBaseUrl = baseUrl
        }
        return api!!
    }

    suspend fun getDetectedApi(): StreamRecorderApi {
        val url = serverDetector.detectedBaseUrl ?: serverDetector.detect()
        return getApi(url)
    }
}
