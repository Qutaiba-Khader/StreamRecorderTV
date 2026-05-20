package com.streamrecorder.core.model

data class Recording(
    val id: Int,
    val recordedAt: String,
    val duration: Int,
    val durationHr: String,
    val status: String,
    val streamtitle: String?,
    val isFav: Boolean,
    val posterSmall192: String?,
    val posterSmall270: String?,
    val thumbLarge: String?,
    val sources: List<Source>,
    val watchPositions: Map<String, WatchPositionEntry>?,
) {
    val isRunning: Boolean get() = status == "running"
    val isFinished: Boolean get() = status == "finished"
    val displayTitle: String get() = streamtitle?.trim()?.ifEmpty { null } ?: "(untitled)"
    val dateTime: String get() = recordedAt.replace("T", " ").take(16)
    val thumbnailUrl: String? get() = posterSmall270 ?: posterSmall192

    val bestSource: Source? get() = sources.maxByOrNull { it.filesize }

    fun watchProgress(resolution: Int): Float? {
        val wp = watchPositions?.get(resolution.toString()) ?: return null
        if (wp.duration <= 0) return null
        return (wp.position / wp.duration).coerceIn(0f, 1f)
    }

    data class WatchPositionEntry(
        val position: Float,
        val duration: Float,
    )
}
