package com.rms.customs.data.repository

import com.rms.customs.data.local.dao.ActivityLogDao
import com.rms.customs.data.local.dao.TransactionDao
import com.rms.customs.data.local.dao.UserDao
import com.rms.customs.data.local.entity.ActivityLogEntity
import com.rms.customs.data.local.entity.toDomain
import com.rms.customs.data.local.entity.toEntity
import com.rms.customs.domain.model.ActivityLog
import com.rms.customs.domain.model.Transaction
import com.rms.customs.domain.model.enums.LogAction
import com.rms.customs.domain.model.enums.TransactionStatus
import com.rms.customs.domain.model.enums.UserRole
import com.rms.customs.domain.model.enums.toPhase
import com.rms.customs.domain.repository.TransactionRepository
import com.rms.customs.domain.statemachine.TransactionStateMachine
import com.rms.customs.domain.statemachine.TransitionResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao,
    private val activityLogDao: ActivityLogDao,
    private val userDao: UserDao,
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

        // Defense in depth: the UI hides the relevant button/checkbox for unauthorized roles,
        // but that alone is not a security boundary — enforce the same restriction here too,
        // so no UI edge case can let an unauthorized actor perform these two exclusive actions.
        val actorRole = userDao.getById(actorUserId.toString())?.role?.let { runCatching { UserRole.valueOf(it) }.getOrNull() }
        if (newStatus == TransactionStatus.CLEARANCE_ISSUED && actorRole?.canMarkClearanceDone != true) {
            error("لا تملك صلاحية تنفيذ التخليص / You do not have permission to mark clearance done")
        }
        if (newStatus == TransactionStatus.TRANSFERRED_TO_WAREHOUSE && actorRole?.canMarkWarehouseTransferred != true) {
            error("لا تملك صلاحية تأكيد النقل للمستودعات / You do not have permission to confirm warehouse transfer")
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
            closedAt = if (newStatus == TransactionStatus.TRANSFERRED_TO_WAREHOUSE) now else existing.closedAt,
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

    override suspend fun countByStatus(status: TransactionStatus): Int =
        transactionDao.countByStatus(status.name)

    override suspend fun generateRef(): String {
        val year = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        val prefix = "RMS-$year-"
        val count = transactionDao.countWithPrefix(prefix)
        return "$prefix${(count + 1).toString().padStart(4, '0')}"
    }
}
