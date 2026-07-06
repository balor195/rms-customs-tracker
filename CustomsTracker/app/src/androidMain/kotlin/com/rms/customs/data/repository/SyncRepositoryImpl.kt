package com.rms.customs.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.rms.customs.data.local.dao.TransactionDao
import com.rms.customs.data.remote.api.CustomsApi
import com.rms.customs.data.remote.dto.SyncPushRequest
import com.rms.customs.data.remote.dto.toEntity
import com.rms.customs.data.remote.dto.toSyncDto
import com.rms.customs.domain.repository.SyncRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private val Context.syncDataStore by preferencesDataStore(name = "rms_sync_prefs")

class SyncRepositoryImpl(
    private val context: Context,
    private val api: CustomsApi,
    private val transactionDao: TransactionDao,
) : SyncRepository {

    private companion object {
        val KEY_LAST_SYNC_MS = longPreferencesKey("last_sync_ms")
        val KEY_DEVICE_ID = stringPreferencesKey("device_id")
    }

    // Cached in memory so getLastSyncTimeMs() can stay synchronous (SyncRepository's
    // interface, and its two ViewModel callers, predate DataStore's Flow/suspend-only API).
    @Volatile
    private var cachedLastSyncMs: Long = runBlocking {
        context.syncDataStore.data.first()[KEY_LAST_SYNC_MS] ?: 0L
    }

    private suspend fun setLastSyncTime(t: Long) {
        cachedLastSyncMs = t
        context.syncDataStore.edit { it[KEY_LAST_SYNC_MS] = t }
    }

    @OptIn(ExperimentalUuidApi::class)
    private suspend fun deviceId(): String {
        val existing = context.syncDataStore.data.first()[KEY_DEVICE_ID]
        if (existing != null) return existing
        val id = Uuid.random().toString()
        context.syncDataStore.edit { it[KEY_DEVICE_ID] = id }
        return id
    }

    override suspend fun push(): Int {
        val since = cachedLastSyncMs
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
        val since = cachedLastSyncMs
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

    override fun getLastSyncTimeMs(): Long = cachedLastSyncMs
}
