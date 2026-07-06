package com.rms.customs.data.remote.api

import com.rms.customs.data.remote.dto.SyncPullResponse
import com.rms.customs.data.remote.dto.SyncPushRequest
import com.rms.customs.data.remote.dto.SyncPushResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class CustomsApi(private val client: HttpClient) {

    suspend fun push(request: SyncPushRequest): SyncPushResponse =
        client.post("api/v1/sync/push") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun pull(since: Long, deviceId: String): SyncPullResponse =
        client.get("api/v1/sync/pull") {
            parameter("since", since)
            parameter("device_id", deviceId)
        }.body()
}
