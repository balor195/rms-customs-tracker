package com.rms.customs.domain.repository

import com.rms.customs.domain.model.ActivityLog
import com.rms.customs.domain.model.PhaseRecord
import com.rms.customs.domain.model.Transaction
import com.rms.customs.domain.model.enums.TransactionStatus
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface TransactionRepository {
    fun observeAll(): Flow<List<Transaction>>
    fun observeActive(): Flow<List<Transaction>>
    fun observeByStatus(vararg statuses: TransactionStatus): Flow<List<Transaction>>
    fun observeById(id: UUID): Flow<Transaction?>
    suspend fun getById(id: UUID): Transaction?
    suspend fun create(transaction: Transaction)
    suspend fun update(transaction: Transaction)
    suspend fun advanceStatus(
        transactionId: UUID,
        newStatus: TransactionStatus,
        actorUserId: UUID,
        payload: String = "{}",
    )
    suspend fun setExceptionState(
        transactionId: UUID,
        exceptionStatus: TransactionStatus,
        reason: String,
        actorUserId: UUID,
    )
    suspend fun clearExceptionState(transactionId: UUID, actorUserId: UUID)
    fun observeActivityLog(transactionId: UUID): Flow<List<ActivityLog>>
    fun observePhaseRecords(transactionId: UUID): Flow<List<PhaseRecord>>
    suspend fun updatePhaseRecord(record: PhaseRecord)
    suspend fun completePhaseRecord(phaseRecordId: UUID, completedByUserId: UUID)
    suspend fun countByStatus(status: TransactionStatus): Int
    suspend fun countOverdueSla(): Int
    suspend fun generateRef(): String
    suspend fun getActivePhasesForSlaCheck(): List<PhaseRecord>
    fun observeAllInProgressPhases(): Flow<List<PhaseRecord>>
    fun observeCompletedPhasesBySubPhase(subPhase: String): Flow<List<PhaseRecord>>
}
