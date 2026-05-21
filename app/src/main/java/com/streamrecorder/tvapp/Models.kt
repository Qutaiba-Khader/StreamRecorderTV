package com.streamrecorder.tvapp

data class Target(
    val id: Int,
    val name: String,
    val platform: String,
    val countTotal: Int,
    val isLive: Boolean,
    val logo: String?,
    val latestTs: Long
)

data class Source(
    val resolution: Int,
    val filesize: Long,
    val downloadlink: String
)

data class Recording(
    val id: Int,
    val recordedAt: String,
    val duration: Int,
    val durationHr: String,
    val sources: List<Source>,
    val isFav: Boolean,
    val posterSmall270: String?,
    val posterSmall192: String?
) {
    val bestSource: Source? get() = sources.maxByOrNull { it.filesize }
    val thumbnail: String? get() = posterSmall270 ?: posterSmall192
    val displayDate: String get() = recordedAt.replace("T", " ").take(16)
}
