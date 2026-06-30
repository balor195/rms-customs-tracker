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
        TENDER_PUBLISHED             to setOf(EVALUATION_IN_PROGRESS),
        EVALUATION_IN_PROGRESS       to setOf(CONTRACT_PENDING_SIGNATURE),
        CONTRACT_PENDING_SIGNATURE   to setOf(CONTRACT_SIGNED),
        CONTRACT_SIGNED              to setOf(CLEARANCE_DOCS_PREPARATION),
        CLEARANCE_DOCS_PREPARATION   to setOf(DECLARATION_SUBMITTED),
        DECLARATION_SUBMITTED        to setOf(GOV_PROCESSING),
        GOV_PROCESSING               to setOf(GOV_APPROVED),
        GOV_APPROVED                 to setOf(FINAL_RELEASE_ISSUED),
        FINAL_RELEASE_ISSUED         to setOf(IN_TRANSIT),
        IN_TRANSIT                   to setOf(RECEIVED_AT_WAREHOUSE),
        RECEIVED_AT_WAREHOUSE        to setOf(INSPECTION_COMPLETE),
        INSPECTION_COMPLETE          to setOf(FINANCIAL_SETTLEMENT_PENDING),
        FINANCIAL_SETTLEMENT_PENDING to setOf(CLOSED),
        // Exception state transitions
        BLOCKED                      to setOf(TENDER_PREPARATION, EVALUATION_IN_PROGRESS,
                                               CLEARANCE_DOCS_PREPARATION, GOV_PROCESSING,
                                               IN_TRANSIT, FINANCIAL_SETTLEMENT_PENDING),
        ON_HOLD                      to setOf(TENDER_PREPARATION, EVALUATION_IN_PROGRESS,
                                               CLEARANCE_DOCS_PREPARATION, GOV_PROCESSING,
                                               IN_TRANSIT, FINANCIAL_SETTLEMENT_PENDING),
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
        // Gate 1: Cannot submit customs declaration without a signed contract
        if (to == DECLARATION_SUBMITTED) {
            val contractReached = hasPassedStatus(transaction.currentStatus, CONTRACT_SIGNED)
            if (!contractReached) {
                return TransitionResult.Failure(
                    "لا يمكن تقديم التصريح الجمركي قبل توقيع العقد / Cannot declare before contract is signed"
                )
            }
        }
        // Gate 2: Cannot issue final release without Military Command approval
        // (enforced structurally — GOV_PROCESSING → GOV_APPROVED requires all PhaseRecords done;
        //  the repository layer validates that Military PhaseRecord is DONE before setting GOV_APPROVED)

        // Gate 3: No shipment before FINAL_RELEASE_ISSUED
        if (to == IN_TRANSIT && transaction.currentStatus != FINAL_RELEASE_ISSUED) {
            return TransitionResult.Failure(
                "لا يمكن تحريك الشحنة قبل أمر الإفراج النهائي / Cannot move shipment before final release"
            )
        }
        // Gate 4: No financial close without signed receipt minutes
        // (enforced structurally — INSPECTION_COMPLETE requires receiving minutes doc to be verified)

        return null
    }

    private fun hasPassedStatus(current: TransactionStatus, milestone: TransactionStatus): Boolean {
        val order = listOf(
            DRAFT, TENDER_PREPARATION, TENDER_PUBLISHED, EVALUATION_IN_PROGRESS,
            CONTRACT_PENDING_SIGNATURE, CONTRACT_SIGNED, CLEARANCE_DOCS_PREPARATION,
            DECLARATION_SUBMITTED, GOV_PROCESSING, GOV_APPROVED, FINAL_RELEASE_ISSUED,
            IN_TRANSIT, RECEIVED_AT_WAREHOUSE, INSPECTION_COMPLETE,
            FINANCIAL_SETTLEMENT_PENDING, CLOSED
        )
        val currentIdx  = order.indexOf(current)
        val milestoneIdx = order.indexOf(milestone)
        return currentIdx >= milestoneIdx
    }
}
