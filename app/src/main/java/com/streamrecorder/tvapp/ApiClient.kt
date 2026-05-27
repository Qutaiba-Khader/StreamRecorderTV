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
    private const val LOCAL_BASE = "http://192.168.1.31:5001"
    private const val REMOTE_BASE = "https://strm-rec-h.websnake.org"

    private val localClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(2, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
    }
    private val remoteClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    @Volatile private var activeBase: String? = null

    private val recordingsCache = ConcurrentHashMap<Int, List<Recording>>()

    var cachedTargetsJson: String? = null
        private set

    private fun get(path: String): String {
        val base = activeBase
        if (base != null) {
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
        return try {
            val req = Request.Builder().url("$LOCAL_BASE$path").build()
            localClient.newCall(req).execute().use { it.body!!.string() }
                .also { activeBase = LOCAL_BASE }
        } catch (_: Exception) {
            activeBase = REMOTE_BASE
            val req = Request.Builder().url("$REMOTE_BASE$path").build()
            remoteClient.newCall(req).execute().use { it.body!!.string() }
        }
    }

    suspend fun loadTargets(): List<Target> = withContext(Dispatchers.IO) {
        val raw = get("/api/targets")
        parseTargets(raw).also { cachedTargetsJson = raw }
    }

    fun parseTargetsFromJson(raw: String): List<Target> = parseTargets(raw)

    private fun parseTargets(raw: String): List<Target> {
        val json = JSONObject(raw)
        val arr = json.getJSONArray("targets")
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            Target(
                id = o.getInt("id"),
                name = o.getString("name"),
                platform = o.optString("platform", "tiktok"),
                countTotal = o.optInt("count_total", 0),
                isLive = o.optBoolean("is_live", false),
                isPostprocessing = o.optBoolean("is_postprocessing", false),
                logo = o.optString("logo", null),
                latestTs = o.optLong("latest_ts", 0)
            )
        }.sortedWith(
            compareByDescending<Target> { AppPreferences.isPinned(it.id) }
                .thenByDescending { it.isLive }
                .thenByDescending { it.isPostprocessing }
                .thenByDescending { it.latestTs }
        )
    }

    suspend fun loadRecordings(targetId: Int): List<Recording> {
        recordingsCache[targetId]?.let { return it }
        return withContext(Dispatchers.IO) {
            val raw = get("/api/recordings/$targetId")
            parseRecordings(raw)
        }.also { recordingsCache[targetId] = it }
    }

    private fun parseRecordings(raw: String): List<Recording> {
        val json = JSONObject(raw)
        val arr = json.getJSONArray("data")
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.getJSONObject(i)
            val status = o.optString("status", "")
            if (status == "running" || status == "postprocessing") return@mapNotNull null
            val srcs = o.optJSONArray("sources") ?: return@mapNotNull null
            val sourceList = (0 until srcs.length()).map { j ->
                val s = srcs.getJSONObject(j)
                Source(
                    resolution = s.optInt("resolution", 0),
                    filesize = s.optLong("filesize", 0),
                    downloadlink = s.optString("downloadlink", ""),
                    deletionTimeHr = s.optString("deletiontime_hr", null).takeIf { !it.isNullOrEmpty() },
                    deletionTime = s.optLong("deletiontime", 0)
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
                thumbLarge = o.optString("thumb_large", null).takeIf { !it.isNullOrEmpty() },
                maxViewerCount = o.optInt("max_viewercount", 0),
                watchPercentage = watchPct
            )
        }
    }

    suspend fun loadRecordingsRaw(targetId: Int): String = withContext(Dispatchers.IO) {
        get("/api/recordings/$targetId")
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
            Log.w("StreamRecAPI", "loadLiveStream($slug) failed", e)
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

    suspend fun unhideSource(recId: Int, res: Int) = withContext(Dispatchers.IO) {
        post("/api/unhide", """{"rec_id":$recId,"res":$res}""")
    }

    suspend fun saveWatchPosition(recId: Int, res: Int, positionMs: Long, durationMs: Long) = withContext(Dispatchers.IO) {
        try {
            val posSec = positionMs / 1000.0
            val durSec = durationMs / 1000.0
            post("/api/watch-position", """{"rec_id":$recId,"res":"$res","position":$posSec,"duration":$durSec}""")
        } catch (_: Exception) {}
    }

    private fun post(path: String, jsonBody: String): JSONObject {
        val base = activeBase ?: LOCAL_BASE
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

    fun playUrl(recId: Int, res: Int): String {
        val base = activeBase ?: LOCAL_BASE
        return "$base/play/$recId?res=$res"
    }

    fun recoPlayUrl(user: String, filename: String): String {
        val base = activeBase ?: LOCAL_BASE
        return "$base/reco/play/${URLEncoder.encode(user, "UTF-8").replace("+", "%20")}/${URLEncoder.encode(filename, "UTF-8").replace("+", "%20")}"
    }

    fun recoThumbUrl(relativePath: String): String {
        val base = activeBase ?: LOCAL_BASE
        return "$base$relativePath"
    }

    suspend fun deleteRecoFile(user: String, filename: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply { put("user", user); put("filename", filename) }
            val json = post("/api/reco/delete", body.toString())
            json.optBoolean("ok", false)
        } catch (_: Exception) { false }
    }

    suspend fun loadSettings(): JSONObject = withContext(Dispatchers.IO) {
        try {
            JSONObject(get("/api/settings"))
        } catch (_: Exception) { JSONObject() }
    }

    suspend fun saveSettings(settings: JSONObject) = withContext(Dispatchers.IO) {
        try {
            post("/api/settings", settings.toString())
        } catch (_: Exception) {}
    }

    suspend fun startDownload(recId: Int, res: Int): String = withContext(Dispatchers.IO) {
        try {
            val json = post("/api/download", """{"rec_id":$recId,"res":$res}""")
            json.optString("status", "error")
        } catch (_: Exception) { "error" }
    }

    suspend fun checkDownload(recId: Int, res: Int): JSONObject = withContext(Dispatchers.IO) {
        try {
            post("/api/download/check", """{"rec_id":$recId,"res":$res}""")
        } catch (_: Exception) { JSONObject().apply { put("status", "error") } }
    }

    suspend fun loadReco(): Map<String, List<RecoFile>> = withContext(Dispatchers.IO) {
        try {
            val raw = get("/api/reco")
            val json = JSONObject(raw)
            val result = linkedMapOf<String, List<RecoFile>>()
            val keys = json.keys()
            while (keys.hasNext()) {
                val user = keys.next()
                val arr = json.getJSONArray(user)
                val files = (0 until arr.length()).map { i ->
                    val o = arr.getJSONObject(i)
                    RecoFile(
                        filename = o.getString("filename"),
                        size = o.optLong("size", 0),
                        resolution = if (o.isNull("resolution")) null else o.optInt("resolution"),
                        date = o.optString("date", null).takeIf { it != "null" && !it.isNullOrEmpty() },
                        user = o.optString("user", user),
                        thumbUrl = o.optString("thumb_url", null).takeIf { !it.isNullOrEmpty() },
                        playUrl = o.optString("play_url", null).takeIf { !it.isNullOrEmpty() },
                        isFav = o.optBoolean("is_fav", false),
                        watchPct = o.optInt("watch_pct", 0)
                    )
                }
                result[user] = files
            }
            result
        } catch (_: Exception) { emptyMap() }
    }

    suspend fun saveRecoWatchPosition(user: String, filename: String, positionMs: Long, durationMs: Long) = withContext(Dispatchers.IO) {
        try {
            val posSec = positionMs / 1000.0
            val durSec = durationMs / 1000.0
            val body = JSONObject().apply { put("user", user); put("filename", filename); put("position", posSec); put("duration", durSec) }
            post("/api/reco/watch-position", body.toString())
        } catch (_: Exception) {}
    }

    suspend fun deleteRecoWatchPosition(user: String, filename: String) = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply { put("user", user); put("filename", filename) }
            post("/api/reco/watch-position/delete", body.toString())
        } catch (_: Exception) {}
    }

    suspend fun toggleRecoFav(user: String, filename: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply { put("user", user); put("filename", filename) }
            val json = post("/api/reco/fav", body.toString())
            json.optBoolean("is_fav", false)
        } catch (_: Exception) { false }
    }

    fun clearCache() {
        recordingsCache.clear()
        activeBase = null
    }

    fun clearCacheFor(targetId: Int) {
        recordingsCache.remove(targetId)
    }
}
