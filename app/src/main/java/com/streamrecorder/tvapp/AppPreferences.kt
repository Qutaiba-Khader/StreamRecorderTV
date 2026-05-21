package com.streamrecorder.tvapp

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color

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

    val players = listOf("Any (System Picker)", "MPV", "MX Player", "VLC", "Just Player")
    val resolutions = listOf("Max", "1080p", "720p", "480p")
    val themeNames = listOf(
        "Glass Dark", "Midnight Blue", "AMOLED Black", "Deep Purple", "Ocean",
        "Charcoal", "Warm Dark", "Light", "Light Blue", "Light Warm"
    )

    private val playerPackages = mapOf(
        "Any (System Picker)" to "",
        "MPV" to "is.xyz.mpv",
        "MX Player" to "com.mxtech.videoplayer.pro",
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

    fun currentTheme(): ThemeColors = themes[theme] ?: themes["Glass Dark"]!!

    fun resolvePlayerPackage(): String = playerPackages[defaultPlayer] ?: ""

    fun resolveResolution(sources: List<Source>): Source? {
        if (preferredResolution == "Max") return sources.maxByOrNull { it.filesize }
        val targetRes = preferredResolution.replace("p", "").toIntOrNull()
            ?: return sources.maxByOrNull { it.filesize }
        return sources.find { it.resolution == targetRes }
            ?: sources.minByOrNull { kotlin.math.abs(it.resolution - targetRes) }
    }
}
