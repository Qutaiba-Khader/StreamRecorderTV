package com.streamrecorder.core.repository

import com.streamrecorder.core.api.*
import com.streamrecorder.core.cache.CacheManager
import com.streamrecorder.core.model.LiveStream
import com.streamrecorder.core.model.RecordingsResponse
import com.streamrecorder.core.model.Target
import com.streamrecorder.core.model.WatchPosition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class StreamerRepository(
    private val cache: CacheManager,
) {
    companion object {
        const val TRIGGER_SECRET = "88342d2ac12d2786e8ef3a21b7a4cdc6"
    }

    private suspend fun api(): StreamRecorderApi = ApiClient.getDetectedApi()

    // --- Targets ---

    suspend fun getTargets(forceRefresh: Boolean = false): Pair<List<Target>, String?> {
        if (!forceRefresh) {
            val cached = cache.getTargets()
            if (cached != null && cache.isTargetsFresh()) {
                return cached to cache.getLastUpdate()
            }
        }
        return withContext(Dispatchers.IO) {
            val resp = api().getTargets()
            val targets = resp.targets.map { it.toDomain() }
            cache.saveTargets(targets, resp.lastUpdate)
            targets to resp.lastUpdate
        }
    }

    fun getCachedTargets(): Pair<List<Target>, String?>? {
        val targets = cache.getTargets() ?: return null
        return targets to cache.getLastUpdate()
    }

    // --- Recordings ---

    suspend fun getRecordings(targetId: Int, forceRefresh: Boolean = false): RecordingsResponse {
        if (!forceRefresh) {
            val cached = cache.getRecordings(targetId)
            if (cached != null && cache.isRecordingsFresh(targetId)) {
                return cached
            }
        }
        return withContext(Dispatchers.IO) {
            val resp = api().getRecordings(targetId)
            val domain = resp.toDomain()
            cache.saveRecordings(targetId, domain)
            domain
        }
    }

    fun getCachedRecordings(targetId: Int): RecordingsResponse? = cache.getRecordings(targetId)

    // --- Live Stream ---

    suspend fun getLiveStream(slug: String): LiveStream = withContext(Dispatchers.IO) {
        api().getLiveStream(slug).toDomain()
    }

    // --- Actions ---

    suspend fun toggleFav(recId: Int): Boolean = withContext(Dispatchers.IO) {
        api().toggleFav(mapOf("rec_id" to recId)).isFav
    }

    suspend fun hideSource(recId: Int, res: String) = withContext(Dispatchers.IO) {
        api().hideSource(mapOf("rec_id" to recId, "res" to res))
    }

    suspend fun unhideSource(recId: Int, res: String) = withContext(Dispatchers.IO) {
        api().unhideSource(mapOf("rec_id" to recId, "res" to res))
    }

    // --- Watch Positions ---

    suspend fun saveWatchPosition(recId: Int, res: String, position: Float, duration: Float) {
        val now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val wp = WatchPosition(recId, res, position, duration, now)
        cache.saveWatchPosition(wp)
        try {
            withContext(Dispatchers.IO) {
                api().saveWatchPosition(mapOf(
                    "rec_id" to recId, "res" to res,
                    "position" to position, "duration" to duration,
                ))
            }
        } catch (_: Exception) { /* fire-and-forget */ }
    }

    suspend fun syncWatchPositions() {
        try {
            withContext(Dispatchers.IO) {
                val remote = api().getWatchPositions()
                val remoteMap = remote.positions.mapValues { it.value.toDomain() }
                cache.mergeWatchPositions(remoteMap)

                val local = cache.getLocalWatchPositions()
                val toUpload = mutableMapOf<String, Any>()
                for ((key, wp) in local) {
                    val remoteWp = remoteMap[key]
                    if (remoteWp == null || wp.updatedAt > remoteWp.updatedAt) {
                        toUpload[key] = mapOf(
                            "rec_id" to wp.recId, "res" to wp.res,
                            "position" to wp.position, "duration" to wp.duration,
                            "updated_at" to wp.updatedAt,
                        )
                    }
                }
                if (toUpload.isNotEmpty()) {
                    api().bulkMergeWatchPositions(mapOf("positions" to toUpload))
                }
            }
        } catch (_: Exception) { /* best-effort sync */ }
    }

    // --- Refresh ---

    suspend fun flushServerCache() = withContext(Dispatchers.IO) {
        try { api().flushCache(TRIGGER_SECRET) } catch (_: Exception) {}
    }

    suspend fun fullRefresh(): Pair<List<Target>, String?> {
        flushServerCache()
        return getTargets(forceRefresh = true)
    }

    // --- Server Detection ---

    suspend fun detectServer(): String = ApiClient.serverDetector.detect()

    fun getBaseUrl(): String? = ApiClient.serverDetector.detectedBaseUrl

    fun setServerMode(mode: ServerDetector.Mode) {
        ApiClient.serverDetector.mode = mode
    }
}
