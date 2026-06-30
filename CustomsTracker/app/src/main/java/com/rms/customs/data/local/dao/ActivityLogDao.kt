package com.rms.customs.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.rms.customs.data.local.entity.ActivityLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityLogDao {

    @Query("SELECT * FROM activity_logs WHERE transactionId = :transactionId ORDER BY occurredAt DESC")
    fun observeForTransaction(transactionId: String): Flow<List<ActivityLogEntity>>

    @Query("SELECT * FROM activity_logs WHERE transactionId = :transactionId ORDER BY occurredAt DESC LIMIT :limit")
    suspend fun getRecent(transactionId: String, limit: Int = 20): List<ActivityLogEntity>

    @Insert
    suspend fun insert(entity: ActivityLogEntity)
}
