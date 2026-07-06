package com.rms.customs.data.repository

import android.content.Context
import com.rms.customs.data.local.dao.TransactionDao
import com.rms.customs.data.remote.api.CustomsApi
import com.rms.customs.data.remote.dto.SyncPushRequest
import com.rms.customs.data.remote.dto.toEntity
import com.rms.customs.data.remote.dto.toSyncDto
import com.rms.customs.domain.repository.SyncRepository
import java.util.UUID

class SyncRepositoryImpl(
    private val context: Context,
    private val api: CustomsApi,
    private val transactionDao: TransactionDao,
) : SyncRepository {

    private val prefs by lazy {
        context.getSharedPreferences("rms_sync_prefs", Context.MODE_PRIVATE)
    }

    private fun getLastSyncTime(): Long = prefs.getLong("last_sync_ms", 0L)

    private fun setLastSyncTime(t: Long) = prefs.edit().putLong("last_sync_ms", t).apply()

    private fun deviceId(): String {
        var id = prefs.getString("device_id", null)
        if (id == null) {
            id = UUID.randomUUID().toString()
            prefs.edit().putString("device_id", id).apply()
        }
        return id!!
    }

    override suspend fun push(): Int {
        val since = getLastSyncTime()
        val modified = transactionDao.getModifiedSince(since)
        if (modified.isEmpty()) return 0

        val dtos = modified.map { tx -> tx.toSyncDto() }

        val response = api.push(
            SyncPushRequest(
                deviceId     = deviceId(),
                pushedAt     = System.currentTimeMillis(),
                transactions = dtos,
            )
        )
        return response.accepted
    }

    override suspend fun pull(): Int {
        val since = getLastSyncTime()
        val response = api.pull(since = since, deviceId = deviceId())

        response.transactions.forEach { dto ->
            transactionDao.insert(dto.toEntity())
        }

        setLastSyncTime(response.serverTimeMs)
        return response.transactions.size
    }

    override suspend fun sync(): Result<Pair<Int, Int>> = runCatching {
        val pushed = push()
        val pulled = pull()
        Pair(pushed, pulled)
    }

    override fun getLastSyncTimeMs(): Long = getLastSyncTime()
}
