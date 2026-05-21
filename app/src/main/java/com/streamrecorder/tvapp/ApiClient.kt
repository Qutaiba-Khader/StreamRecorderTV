package com.streamrecorder.tvapp

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object ApiClient {
    private const val TAG = "StreamRecAPI"
    private const val BASE = "http://192.168.1.31:5001"
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val recordingsCache = ConcurrentHashMap<Int, List<Recording>>()

    private fun get(path: String): String {
        val req = Request.Builder().url("$BASE$path").build()
        val resp = client.newCall(req).execute()
        return resp.use { it.body!!.string() }
    }

    suspend fun loadTargets(): List<Target> = withContext(Dispatchers.IO) {
        val json = JSONObject(get("/api/targets"))
        val arr = json.getJSONArray("targets")
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            Target(
                id = o.getInt("id"),
                name = o.getString("name"),
                platform = o.optString("platform", "tiktok"),
                countTotal = o.optInt("count_total", 0),
                isLive = o.optBoolean("is_live", false),
                logo = o.optString("logo", null),
                latestTs = o.optLong("latest_ts", 0)
            )
        }.sortedWith(compareByDescending<Target> { it.isLive }.thenByDescending { it.latestTs })
    }

    suspend fun loadRecordings(targetId: Int): List<Recording> {
        recordingsCache[targetId]?.let { return it }
        return withContext(Dispatchers.IO) {
            val json = JSONObject(get("/api/recordings/$targetId"))
            val arr = json.getJSONArray("data")
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.getJSONObject(i)
                if (o.optString("status") == "running") return@mapNotNull null
                val srcs = o.optJSONArray("sources") ?: return@mapNotNull null
                val sourceList = (0 until srcs.length()).map { j ->
                    val s = srcs.getJSONObject(j)
                    Source(
                        resolution = s.optInt("resolution", 0),
                        filesize = s.optLong("filesize", 0),
                        downloadlink = s.optString("downloadlink", "")
                    )
                }
                if (sourceList.isEmpty()) return@mapNotNull null
                Recording(
                    id = o.getInt("id"),
                    recordedAt = o.optString("recorded_at", ""),
                    duration = o.optInt("duration", 0),
                    durationHr = o.optString("duration_hr", ""),
                    sources = sourceList,
                    isFav = o.optBoolean("is_fav", false),
                    posterSmall270 = o.optString("poster_small_270", null).takeIf { !it.isNullOrEmpty() },
                    posterSmall192 = o.optString("poster_small_192", null).takeIf { !it.isNullOrEmpty() }
                )
            }
        }.also { recordingsCache[targetId] = it }
    }

    fun playUrl(recId: Int, res: Int): String = "$BASE/play/$recId?res=$res"

    fun clearCache() {
        recordingsCache.clear()
    }
}
