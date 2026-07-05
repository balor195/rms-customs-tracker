package com.rms.customs.domain.model.enums

enum class TransactionStatus {
    // ── Phase 1: Tender Preparation ──────────────────────────────────────
    DRAFT,
    TENDER_PREPARATION,
    TENDER_PUBLISHED,

    // ── Phase 2: Clearance Request ───────────────────────────────────────
    CLEARANCE_ISSUED,                  // "تم التخليص" — Hard Gate: nothing after without this

    // ── Phase 3: Financial Settlement ────────────────────────────────────
    FINANCIAL_SETTLEMENT_PENDING,
    CLOSED,

    // ── Phase 4: Warehouse Transfer Confirmation ─────────────────────────
    TRANSFERRED_TO_WAREHOUSE,         // terminal state — "تم النقل الى المستودعات"

    // ── Exception overlays (stored separately in exceptionState) ─────────
    BLOCKED,
    ON_HOLD,
    DISPUTED;

    val isTerminal: Boolean get() = this == TRANSFERRED_TO_WAREHOUSE
    val isException: Boolean get() = this in setOf(BLOCKED, ON_HOLD, DISPUTED)
}
