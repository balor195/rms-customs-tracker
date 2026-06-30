package com.rms.customs.domain.repository

interface SyncRepository {
    suspend fun push(): Int
    suspend fun pull(): Int
    suspend fun sync(): Result<Pair<Int, Int>>
    fun getLastSyncTimeMs(): Long
}
