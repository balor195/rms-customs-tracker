package com.rms.customs.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.rms.customs.data.local.entity.PhaseRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PhaseRecordDao {

    @Query("SELECT * FROM phase_records WHERE transactionId = :transactionId ORDER BY phaseNumber, subPhase")
    fun observeForTransaction(transactionId: String): Flow<List<PhaseRecordEntity>>

    @Query("SELECT * FROM phase_records WHERE transactionId = :transactionId AND phaseNumber = :phase ORDER BY subPhase")
    fun observeForPhase(transactionId: String, phase: Int): Flow<List<PhaseRecordEntity>>

    @Query("SELECT * FROM phase_records WHERE id = :id")
    suspend fun getById(id: String): PhaseRecordEntity?

    @Query("SELECT * FROM phase_records WHERE transactionId = :transactionId AND phaseNumber = 4")
    suspend fun getPhase4Records(transactionId: String): List<PhaseRecordEntity>

    @Query("SELECT COUNT(*) FROM phase_records WHERE transactionId = :transactionId AND phaseNumber = 4 AND status NOT IN ('DONE', 'SKIPPED')")
    suspend fun countPhase4Incomplete(transactionId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PhaseRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<PhaseRecordEntity>)

    @Update
    suspend fun update(entity: PhaseRecordEntity)

    @Query("UPDATE phase_records SET status = :status, completedAt = :completedAt, completedByUserId = :userId WHERE id = :id")
    suspend fun markComplete(id: String, status: String, completedAt: Long, userId: String)

    @Query("SELECT * FROM phase_records WHERE status = 'IN_PROGRESS' AND startedAt IS NOT NULL")
    suspend fun getActivePhasesForSlaCheck(): List<PhaseRecordEntity>

    @Query("SELECT * FROM phase_records WHERE status = 'IN_PROGRESS' AND startedAt IS NOT NULL")
    fun observeAllInProgress(): Flow<List<PhaseRecordEntity>>

    @Query("SELECT * FROM phase_records WHERE status = 'DONE' AND subPhase = :subPhase AND startedAt IS NOT NULL AND completedAt IS NOT NULL")
    fun observeCompletedBySubPhase(subPhase: String): Flow<List<PhaseRecordEntity>>

    // ── Sync helpers ────────────────────────────────────────────────────────

    @Query("SELECT * FROM phase_records WHERE transactionId = :transactionId ORDER BY phaseNumber, subPhase")
    suspend fun getAllForTransaction(transactionId: String): List<PhaseRecordEntity>
}
