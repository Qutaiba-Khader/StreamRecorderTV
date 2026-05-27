package com.streamrecorder.tvapp

data class Target(
    val id: Int,
    val name: String,
    val platform: String,
    val countTotal: Int,
    val isLive: Boolean,
    val isPostprocessing: Boolean = false,
    val logo: String?,
    val latestTs: Long
)

data class Source(
    val resolution: Int,
    val filesize: Long,
    val downloadlink: String,
    val deletionTimeHr: String? = null,
    val deletionTime: Long = 0
)

data class Recording(
    val id: Int,
    val recordedAt: String,
    val duration: Int,
    val durationHr: String,
    val sources: List<Source>,
    val isFav: Boolean,
    val posterSmall270: String?,
    val posterSmall192: String?,
    val thumbLarge: String? = null,
    val maxViewerCount: Int = 0,
    val watchPercentage: Int = 0
) {
    val bestSource: Source? get() = sources.maxByOrNull { it.filesize }
    val thumbnail: String? get() = posterSmall270 ?: posterSmall192
    val displayDate: String get() = recordedAt.replace("T", " ").take(16)
    val deletionDays: Int? get() {
        val dt = bestSource?.deletionTime ?: return null
        return if (dt > 0) (dt / 86400).toInt() else null
    }
}

data class LiveStreamData(
    val isLive: Boolean,
    val title: String,
    val streams: Map<String, String>
)

data class LiveStreamCard(
    val title: String,
    val streams: Map<String, String>,
    val streamerName: String
)

data class PostProcessingCard(
    val streamerName: String
)

data class HiddenSource(
    val recId: Int,
    val res: Int,
    val date: String,
    val streamer: String
)

data class RecoFile(
    val filename: String,
    val size: Long,
    val resolution: Int?,
    val date: String?,
    val user: String,
    val thumbUrl: String? = null,
    val playUrl: String? = null,
    val isFav: Boolean = false,
    val watchPct: Int = 0
)
