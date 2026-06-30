package com.rms.customs.domain.statemachine

import com.rms.customs.domain.model.Transaction
import com.rms.customs.domain.model.enums.Priority
import com.rms.customs.domain.model.enums.TransactionPhase
import com.rms.customs.domain.model.enums.TransactionStatus
import com.rms.customs.domain.model.enums.TransactionStatus.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

class TransactionStateMachineTest {

    private lateinit var machine: TransactionStateMachine

    @Before
    fun setUp() {
        machine = TransactionStateMachine()
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private fun tx(status: TransactionStatus, exception: TransactionStatus? = null) = Transaction(
        id = UUID.randomUUID(),
        transactionRef = "RMS-TEST-0001",
        title = "Test",
        supplierName = "Test Supplier",
        currentPhase = TransactionPhase.PHASE_1_TENDER,
        currentStatus = status,
        exceptionState = exception,
        createdAt = 0L,
        createdByUserId = UUID.randomUUID(),
        updatedAt = 0L,
    )

    private fun assertSuccess(result: TransitionResult) =
        assertTrue("Expected Success but got: $result", result is TransitionResult.Success)

    private fun assertFailure(result: TransitionResult, msgContains: String? = null) {
        assertTrue("Expected Failure but got: $result", result is TransitionResult.Failure)
        if (msgContains != null) {
            assertTrue(
                "Failure message '${(result as TransitionResult.Failure).reason}' should contain '$msgContains'",
                result.reason.contains(msgContains, ignoreCase = true)
            )
        }
    }

    // ── Valid sequential transitions ───────────────────────────────────────

    @Test fun `DRAFT to TENDER_PREPARATION succeeds`() =
        assertSuccess(machine.advance(tx(DRAFT), TENDER_PREPARATION))

    @Test fun `TENDER_PREPARATION to TENDER_PUBLISHED succeeds`() =
        assertSuccess(machine.advance(tx(TENDER_PREPARATION), TENDER_PUBLISHED))

    @Test fun `TENDER_PUBLISHED to EVALUATION_IN_PROGRESS succeeds`() =
        assertSuccess(machine.advance(tx(TENDER_PUBLISHED), EVALUATION_IN_PROGRESS))

    @Test fun `EVALUATION_IN_PROGRESS to CONTRACT_PENDING_SIGNATURE succeeds`() =
        assertSuccess(machine.advance(tx(EVALUATION_IN_PROGRESS), CONTRACT_PENDING_SIGNATURE))

    @Test fun `CONTRACT_PENDING_SIGNATURE to CONTRACT_SIGNED succeeds`() =
        assertSuccess(machine.advance(tx(CONTRACT_PENDING_SIGNATURE), CONTRACT_SIGNED))

    @Test fun `CONTRACT_SIGNED to CLEARANCE_DOCS_PREPARATION succeeds`() =
        assertSuccess(machine.advance(tx(CONTRACT_SIGNED), CLEARANCE_DOCS_PREPARATION))

    @Test fun `CLEARANCE_DOCS_PREPARATION to DECLARATION_SUBMITTED succeeds`() =
        assertSuccess(machine.advance(tx(CLEARANCE_DOCS_PREPARATION), DECLARATION_SUBMITTED))

    @Test fun `DECLARATION_SUBMITTED to GOV_PROCESSING succeeds`() =
        assertSuccess(machine.advance(tx(DECLARATION_SUBMITTED), GOV_PROCESSING))

    @Test fun `GOV_PROCESSING to GOV_APPROVED succeeds`() =
        assertSuccess(machine.advance(tx(GOV_PROCESSING), GOV_APPROVED))

    @Test fun `GOV_APPROVED to FINAL_RELEASE_ISSUED succeeds`() =
        assertSuccess(machine.advance(tx(GOV_APPROVED), FINAL_RELEASE_ISSUED))

    @Test fun `FINAL_RELEASE_ISSUED to IN_TRANSIT succeeds`() =
        assertSuccess(machine.advance(tx(FINAL_RELEASE_ISSUED), IN_TRANSIT))

    @Test fun `IN_TRANSIT to RECEIVED_AT_WAREHOUSE succeeds`() =
        assertSuccess(machine.advance(tx(IN_TRANSIT), RECEIVED_AT_WAREHOUSE))

    @Test fun `RECEIVED_AT_WAREHOUSE to INSPECTION_COMPLETE succeeds`() =
        assertSuccess(machine.advance(tx(RECEIVED_AT_WAREHOUSE), INSPECTION_COMPLETE))

    @Test fun `INSPECTION_COMPLETE to FINANCIAL_SETTLEMENT_PENDING succeeds`() =
        assertSuccess(machine.advance(tx(INSPECTION_COMPLETE), FINANCIAL_SETTLEMENT_PENDING))

    @Test fun `FINANCIAL_SETTLEMENT_PENDING to CLOSED succeeds`() =
        assertSuccess(machine.advance(tx(FINANCIAL_SETTLEMENT_PENDING), CLOSED))

    // ── Invalid skip transitions ───────────────────────────────────────────

    @Test fun `DRAFT cannot skip to GOV_PROCESSING`() =
        assertFailure(machine.advance(tx(DRAFT), GOV_PROCESSING))

    @Test fun `TENDER_PREPARATION cannot skip to CONTRACT_SIGNED`() =
        assertFailure(machine.advance(tx(TENDER_PREPARATION), CONTRACT_SIGNED))

    @Test fun `CONTRACT_SIGNED cannot skip to FINAL_RELEASE_ISSUED`() =
        assertFailure(machine.advance(tx(CONTRACT_SIGNED), FINAL_RELEASE_ISSUED))

    // ── Invalid reverse transitions ────────────────────────────────────────

    @Test fun `CONTRACT_SIGNED cannot go back to TENDER_PREPARATION`() =
        assertFailure(machine.advance(tx(CONTRACT_SIGNED), TENDER_PREPARATION))

    @Test fun `GOV_APPROVED cannot go back to EVALUATION_IN_PROGRESS`() =
        assertFailure(machine.advance(tx(GOV_APPROVED), EVALUATION_IN_PROGRESS))

    // ── Terminal state ─────────────────────────────────────────────────────

    @Test fun `CLOSED rejects any further transition`() {
        assertFailure(machine.advance(tx(CLOSED), FINANCIAL_SETTLEMENT_PENDING))
        assertFailure(machine.advance(tx(CLOSED), TENDER_PREPARATION))
        assertFailure(machine.advance(tx(CLOSED), BLOCKED))
    }

    // ── Hard Gate 1: DECLARATION_SUBMITTED requires CONTRACT_SIGNED history ─

    @Test fun `Hard Gate 1 - CLEARANCE_DOCS_PREPARATION to DECLARATION_SUBMITTED passes (contract milestone already behind)`() {
        // currentStatus is CLEARANCE_DOCS_PREPARATION which comes after CONTRACT_SIGNED
        assertSuccess(machine.advance(tx(CLEARANCE_DOCS_PREPARATION), DECLARATION_SUBMITTED))
    }

    // ── Hard Gate 3: IN_TRANSIT requires FINAL_RELEASE_ISSUED ──────────────

    @Test fun `Hard Gate 3 - cannot transit without FINAL_RELEASE_ISSUED`() {
        // Try to skip to IN_TRANSIT from a state before FINAL_RELEASE_ISSUED
        val result = machine.advance(tx(GOV_APPROVED), IN_TRANSIT)
        assertFailure(result)
    }

    @Test fun `Hard Gate 3 - IN_TRANSIT succeeds when current is FINAL_RELEASE_ISSUED`() =
        assertSuccess(machine.advance(tx(FINAL_RELEASE_ISSUED), IN_TRANSIT))

    // ── canAdvance helper ──────────────────────────────────────────────────

    @Test fun `canAdvance returns true for valid transition`() =
        assertTrue(machine.canAdvance(DRAFT, TENDER_PREPARATION))

    @Test fun `canAdvance returns false for invalid transition`() =
        assertTrue(!machine.canAdvance(DRAFT, CLOSED))

    // ── Result content ─────────────────────────────────────────────────────

    @Test fun `Success carries the new status`() {
        val result = machine.advance(tx(DRAFT), TENDER_PREPARATION) as TransitionResult.Success
        assertEquals(TENDER_PREPARATION, result.newStatus)
    }
}
