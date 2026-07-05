package com.rms.customs.domain.model.enums

fun TransactionStatus.toPhase(): TransactionPhase = when (this) {
    TransactionStatus.DRAFT,
    TransactionStatus.TENDER_PREPARATION             -> TransactionPhase.PHASE_1_TENDER

    TransactionStatus.ARRIVED_AT_AIRPORT             -> TransactionPhase.PHASE_2_AIRPORT_ARRIVAL

    TransactionStatus.CLEARANCE_ISSUED               -> TransactionPhase.PHASE_3_CLEARANCE

    TransactionStatus.TRANSFERRED_TO_WAREHOUSE       -> TransactionPhase.PHASE_4_WAREHOUSE_CONFIRMATION

    // Exception overlays: phase stays at the transaction's currentStatus phase
    TransactionStatus.BLOCKED,
    TransactionStatus.ON_HOLD,
    TransactionStatus.DISPUTED                       -> TransactionPhase.PHASE_1_TENDER
}
