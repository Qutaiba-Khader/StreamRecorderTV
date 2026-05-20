package com.streamrecorder.core.cache

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.streamrecorder.core.model.Target
import com.streamrecorder.core.model.RecordingsResponse
import com.streamrecorder.core.model.WatchPosition

class CacheManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("sr_cache", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val KEY_TARGETS = "targets"
        private const val KEY_TARGETS_TIME = "targets_time"
        private const val KEY_LAST_UPDATE = "last_update"
        private const val KEY_RECS_PREFIX = "recs_"
        private const val KEY_RECS_TIME_PREFIX = "recs_time_"
        private const val KEY_WATCH_POSITIONS = "watch_positions"
        private const val KEY_SERVER_MODE = "server_mode"
        private const val KEY_DEFAULT_PLAYER = "default_player"
        private const val KEY_MX_USE_PRO = "mx_use_pro"

        private const val TARGETS_TTL_MS = 60 * 60 * 1000L  // 1 hour
        private const val RECS_TTL_MS = 5 * 60 * 1000L      // 5 min
    }

    // --- Targets ---

    fun saveTargets(targets: List<Target>, lastUpdate: String?) {
        prefs.edit()
            .putString(KEY_TARGETS, gson.toJson(targets))
            .putLong(KEY_TARGETS_TIME, System.currentTimeMillis())
            .putString(KEY_LAST_UPDATE, lastUpdate)
            .apply()
    }

    fun getTargets(): List<Target>? {
        val json = prefs.getString(KEY_TARGETS, null) ?: return null
        return try {
            gson.fromJson(json, object : TypeToken<List<Target>>() {}.type)
        } catch (_: Exception) { null }
    }

    fun isTargetsFresh(): Boolean {
        val time = prefs.getLong(KEY_TARGETS_TIME, 0)
        return System.currentTimeMillis() - time < TARGETS_TTL_MS
    }

    fun getLastUpdate(): String? = prefs.getString(KEY_LAST_UPDATE, null)

    // --- Recordings ---

    fun saveRecordings(targetId: Int, data: RecordingsResponse) {
        prefs.edit()
            .putString("$KEY_RECS_PREFIX$targetId", gson.toJson(data))
            .putLong("$KEY_RECS_TIME_PREFIX$targetId", System.currentTimeMillis())
            .apply()
    }

    fun getRecordings(targetId: Int): RecordingsResponse? {
        val json = prefs.getString("$KEY_RECS_PREFIX$targetId", null) ?: return null
        return try {
            gson.fromJson(json, RecordingsResponse::class.java)
        } catch (_: Exception) { null }
    }

    fun isRecordingsFresh(targetId: Int): Boolean {
        val time = prefs.getLong("$KEY_RECS_TIME_PREFIX$targetId", 0)
        return System.currentTimeMillis() - time < RECS_TTL_MS
    }

    // --- Watch Positions (local) ---

    fun saveWatchPosition(wp: WatchPosition) {
        val all = getLocalWatchPositions().toMutableMap()
        all[wp.key] = wp
        prefs.edit().putString(KEY_WATCH_POSITIONS, gson.toJson(all)).apply()
    }

    fun getLocalWatchPositions(): Map<String, WatchPosition> {
        val json = prefs.getString(KEY_WATCH_POSITIONS, null) ?: return emptyMap()
        return try {
            gson.fromJson(json, object : TypeToken<Map<String, WatchPosition>>() {}.type)
        } catch (_: Exception) { emptyMap() }
    }

    fun mergeWatchPositions(remote: Map<String, WatchPosition>) {
        val local = getLocalWatchPositions().toMutableMap()
        for ((key, remoteWp) in remote) {
            val localWp = local[key]
            if (localWp == null || remoteWp.updatedAt > localWp.updatedAt) {
                local[key] = remoteWp
            }
        }
        prefs.edit().putString(KEY_WATCH_POSITIONS, gson.toJson(local)).apply()
    }

    fun deleteWatchPosition(recId: Int, res: String) {
        val all = getLocalWatchPositions().toMutableMap()
        all.remove("$recId:$res")
        prefs.edit().putString(KEY_WATCH_POSITIONS, gson.toJson(all)).apply()
    }

    // --- Settings ---

    var serverMode: String
        get() = prefs.getString(KEY_SERVER_MODE, "auto") ?: "auto"
        set(value) = prefs.edit().putString(KEY_SERVER_MODE, value).apply()

    var defaultPlayer: String
        get() = prefs.getString(KEY_DEFAULT_PLAYER, "mpv") ?: "mpv"
        set(value) = prefs.edit().putString(KEY_DEFAULT_PLAYER, value).apply()

    var mxUsePro: Boolean
        get() = prefs.getBoolean(KEY_MX_USE_PRO, true)
        set(value) = prefs.edit().putBoolean(KEY_MX_USE_PRO, value).apply()
}
