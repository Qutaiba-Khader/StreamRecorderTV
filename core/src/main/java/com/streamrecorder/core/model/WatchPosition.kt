package com.streamrecorder.core.model

data class WatchPosition(
    val recId: Int,
    val res: String,
    val position: Float,
    val duration: Float,
    val updatedAt: String,
) {
    val key: String get() = "$recId:$res"
    val progress: Float get() = if (duration > 0) (position / duration).coerceIn(0f, 1f) else 0f
}
