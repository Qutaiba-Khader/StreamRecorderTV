package com.streamrecorder.core.model

data class Target(
    val id: Int,
    val name: String,
    val platform: String,
    val countTotal: Int,
    val latestTs: Long,
    val isLive: Boolean,
    val logo: String,
    val playlistUrl: String,
) {
    val slug: String get() = name.removePrefix("@")
    val avatarUrl: String get() = logo.replace("250x250", "56x56")
}
