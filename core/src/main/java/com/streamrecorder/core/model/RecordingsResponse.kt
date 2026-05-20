package com.streamrecorder.core.model

data class RecordingsResponse(
    val data: List<Recording>,
    val countTotal: Int,
)
