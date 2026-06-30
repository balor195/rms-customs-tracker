package com.rms.customs.data.remote.api

import com.rms.customs.data.remote.dto.SyncPullResponse
import com.rms.customs.data.remote.dto.SyncPushRequest
import com.rms.customs.data.remote.dto.SyncPushResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface CustomsApi {

    @POST("api/v1/sync/push")
    suspend fun push(@Body request: SyncPushRequest): SyncPushResponse

    @GET("api/v1/sync/pull")
    suspend fun pull(
        @Query("since") since: Long,
        @Query("device_id") deviceId: String,
    ): SyncPullResponse
}
