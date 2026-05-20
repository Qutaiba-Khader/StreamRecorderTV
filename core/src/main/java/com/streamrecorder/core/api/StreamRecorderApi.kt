package com.streamrecorder.core.api

import retrofit2.http.*

interface StreamRecorderApi {

    @GET("api/targets")
    suspend fun getTargets(): TargetsResponse

    @GET("api/recordings/{id}")
    suspend fun getRecordings(@Path("id") targetId: Int): ApiRecordingsResponse

    @GET("api/live-stream/{slug}")
    suspend fun getLiveStream(@Path("slug") slug: String): ApiLiveStream

    @POST("api/fav")
    @Headers("Content-Type: application/json")
    suspend fun toggleFav(@Body body: Map<String, Int>): FavResponse

    @POST("api/hide")
    @Headers("Content-Type: application/json")
    suspend fun hideSource(@Body body: Map<String, @JvmSuppressWildcards Any>): OkResponse

    @POST("api/unhide")
    @Headers("Content-Type: application/json")
    suspend fun unhideSource(@Body body: Map<String, @JvmSuppressWildcards Any>): OkResponse

    @POST("api/watch-position")
    @Headers("Content-Type: application/json")
    suspend fun saveWatchPosition(@Body body: Map<String, @JvmSuppressWildcards Any>): OkResponse

    @POST("api/watch-position/delete")
    @Headers("Content-Type: application/json")
    suspend fun deleteWatchPosition(@Body body: Map<String, @JvmSuppressWildcards Any>): OkResponse

    @GET("api/watch-positions")
    suspend fun getWatchPositions(): WatchPositionsResponse

    @POST("api/watch-positions/bulk")
    @Headers("Content-Type: application/json")
    suspend fun bulkMergeWatchPositions(@Body body: Map<String, @JvmSuppressWildcards Any>): BulkMergeResponse

    @POST("trigger/{secret}")
    suspend fun flushCache(@Path("secret") secret: String): okhttp3.ResponseBody
}
