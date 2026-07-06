package com.rms.customs.data.repository

import com.rms.customs.data.local.dao.ActivityLogDao
import com.rms.customs.data.local.dao.TransactionDao
import com.rms.customs.data.local.entity.ActivityLogEntity
import com.rms.customs.data.local.entity.toDomain
import com.rms.customs.data.local.entity.toEntity
import com.rms.customs.domain.model.ActivityLog
import com.rms.customs.domain.model.AppNotification
import com.rms.customs.domain.model.NotificationType
import com.rms.customs.domain.model.Transaction
import com.rms.customs.domain.model.enums.LogAction
import com.rms.customs.domain.model.enums.TransactionStatus
import com.rms.customs.domain.model.enums.UserRole
import com.rms.customs.domain.model.enums.toPhase
import com.rms.customs.domain.repository.NotificationRepository
import com.rms.customs.domain.repository.TransactionRepository
import com.rms.customs.domain.statemachine.TransactionStateMachine
import com.rms.customs.domain.statemachine.TransitionResult
import com.rms.customs.notifications.CustomsNotificationManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class TransactionRepositoryImpl(
    private val transactionDao: TransactionDao,
    private val activityLogDao: ActivityLogDao,
    private val notificationRepository: NotificationRepository,
    private val notificationManager: CustomsNotificationManager,
    private val stateMachine: TransactionStateMachine,
) : TransactionRepository {

    override fun observeAll(): Flow<List<Transaction>> =
        transactionDao.observeAll().map { it.map { e -> e.toDomain() } }

    override fun observeActive(): Flow<List<Transaction>> =
        transactionDao.observeActive().map { it.map { e -> e.toDomain() } }

    override fun observeByStatus(vararg statuses: TransactionStatus): Flow<List<Transaction>> =
        transactionDao.observeByStatus(statuses.map { it.name })
            .map { it.map { e -> e.toDomain() } }

    override fun observeById(id: String): Flow<Transaction?> =
        transactionDao.observeById(id).map { it?.toDomain() }

    override suspend fun getById(id: String): Transaction? =
        transactionDao.getById(id)?.toDomain()

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun create(transaction: Transaction) {
        transactionDao.insert(transaction.toEntity())
        activityLogDao.insert(
            ActivityLogEntity(
                id = Uuid.random().toString(),
                transactionId = transaction.id,
                userId = transaction.createdByUserId,
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

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun advanceStatus(
        transactionId: String,
        newStatus: TransactionStatus,
        actorUserId: String,
        actorRole: UserRole,
        payload: String,
    ) {
        val existing = transactionDao.getById(transactionId)
            ?.toDomain()
            ?: error("Transaction $transactionId not found")

        // Defense in depth: the UI hides the relevant button/checkbox for unauthorized roles,
        // but that alone is not a security boundary — enforce the same restriction here too.
        // actorRole is the caller's *effective* role (i.e. the simulated role during an admin's
        // "view as" session) rather than something re-derived from actorUserId's DB record —
        // re-deriving from the DB would always resolve to the real admin during "view as" and
        // silently authorize actions the simulated role should be blocked from.
        if (newStatus == TransactionStatus.CLEARANCE_ISSUED && !actorRole.canMarkClearanceDone) {
            error("لا تملك صلاحية تنفيذ التخليص / You do not have permission to mark clearance done")
        }
        if (newStatus == TransactionStatus.TRANSFERRED_TO_WAREHOUSE && !actorRole.canMarkWarehouseTransferred) {
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
            actualArrivalDate = if (newStatus == TransactionStatus.ARRIVED_AT_AIRPORT) now else existing.actualArrivalDate,
            closedAt = if (newStatus == TransactionStatus.TRANSFERRED_TO_WAREHOUSE) now else existing.closedAt,
        )
        val log = ActivityLogEntity(
            id = Uuid.random().toString(),
            transactionId = transactionId,
            userId = actorUserId,
            action = LogAction.STATUS_CHANGED.name,
            fromStatus = existing.currentStatus.name,
            toStatus = newStatus.name,
            payload = payload,
            occurredAt = now,
        )
        transactionDao.advanceStatus(updated.toEntity(), log)

        // Notify every account on this device that the transaction is now fully closed.
        if (newStatus == TransactionStatus.TRANSFERRED_TO_WAREHOUSE) {
            val notification = AppNotification(
                id            = Uuid.random().toString(),
                transactionId = transactionId,
                type          = NotificationType.TRANSACTION_CLOSED,
                titleAr       = "تم إغلاق المعاملة",
                titleEn       = "Transaction Closed",
                messageAr     = "تم نقل شحنة المعاملة ${existing.transactionRef} إلى المستودعات وأُغلقت المعاملة",
                messageEn     = "Transaction ${existing.transactionRef} was transferred to warehouses and closed",
                isRead        = false,
                createdAt     = now,
            )
            notificationRepository.create(notification)
            notificationManager.postTransactionClosedNotification(notification, transactionId.hashCode())
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun setExceptionState(
        transactionId: String,
        exceptionStatus: TransactionStatus,
        reason: String,
        actorUserId: String,
    ) {
        require(exceptionStatus.isException) { "Not an exception state: $exceptionStatus" }
        val existing = transactionDao.getById(transactionId)
            ?.toDomain() ?: error("Transaction not found")
        val now = System.currentTimeMillis()
        val updated = existing.copy(exceptionState = exceptionStatus, updatedAt = now)
        val log = ActivityLogEntity(
            id = Uuid.random().toString(),
            transactionId = transactionId,
            userId = actorUserId,
            action = LogAction.EXCEPTION_SET.name,
            fromStatus = existing.currentStatus.name,
            toStatus = exceptionStatus.name,
            payload = """{"reason":"$reason"}""",
            occurredAt = now,
        )
        transactionDao.advanceStatus(updated.toEntity(), log)
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun clearExceptionState(transactionId: String, actorUserId: String) {
        val existing = transactionDao.getById(transactionId)
            ?.toDomain() ?: error("Transaction not found")
        val now = System.currentTimeMillis()
        val updated = existing.copy(exceptionState = null, updatedAt = now)
        val log = ActivityLogEntity(
            id = Uuid.random().toString(),
            transactionId = transactionId,
            userId = actorUserId,
            action = LogAction.EXCEPTION_CLEARED.name,
            fromStatus = existing.exceptionState?.name,
            toStatus = existing.currentStatus.name,
            payload = "{}",
            occurredAt = now,
        )
        transactionDao.advanceStatus(updated.toEntity(), log)
    }

    override fun observeActivityLog(transactionId: String): Flow<List<ActivityLog>> =
        activityLogDao.observeForTransaction(transactionId)
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
