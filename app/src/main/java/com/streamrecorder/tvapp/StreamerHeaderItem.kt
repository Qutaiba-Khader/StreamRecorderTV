package com.streamrecorder.tvapp

import androidx.leanback.widget.HeaderItem

class StreamerHeaderItem(
    id: Long,
    name: String,
    val logoUrl: String?,
    val isLive: Boolean
) : HeaderItem(id, name)
