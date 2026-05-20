package com.streamrecorder.core.api

import com.google.gson.annotations.SerializedName

data class TargetsResponse(
    val targets: List<ApiTarget>,
    @SerializedName("last_update") val lastUpdate: String?,
)

data class ApiTarget(
    val id: Int,
    val name: String,
    val platform: String,
    @SerializedName("count_total") val countTotal: Int,
    @SerializedName("latest_ts") val latestTs: Long,
    @SerializedName("is_live") val isLive: Boolean,
    val logo: String,
    @SerializedName("playlist_url") val playlistUrl: String,
)

data class ApiRecordingsResponse(
    val data: List<ApiRecording>,
    @SerializedName("count_total") val countTotal: Int,
)

data class ApiRecording(
    val id: Int,
    @SerializedName("recorded_at") val recordedAt: String,
    val duration: Int,
    @SerializedName("duration_hr") val durationHr: String?,
    val status: String,
    val streamtitle: String?,
    @SerializedName("is_fav") val isFav: Boolean,
    @SerializedName("poster_small_192") val posterSmall192: String?,
    @SerializedName("poster_small_270") val posterSmall270: String?,
    @SerializedName("thumb_large") val thumbLarge: String?,
    val sources: List<ApiSource>,
    @SerializedName("watch_positions") val watchPositions: Map<String, ApiWatchPositionEntry>?,
)

data class ApiSource(
    val resolution: Int,
    val filesize: Long,
    val downloadlink: String,
    val deletiontime: Long?,
    val recordedfileid: Long?,
)

data class ApiWatchPositionEntry(
    val position: Float,
    val duration: Float,
)

data class ApiLiveStream(
    @SerializedName("is_live") val isLive: Boolean,
    val username: String?,
    val title: String?,
    val streams: Map<String, Any>?,
)

data class FavResponse(
    val ok: Boolean,
    @SerializedName("is_fav") val isFav: Boolean,
)

data class OkResponse(val ok: Boolean)

data class WatchPositionsResponse(
    val positions: Map<String, ApiWatchPositionFull>,
)

data class ApiWatchPositionFull(
    @SerializedName("rec_id") val recId: Int,
    val res: String,
    val position: Float,
    val duration: Float,
    @SerializedName("updated_at") val updatedAt: String,
)

data class BulkMergeResponse(
    val ok: Boolean,
    val merged: Int,
)
