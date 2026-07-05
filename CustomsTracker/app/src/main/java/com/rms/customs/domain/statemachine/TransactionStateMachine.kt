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
        DRAFT               to setOf(TENDER_PREPARATION),
        TENDER_PREPARATION  to setOf(ARRIVED_AT_AIRPORT),
        ARRIVED_AT_AIRPORT  to setOf(CLEARANCE_ISSUED),
        CLEARANCE_ISSUED    to setOf(TRANSFERRED_TO_WAREHOUSE),
        // Exception state transitions
        BLOCKED             to setOf(TENDER_PREPARATION, ARRIVED_AT_AIRPORT),
        ON_HOLD             to setOf(TENDER_PREPARATION, ARRIVED_AT_AIRPORT),
        DISPUTED            to setOf(CLEARANCE_ISSUED),
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

        return TransitionResult.Success(to)
    }
}
