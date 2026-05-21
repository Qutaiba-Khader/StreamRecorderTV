package com.streamrecorder.tvapp

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object ApiClient {
    private const val TAG = "StreamRecAPI"
    private const val LOCAL_BASE = "http://192.168.1.31:5001"
    private const val REMOTE_BASE = "https://strm-rec-h.websnake.org"

    private val localClient = OkHttpClient.Builder()
        .connectTimeout(2, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
    private val remoteClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    @Volatile private var activeBase: String? = null

    private val recordingsCache = ConcurrentHashMap<Int, List<Recording>>()

    private fun resolveBase(): String {
        activeBase?.let { return it }
        return try {
            val req = Request.Builder().url("$LOCAL_BASE/api/targets").build()
            localClient.newCall(req).execute().use { it.body!!.string() }
            LOCAL_BASE.also { activeBase = it }
        } catch (_: Exception) {
            REMOTE_BASE.also { activeBase = it }
        }
    }

    private fun get(path: String): String {
        val base = resolveBase()
        val client = if (base == LOCAL_BASE) localClient else remoteClient
        val req = Request.Builder().url("$base$path").build()
        return try {
            client.newCall(req).execute().use { it.body!!.string() }
        } catch (e: Exception) {
            if (base == LOCAL_BASE) {
                activeBase = REMOTE_BASE
                val req2 = Request.Builder().url("$REMOTE_BASE$path").build()
                remoteClient.newCall(req2).execute().use { it.body!!.string() }
            } else throw e
        }
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
                var watchPct = 0
                val wpObj = o.optJSONObject("watch_positions")
                if (wpObj != null) {
                    val keys = wpObj.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        val wp = wpObj.getJSONObject(k)
                        val pos = wp.optDouble("position", 0.0)
                        val dur = wp.optDouble("duration", 1.0)
                        if (dur > 0) {
                            val pct = ((pos / dur) * 100).toInt().coerceIn(0, 100)
                            if (pct > watchPct) watchPct = pct
                        }
                    }
                }
                Recording(
                    id = o.getInt("id"),
                    recordedAt = o.optString("recorded_at", ""),
                    duration = o.optInt("duration", 0),
                    durationHr = o.optString("duration_hr", ""),
                    sources = sourceList,
                    isFav = o.optBoolean("is_fav", false),
                    posterSmall270 = o.optString("poster_small_270", null).takeIf { !it.isNullOrEmpty() },
                    posterSmall192 = o.optString("poster_small_192", null).takeIf { !it.isNullOrEmpty() },
                    watchPercentage = watchPct
                )
            }
        }.also { recordingsCache[targetId] = it }
    }

    suspend fun loadLiveStream(slug: String): LiveStreamData? = withContext(Dispatchers.IO) {
        try {
            val encoded = URLEncoder.encode(slug, "UTF-8")
            val json = JSONObject(get("/api/live-stream/$encoded"))
            if (!json.optBoolean("is_live", false)) return@withContext null
            val title = json.optString("title", "$slug LIVE")
            val streamsObj = json.optJSONObject("streams")
            val streamUrls = mutableMapOf<String, String>()
            if (streamsObj != null) {
                val flv = streamsObj.optJSONObject("flv")
                if (flv != null) {
                    val keys = flv.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        streamUrls["FLV $k"] = flv.getString(k)
                    }
                }
                val hls = streamsObj.optJSONObject("hls")
                if (hls != null) {
                    val keys = hls.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        streamUrls["HLS $k"] = hls.getString(k)
                    }
                }
                val rtmp = streamsObj.optString("rtmp", null)
                if (!rtmp.isNullOrEmpty() && flv == null) {
                    streamUrls["RTMP"] = rtmp
                }
            }
            if (streamUrls.isEmpty()) return@withContext null
            LiveStreamData(isLive = true, title = title, streams = streamUrls)
        } catch (e: Exception) {
            Log.w(TAG, "loadLiveStream($slug) failed", e)
            null
        }
    }

    suspend fun toggleFav(recId: Int): Boolean = withContext(Dispatchers.IO) {
        val json = post("/api/fav", """{"rec_id":$recId}""")
        json.optBoolean("is_fav", false)
    }

    suspend fun hideSource(recId: Int, res: Int) = withContext(Dispatchers.IO) {
        post("/api/hide", """{"rec_id":$recId,"res":$res}""")
    }

    suspend fun saveWatchPosition(recId: Int, res: Int, positionMs: Long, durationMs: Long) = withContext(Dispatchers.IO) {
        try {
            val posSec = positionMs / 1000.0
            val durSec = durationMs / 1000.0
            post("/api/watch-position", """{"rec_id":$recId,"res":"$res","position":$posSec,"duration":$durSec}""")
        } catch (e: Exception) {
            Log.w(TAG, "saveWatchPosition failed", e)
        }
    }

    private fun post(path: String, jsonBody: String): JSONObject {
        val base = resolveBase()
        val client = if (base == LOCAL_BASE) localClient else remoteClient
        val body = okhttp3.RequestBody.create(null, jsonBody.toByteArray())
        val req = Request.Builder()
            .url("$base$path")
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()
        return try {
            client.newCall(req).execute().use { JSONObject(it.body!!.string()) }
        } catch (e: Exception) {
            if (base == LOCAL_BASE) {
                activeBase = REMOTE_BASE
                val req2 = Request.Builder()
                    .url("$REMOTE_BASE$path")
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build()
                remoteClient.newCall(req2).execute().use { JSONObject(it.body!!.string()) }
            } else throw e
        }
    }

    fun playUrl(recId: Int, res: Int): String = "${resolveBase()}/play/$recId?res=$res"

    fun clearCache() {
        recordingsCache.clear()
        activeBase = null
    }

    fun clearCacheFor(targetId: Int) {
        recordingsCache.remove(targetId)
    }
}
