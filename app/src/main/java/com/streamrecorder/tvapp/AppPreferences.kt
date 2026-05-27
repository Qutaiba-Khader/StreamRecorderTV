package com.streamrecorder.tvapp

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import org.json.JSONArray
import org.json.JSONObject

data class ThemeColors(
    val brand: Int,
    val accent: Int,
    val cardBg: Int,
    val windowBg: Int,
    val textPrimary: Int,
    val textSecondary: Int
)

object AppPreferences {
    private lateinit var prefs: SharedPreferences

    val players = listOf("Any (System Picker)", "MPV", "MX Player", "MX Player Pro", "VLC", "Just Player")
    val resolutions = listOf("Max", "1080p", "720p", "480p")
    val cardSizes = listOf("Tiny", "Small", "Medium", "Large")

    val themeNames = listOf(
        "Glass Dark", "Midnight Blue", "AMOLED Black", "Deep Purple", "Ocean",
        "Charcoal", "Warm Dark", "Light", "Light Blue", "Light Warm"
    )

    private val playerPackages = mapOf(
        "Any (System Picker)" to "",
        "MPV" to "is.xyz.mpv",
        "MX Player" to "com.mxtech.videoplayer.ad",
        "MX Player Pro" to "com.mxtech.videoplayer.pro",
        "VLC" to "org.videolan.vlc",
        "Just Player" to "com.brouken.player"
    )

    private val themes = mapOf(
        "Glass Dark" to ThemeColors(
            Color.parseColor("#0F0F23"), Color.parseColor("#00D4AA"),
            Color.parseColor("#1A1A2E"), Color.parseColor("#000000"),
            Color.WHITE, Color.parseColor("#AAAAAA")
        ),
        "Midnight Blue" to ThemeColors(
            Color.parseColor("#0D1B2A"), Color.parseColor("#48CAE4"),
            Color.parseColor("#1B2838"), Color.parseColor("#050D18"),
            Color.WHITE, Color.parseColor("#8899AA")
        ),
        "AMOLED Black" to ThemeColors(
            Color.parseColor("#000000"), Color.parseColor("#00D4AA"),
            Color.parseColor("#111111"), Color.parseColor("#000000"),
            Color.WHITE, Color.parseColor("#888888")
        ),
        "Deep Purple" to ThemeColors(
            Color.parseColor("#1A0A2E"), Color.parseColor("#B388FF"),
            Color.parseColor("#2D1B69"), Color.parseColor("#0A0514"),
            Color.WHITE, Color.parseColor("#BBAACC")
        ),
        "Ocean" to ThemeColors(
            Color.parseColor("#0A1628"), Color.parseColor("#00BCD4"),
            Color.parseColor("#122240"), Color.parseColor("#050B14"),
            Color.WHITE, Color.parseColor("#8899AA")
        ),
        "Charcoal" to ThemeColors(
            Color.parseColor("#1A1A1A"), Color.parseColor("#FF7043"),
            Color.parseColor("#252525"), Color.parseColor("#0A0A0A"),
            Color.WHITE, Color.parseColor("#999999")
        ),
        "Warm Dark" to ThemeColors(
            Color.parseColor("#1A1410"), Color.parseColor("#FFB74D"),
            Color.parseColor("#2C2218"), Color.parseColor("#0A0806"),
            Color.WHITE, Color.parseColor("#AA9988")
        ),
        "Light" to ThemeColors(
            Color.parseColor("#E0E0EC"), Color.parseColor("#6200EA"),
            Color.parseColor("#F5F5F5"), Color.parseColor("#F0F0F5"),
            Color.parseColor("#222222"), Color.parseColor("#666666")
        ),
        "Light Blue" to ThemeColors(
            Color.parseColor("#C8D8F0"), Color.parseColor("#1976D2"),
            Color.parseColor("#E0ECF8"), Color.parseColor("#E8F0FA"),
            Color.parseColor("#222222"), Color.parseColor("#666666")
        ),
        "Light Warm" to ThemeColors(
            Color.parseColor("#E0D8CC"), Color.parseColor("#E65100"),
            Color.parseColor("#F0E8DC"), Color.parseColor("#F5EFE8"),
            Color.parseColor("#222222"), Color.parseColor("#666666")
        )
    )

    fun init(context: Context) {
        prefs = context.getSharedPreferences("sr_settings", Context.MODE_PRIVATE)
    }

    var defaultPlayer: String
        get() = prefs.getString("player", "MPV") ?: "MPV"
        set(value) = prefs.edit().putString("player", value).apply()

    var preferredResolution: String
        get() = prefs.getString("resolution", "Max") ?: "Max"
        set(value) = prefs.edit().putString("resolution", value).apply()

    var trackPosition: Boolean
        get() = prefs.getBoolean("trackPosition", true)
        set(value) = prefs.edit().putBoolean("trackPosition", value).apply()

    var theme: String
        get() = prefs.getString("theme", "Glass Dark") ?: "Glass Dark"
        set(value) = prefs.edit().putString("theme", value).apply()

