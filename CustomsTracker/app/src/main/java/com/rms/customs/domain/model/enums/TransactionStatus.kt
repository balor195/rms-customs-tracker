package com.rms.customs.domain.model.enums

enum class TransactionStatus {
    // ── Phase 1: Tender Preparation ──────────────────────────────────────
    DRAFT,
    TENDER_PREPARATION,
    TENDER_PUBLISHED,

    // ── Phase 2: Evaluation & Contract ───────────────────────────────────
    EVALUATION_IN_PROGRESS,
    CONTRACT_PENDING_SIGNATURE,
    CONTRACT_SIGNED,                  // Hard Gate: nothing after without this

    // ── Phase 3: Clearance Documentation ─────────────────────────────────
    CLEARANCE_DOCS_PREPARATION,
    DECLARATION_SUBMITTED,

    // ── Phase 4: Gov-Agency Processing (parallel tracks) ─────────────────
    GOV_PROCESSING,                   // parallel PhaseRecords active
    GOV_APPROVED,                     // all three tracks complete

    // ── Phase 5: Release ──────────────────────────────────────────────────
    FINAL_RELEASE_ISSUED,             // Hard Gate: no transit without this

    // ── Phase 6: Transit & Receipt ───────────────────────────────────────
    IN_TRANSIT,
    RECEIVED_AT_WAREHOUSE,
    INSPECTION_COMPLETE,              // receipt minutes signed

    // ── Phase 7: Financial Settlement ────────────────────────────────────
    FINANCIAL_SETTLEMENT_PENDING,
    CLOSED,                           // terminal state

    // ── Exception overlays (stored separately in exceptionState) ─────────
    BLOCKED,
    ON_HOLD,
    DISPUTED;

    val isTerminal: Boolean get() = this == CLOSED
    val isException: Boolean get() = this in setOf(BLOCKED, ON_HOLD, DISPUTED)
}
