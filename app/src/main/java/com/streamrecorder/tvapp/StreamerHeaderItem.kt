package com.streamrecorder.tvapp

import androidx.leanback.widget.HeaderItem

class StreamerHeaderItem(
    id: Long,
    name: String,
    val logoUrl: String?,
    val isLive: Boolean,
    val platform: String = "tiktok",
    val countTotal: Int = 0
) : HeaderItem(id, name)
