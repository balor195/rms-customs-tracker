package com.rms.customs.domain.model.enums

enum class TransactionStatus {
    // ── Phase 1: Tender Preparation ──────────────────────────────────────
    DRAFT,
    TENDER_PREPARATION,

    // ── Phase 2: Shipment Arrived at Airport ─────────────────────────────
    ARRIVED_AT_AIRPORT,                // "وصلت الشحنة للمطار" — Tender Officer's exclusive action

    // ── Phase 3: Clearance ────────────────────────────────────────────────
    CLEARANCE_ISSUED,                  // "تم التخليص" — Clearance's exclusive action

    // ── Phase 4: Warehouse Transfer Confirmation ─────────────────────────
    TRANSFERRED_TO_WAREHOUSE,          // "تم النقل الى المستودعات" — terminal, Warehouse's exclusive action

    // ── Exception overlays (stored separately in exceptionState) ─────────
    BLOCKED,
    ON_HOLD,
    DISPUTED;

    val isTerminal: Boolean get() = this == TRANSFERRED_TO_WAREHOUSE
    val isException: Boolean get() = this in setOf(BLOCKED, ON_HOLD, DISPUTED)
}
