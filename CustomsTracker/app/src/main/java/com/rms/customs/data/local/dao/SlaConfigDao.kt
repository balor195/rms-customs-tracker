package com.rms.customs.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.rms.customs.data.local.entity.SlaConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SlaConfigDao {

    @Query("SELECT * FROM sla_configs WHERE isActive = 1")
    fun observeAll(): Flow<List<SlaConfigEntity>>

    @Query("SELECT * FROM sla_configs WHERE phaseNumber = :phase AND subPhase = :subPhase AND isActive = 1 LIMIT 1")
    suspend fun getForSubPhase(phase: Int, subPhase: String): SlaConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SlaConfigEntity)

    @Query("UPDATE sla_configs SET isActive = :active WHERE id = :id")
    suspend fun setActive(id: String, active: Boolean)

    @Query("SELECT * FROM sla_configs ORDER BY phaseNumber, subPhase")
    fun observeAllForAdmin(): Flow<List<SlaConfigEntity>>
}
