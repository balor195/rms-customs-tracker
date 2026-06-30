package com.rms.customs.data.repository

import com.rms.customs.data.local.dao.ActivityLogDao
import com.rms.customs.data.local.dao.PhaseRecordDao
import com.rms.customs.data.local.dao.TransactionDao
import com.rms.customs.data.local.entity.ActivityLogEntity
import com.rms.customs.data.local.entity.toDomain
import com.rms.customs.data.local.entity.toEntity
import com.rms.customs.domain.model.ActivityLog
import com.rms.customs.domain.model.PhaseRecord
import com.rms.customs.domain.model.Transaction
import com.rms.customs.domain.model.enums.LogAction
import com.rms.customs.domain.model.enums.PhaseStatus
import com.rms.customs.domain.model.enums.TransactionStatus
import com.rms.customs.domain.model.enums.toPhase
import com.rms.customs.domain.repository.SlaRepository
import com.rms.customs.domain.repository.TransactionRepository
import com.rms.customs.domain.statemachine.TransactionStateMachine
import com.rms.customs.domain.statemachine.TransitionResult
import com.rms.customs.domain.usecase.Phase4Tracks
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao,
    private val activityLogDao: ActivityLogDao,
    private val phaseRecordDao: PhaseRecordDao,
    private val slaRepository: SlaRepository,
    private val stateMachine: TransactionStateMachine,
) : TransactionRepository {

    override fun observeAll(): Flow<List<Transaction>> =
        transactionDao.observeAll().map { it.map { e -> e.toDomain() } }

    override fun observeActive(): Flow<List<Transaction>> =
        transactionDao.observeActive().map { it.map { e -> e.toDomain() } }

    override fun observeByStatus(vararg statuses: TransactionStatus): Flow<List<Transaction>> =
        transactionDao.observeByStatus(statuses.map { it.name })
            .map { it.map { e -> e.toDomain() } }

    override fun observeById(id: UUID): Flow<Transaction?> =
        transactionDao.observeById(id.toString()).map { it?.toDomain() }

    override suspend fun getById(id: UUID): Transaction? =
        transactionDao.getById(id.toString())?.toDomain()

    override suspend fun create(transaction: Transaction) {
        transactionDao.insert(transaction.toEntity())
        activityLogDao.insert(
            ActivityLogEntity(
                id = UUID.randomUUID().toString(),
                transactionId = transaction.id.toString(),
                userId = transaction.createdByUserId.toString(),
                action = LogAction.CREATED.name,
                fromStatus = null,
                toStatus = transaction.currentStatus.name,
                payload = "{}",
                occurredAt = transaction.createdAt,
            )
        )
    }

    override suspend fun update(transaction: Transaction) {
        transactionDao.update(transaction.toEntity())
    }

    override suspend fun advanceStatus(
        transactionId: UUID,
        newStatus: TransactionStatus,
        actorUserId: UUID,
        payload: String,
    ) {
        val existing = transactionDao.getById(transactionId.toString())
            ?.toDomain()
            ?: error("Transaction $transactionId not found")

        // Hard gate for GOV_APPROVED: all Phase 4 PhaseRecords must be DONE
        if (newStatus == TransactionStatus.GOV_APPROVED) {
            val incomplete = phaseRecordDao.countPhase4Incomplete(transactionId.toString())
            if (incomplete > 0) {
                error("لا يمكن الموافقة قبل اكتمال جميع مسارات المرحلة الرابعة / All Phase 4 tracks must be complete")
            }
        }

        val result = stateMachine.advance(existing, newStatus)
        if (result is TransitionResult.Failure) error(result.reason)

        val now = System.currentTimeMillis()
        val newPhase = if (newStatus.isException) existing.currentPhase else newStatus.toPhase()
        val updated = existing.copy(
            currentStatus = newStatus,
            currentPhase  = newPhase,
            exceptionState = if (newStatus.isException) newStatus else null,
            updatedAt = now,
            closedAt = if (newStatus == TransactionStatus.CLOSED) now else existing.closedAt,
        )
        val log = ActivityLogEntity(
            id = UUID.randomUUID().toString(),
            transactionId = transactionId.toString(),
            userId = actorUserId.toString(),
            action = LogAction.STATUS_CHANGED.name,
            fromStatus = existing.currentStatus.name,
            toStatus = newStatus.name,
            payload = payload,
            occurredAt = now,
        )
        transactionDao.advanceStatus(updated.toEntity(), log)

        // When entering Phase 4, seed the 4 gov-agency tracks (4.4 Ministry of Health starts SKIPPED).
        if (newStatus == TransactionStatus.GOV_PROCESSING) {
            val slaMap = mapOf(
                "4.1" to (slaRepository.getForSubPhase(4, "4.1")?.targetDays ?: 15),
                "4.2" to (slaRepository.getForSubPhase(4, "4.2")?.targetDays ?: 10),
                "4.3" to (slaRepository.getForSubPhase(4, "4.3")?.targetDays ?: 12),
                "4.4" to (slaRepository.getForSubPhase(4, "4.4")?.targetDays ?: 7),
            )
            val tracks = Phase4Tracks.createFor(transactionId, slaMap, now)
            phaseRecordDao.insertAll(tracks.map { it.toEntity() })
        }
    }

    override suspend fun setExceptionState(
        transactionId: UUID,
        exceptionStatus: TransactionStatus,
        reason: String,
        actorUserId: UUID,
    ) {
        require(exceptionStatus.isException) { "Not an exception state: $exceptionStatus" }
        val existing = transactionDao.getById(transactionId.toString())
            ?.toDomain() ?: error("Transaction not found")
        val now = System.currentTimeMillis()
        val updated = existing.copy(exceptionState = exceptionStatus, updatedAt = now)
        val log = ActivityLogEntity(
            id = UUID.randomUUID().toString(),
            transactionId = transactionId.toString(),
            userId = actorUserId.toString(),
            action = LogAction.EXCEPTION_SET.name,
            fromStatus = existing.currentStatus.name,
            toStatus = exceptionStatus.name,
            payload = """{"reason":"$reason"}""",
            occurredAt = now,
        )
        transactionDao.advanceStatus(updated.toEntity(), log)
    }

    override suspend fun clearExceptionState(transactionId: UUID, actorUserId: UUID) {
        val existing = transactionDao.getById(transactionId.toString())
            ?.toDomain() ?: error("Transaction not found")
        val now = System.currentTimeMillis()
        val updated = existing.copy(exceptionState = null, updatedAt = now)
        val log = ActivityLogEntity(
            id = UUID.randomUUID().toString(),
            transactionId = transactionId.toString(),
            userId = actorUserId.toString(),
            action = LogAction.EXCEPTION_CLEARED.name,
            fromStatus = existing.exceptionState?.name,
            toStatus = existing.currentStatus.name,
            payload = "{}",
            occurredAt = now,
        )
        transactionDao.advanceStatus(updated.toEntity(), log)
    }

    override fun observeActivityLog(transactionId: UUID): Flow<List<ActivityLog>> =
        activityLogDao.observeForTransaction(transactionId.toString())
            .map { it.map { e -> e.toDomain() } }

    override fun observePhaseRecords(transactionId: UUID): Flow<List<PhaseRecord>> =
        phaseRecordDao.observeForTransaction(transactionId.toString())
            .map { it.map { e -> e.toDomain() } }

    override suspend fun updatePhaseRecord(record: PhaseRecord) {
        phaseRecordDao.update(record.toEntity())
        transactionDao.bumpUpdatedAt(record.transactionId.toString(), System.currentTimeMillis())
    }

    override suspend fun completePhaseRecord(phaseRecordId: UUID, completedByUserId: UUID) {
        val now = System.currentTimeMillis()
        phaseRecordDao.markComplete(
            id          = phaseRecordId.toString(),
            status      = PhaseStatus.DONE.name,
            completedAt = now,
            userId      = completedByUserId.toString(),
        )
        val txId = phaseRecordDao.getById(phaseRecordId.toString())?.transactionId ?: return
        transactionDao.bumpUpdatedAt(txId, now)
    }

    override suspend fun countByStatus(status: TransactionStatus): Int =
        transactionDao.countByStatus(status.name)

    override suspend fun countOverdueSla(): Int =
        transactionDao.countOverdueSla(System.currentTimeMillis())

    override suspend fun generateRef(): String {
        val year = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        val prefix = "RMS-$year-"
        val count = transactionDao.countWithPrefix(prefix)
        return "$prefix${(count + 1).toString().padStart(4, '0')}"
    }

    override suspend fun getActivePhasesForSlaCheck(): List<PhaseRecord> =
        phaseRecordDao.getActivePhasesForSlaCheck().map { it.toDomain() }

    override fun observeAllInProgressPhases(): Flow<List<PhaseRecord>> =
        phaseRecordDao.observeAllInProgress().map { it.map { e -> e.toDomain() } }

    override fun observeCompletedPhasesBySubPhase(subPhase: String): Flow<List<PhaseRecord>> =
        phaseRecordDao.observeCompletedBySubPhase(subPhase).map { it.map { e -> e.toDomain() } }
}