    var cardSize: String
        get() = prefs.getString("cardSize", "Medium") ?: "Medium"
        set(value) = prefs.edit().putString("cardSize", value).apply()

    private val featureKeys = listOf(
        "hdThumbnails", "showViewerCount", "showDeletionCountdown"
    )

    var hdThumbnails: Boolean
        get() = prefs.getBoolean("hdThumbnails", false)
        set(value) = prefs.edit().putBoolean("hdThumbnails", value).apply()

    var showViewerCount: Boolean
        get() = prefs.getBoolean("showViewerCount", false)
        set(value) = prefs.edit().putBoolean("showViewerCount", value).apply()

    var showDeletionCountdown: Boolean
        get() = prefs.getBoolean("showDeletionCountdown", false)
        set(value) = prefs.edit().putBoolean("showDeletionCountdown", value).apply()

    fun getFullSettings(): JSONObject {
        val json = JSONObject()
        for (key in featureKeys) {
            json.put(key, prefs.getBoolean(key, false))
        }
        val pinned = getPinnedUsers()
        val arr = JSONArray()
        pinned.forEach { arr.put(it) }
        json.put("pinnedUsers", arr)
        return json
    }

    fun applyServerSettings(remote: JSONObject) {
        val editor = prefs.edit()
        for (key in featureKeys) {
            if (remote.has(key)) editor.putBoolean(key, remote.optBoolean(key, false))
        }
        if (remote.has("pinnedUsers")) {
            val arr = remote.optJSONArray("pinnedUsers")
            if (arr != null) {
                val set = (0 until arr.length()).map { arr.getInt(it).toString() }.toSet()
                editor.putStringSet("pinned_users", set)
            }
        }
        editor.apply()
    }

    fun getPinnedUsers(): Set<Int> {
        val set = prefs.getStringSet("pinned_users", emptySet()) ?: emptySet()
        return set.mapNotNull { it.toIntOrNull() }.toSet()
    }

    fun isPinned(targetId: Int): Boolean = getPinnedUsers().contains(targetId)

    fun togglePin(targetId: Int): Boolean {
        val set = prefs.getStringSet("pinned_users", mutableSetOf())!!.toMutableSet()
        val idStr = targetId.toString()
        val nowPinned = if (set.contains(idStr)) { set.remove(idStr); false } else { set.add(idStr); true }
        prefs.edit().putStringSet("pinned_users", set).apply()
        return nowPinned
    }

    fun saveWatchPosition(recId: Int, position: Long, duration: Long) {
        if (duration <= 0) return
        val pct = ((position * 100) / duration).toInt().coerceIn(0, 100)
        prefs.edit().putInt("wp_$recId", pct).apply()
    }

    fun getWatchPercent(recId: Int): Int = prefs.getInt("wp_$recId", 0)

    fun cardWidthFraction(): Float = when (cardSize) {
        "Tiny" -> 0.65f
        "Small" -> 0.72f
        "Medium" -> 0.80f
        "Large" -> 0.88f
        else -> 0.80f
    }

    fun currentTheme(): ThemeColors = themes[theme] ?: themes["Glass Dark"]!!

    fun addHidden(recId: Int, res: Int, date: String, streamer: String) {
        val set = prefs.getStringSet("hidden_sources", mutableSetOf())!!.toMutableSet()
        set.add("$recId|$res|$date|$streamer")
        prefs.edit().putStringSet("hidden_sources", set).apply()
    }

    fun removeHidden(recId: Int, res: Int) {
        val set = prefs.getStringSet("hidden_sources", mutableSetOf())!!.toMutableSet()
        set.removeAll { it.startsWith("$recId|$res|") }
        prefs.edit().putStringSet("hidden_sources", set).apply()
    }

    fun getHiddenSources(): List<HiddenSource> {
        val set = prefs.getStringSet("hidden_sources", emptySet()) ?: emptySet()
        return set.mapNotNull { entry ->
            val parts = entry.split("|", limit = 4)
            if (parts.size >= 4) HiddenSource(
                recId = parts[0].toIntOrNull() ?: return@mapNotNull null,
                res = parts[1].toIntOrNull() ?: return@mapNotNull null,
                date = parts[2],
                streamer = parts[3]
            ) else null
        }.sortedByDescending { it.date }
    }

    var cachedTargetsJson: String?
        get() = prefs.getString("cached_targets", null)
        set(value) = prefs.edit().putString("cached_targets", value).apply()

    fun resolvePlayerPackage(): String = playerPackages[defaultPlayer] ?: ""

    fun resolveResolution(sources: List<Source>): Source? {
        if (preferredResolution == "Max") return sources.maxByOrNull { it.filesize }
        val targetRes = preferredResolution.replace("p", "").toIntOrNull()
            ?: return sources.maxByOrNull { it.filesize }
        return sources.find { it.resolution == targetRes }
            ?: sources.minByOrNull { kotlin.math.abs(it.resolution - targetRes) }
    }
}
