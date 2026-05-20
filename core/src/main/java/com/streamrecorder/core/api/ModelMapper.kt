package com.streamrecorder.core.api

import com.streamrecorder.core.model.*

fun ApiTarget.toDomain() = Target(
    id = id,
    name = name,
    platform = platform,
    countTotal = countTotal,
    latestTs = latestTs,
    isLive = isLive,
    logo = logo,
    playlistUrl = playlistUrl,
)

fun ApiSource.toDomain() = Source(
    resolution = resolution,
    filesize = filesize,
    downloadlink = downloadlink,
    deletiontime = deletiontime ?: 0,
    recordedfileid = recordedfileid ?: 0,
)

fun ApiRecording.toDomain() = Recording(
    id = id,
    recordedAt = recordedAt,
    duration = duration,
    durationHr = durationHr ?: "",
    status = status,
    streamtitle = streamtitle,
    isFav = isFav,
    posterSmall192 = posterSmall192,
    posterSmall270 = posterSmall270,
    thumbLarge = thumbLarge,
    sources = sources.map { it.toDomain() },
    watchPositions = watchPositions?.mapValues {
        Recording.WatchPositionEntry(it.value.position, it.value.duration)
    },
)

fun ApiRecordingsResponse.toDomain() = RecordingsResponse(
    data = data.map { it.toDomain() },
    countTotal = countTotal,
)

fun ApiLiveStream.toDomain() = LiveStream(
    isLive = isLive,
    username = username,
    title = title,
    streams = streams,
)

fun ApiWatchPositionFull.toDomain() = WatchPosition(
    recId = recId,
    res = res,
    position = position,
    duration = duration,
    updatedAt = updatedAt,
)
