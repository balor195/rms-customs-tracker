package com.rms.customs.domain.model.enums

fun TransactionStatus.toPhase(): TransactionPhase = when (this) {
    TransactionStatus.DRAFT,
    TransactionStatus.TENDER_PREPARATION,
    TransactionStatus.TENDER_PUBLISHED              -> TransactionPhase.PHASE_1_TENDER

    TransactionStatus.EVALUATION_IN_PROGRESS,
    TransactionStatus.CONTRACT_PENDING_SIGNATURE,
    TransactionStatus.CONTRACT_SIGNED               -> TransactionPhase.PHASE_2_EVALUATION

    TransactionStatus.CLEARANCE_DOCS_PREPARATION,
    TransactionStatus.DECLARATION_SUBMITTED         -> TransactionPhase.PHASE_3_CLEARANCE_PREP

    TransactionStatus.GOV_PROCESSING,
    TransactionStatus.GOV_APPROVED                  -> TransactionPhase.PHASE_4_GOV_AGENCIES

    TransactionStatus.FINAL_RELEASE_ISSUED          -> TransactionPhase.PHASE_5_RELEASE

    TransactionStatus.IN_TRANSIT,
    TransactionStatus.RECEIVED_AT_WAREHOUSE,
    TransactionStatus.INSPECTION_COMPLETE           -> TransactionPhase.PHASE_6_TRANSIT

    TransactionStatus.FINANCIAL_SETTLEMENT_PENDING,
    TransactionStatus.CLOSED                        -> TransactionPhase.PHASE_7_FINANCIAL

    // Exception overlays: phase stays at the transaction's currentStatus phase
    TransactionStatus.BLOCKED,
    TransactionStatus.ON_HOLD,
    TransactionStatus.DISPUTED                      -> TransactionPhase.PHASE_1_TENDER
}
