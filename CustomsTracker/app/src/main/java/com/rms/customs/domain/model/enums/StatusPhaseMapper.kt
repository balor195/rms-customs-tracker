package com.rms.customs.domain.model.enums

fun TransactionStatus.toPhase(): TransactionPhase = when (this) {
    TransactionStatus.DRAFT,
    TransactionStatus.TENDER_PREPARATION,
    TransactionStatus.TENDER_PUBLISHED              -> TransactionPhase.PHASE_1_TENDER

    TransactionStatus.CLEARANCE_ISSUED               -> TransactionPhase.PHASE_2_CLEARANCE

    TransactionStatus.FINANCIAL_SETTLEMENT_PENDING,
    TransactionStatus.CLOSED                        -> TransactionPhase.PHASE_3_FINANCIAL

    TransactionStatus.TRANSFERRED_TO_WAREHOUSE      -> TransactionPhase.PHASE_4_WAREHOUSE_CONFIRMATION

    // Exception overlays: phase stays at the transaction's currentStatus phase
    TransactionStatus.BLOCKED,
    TransactionStatus.ON_HOLD,
    TransactionStatus.DISPUTED                      -> TransactionPhase.PHASE_1_TENDER
}
