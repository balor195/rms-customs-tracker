package com.rms.customs.domain.statemachine

import com.rms.customs.domain.model.Transaction
import com.rms.customs.domain.model.enums.TransactionStatus
import com.rms.customs.domain.model.enums.TransactionStatus.*
import javax.inject.Inject
import javax.inject.Singleton

sealed class TransitionResult {
    data class Success(val newStatus: TransactionStatus) : TransitionResult()
    data class Failure(val reason: String) : TransitionResult()
}

@Singleton
class TransactionStateMachine @Inject constructor() {

    private val allowedTransitions: Map<TransactionStatus, Set<TransactionStatus>> = mapOf(
        DRAFT                        to setOf(TENDER_PREPARATION),
        TENDER_PREPARATION           to setOf(TENDER_PUBLISHED),
        TENDER_PUBLISHED             to setOf(CLEARANCE_ISSUED),
        CLEARANCE_ISSUED             to setOf(FINANCIAL_SETTLEMENT_PENDING),
        FINANCIAL_SETTLEMENT_PENDING to setOf(CLOSED),
        CLOSED                       to setOf(TRANSFERRED_TO_WAREHOUSE),
        // Exception state transitions
        BLOCKED                      to setOf(TENDER_PREPARATION, FINANCIAL_SETTLEMENT_PENDING),
        ON_HOLD                      to setOf(TENDER_PREPARATION, FINANCIAL_SETTLEMENT_PENDING),
        DISPUTED                     to setOf(FINANCIAL_SETTLEMENT_PENDING),
    )

    fun canAdvance(from: TransactionStatus, to: TransactionStatus): Boolean =
        allowedTransitions[from]?.contains(to) == true

    fun nextForwardStatus(current: TransactionStatus): TransactionStatus? =
        allowedTransitions[current]?.firstOrNull { !it.isException }

    fun advance(transaction: Transaction, to: TransactionStatus): TransitionResult {
        val from = transaction.exceptionState ?: transaction.currentStatus

        if (from.isTerminal) {
            return TransitionResult.Failure("معاملة مغلقة لا يمكن تعديلها / Transaction is closed")
        }

        if (!canAdvance(from, to)) {
            return TransitionResult.Failure(
                "انتقال غير مسموح: $from → $to / Transition not allowed: $from → $to"
            )
        }

        // Hard gates
        val gateFailure = checkHardGates(transaction, to)
        if (gateFailure != null) return gateFailure

        return TransitionResult.Success(to)
    }

    private fun checkHardGates(transaction: Transaction, to: TransactionStatus): TransitionResult.Failure? {
        // Gate: No warehouse-transfer confirmation without financial closing — this is also
        // the transaction's closing action (isTerminal), performed only by WAREHOUSE/ADMIN.
        if (to == TRANSFERRED_TO_WAREHOUSE && transaction.currentStatus != CLOSED) {
            return TransitionResult.Failure(
                "لا يمكن تأكيد النقل للمستودعات قبل إغلاق المعاملة مالياً / Cannot confirm warehouse transfer before financial closing"
            )
        }

        return null
    }
}
