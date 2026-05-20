package com.streamrecorder.core.model

data class LiveStream(
    val isLive: Boolean,
    val username: String?,
    val title: String?,
    val streams: Map<String, Any>?,
) {
    val flvQualities: Map<String, String>
        get() {
            @Suppress("UNCHECKED_CAST")
            val flv = streams?.get("flv") as? Map<String, String> ?: return emptyMap()
            return flv
        }

    val hlsQualities: Map<String, String>
        get() {
            @Suppress("UNCHECKED_CAST")
            val hls = streams?.get("hls") as? Map<String, String> ?: return emptyMap()
            return hls
        }
}
