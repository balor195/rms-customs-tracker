package com.rms.customs.domain.statemachine

import com.rms.customs.domain.model.Transaction
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

    @Test fun `TENDER_PUBLISHED to CLEARANCE_ISSUED succeeds`() =
        assertSuccess(machine.advance(tx(TENDER_PUBLISHED), CLEARANCE_ISSUED))

    @Test fun `CLEARANCE_ISSUED to FINANCIAL_SETTLEMENT_PENDING succeeds`() =
        assertSuccess(machine.advance(tx(CLEARANCE_ISSUED), FINANCIAL_SETTLEMENT_PENDING))

    @Test fun `FINANCIAL_SETTLEMENT_PENDING to CLOSED succeeds`() =
        assertSuccess(machine.advance(tx(FINANCIAL_SETTLEMENT_PENDING), CLOSED))

    @Test fun `CLOSED to TRANSFERRED_TO_WAREHOUSE succeeds`() =
        assertSuccess(machine.advance(tx(CLOSED), TRANSFERRED_TO_WAREHOUSE))

    // ── Invalid skip transitions ───────────────────────────────────────────

    @Test fun `DRAFT cannot skip to CLEARANCE_ISSUED`() =
        assertFailure(machine.advance(tx(DRAFT), CLEARANCE_ISSUED))

    @Test fun `TENDER_PREPARATION cannot skip to FINANCIAL_SETTLEMENT_PENDING`() =
        assertFailure(machine.advance(tx(TENDER_PREPARATION), FINANCIAL_SETTLEMENT_PENDING))

    @Test fun `CLEARANCE_ISSUED cannot skip to CLOSED`() =
        assertFailure(machine.advance(tx(CLEARANCE_ISSUED), CLOSED))

    @Test fun `CLEARANCE_ISSUED cannot skip to TRANSFERRED_TO_WAREHOUSE`() =
        assertFailure(machine.advance(tx(CLEARANCE_ISSUED), TRANSFERRED_TO_WAREHOUSE))

    // ── Invalid reverse transitions ────────────────────────────────────────

    @Test fun `CLEARANCE_ISSUED cannot go back to TENDER_PREPARATION`() =
        assertFailure(machine.advance(tx(CLEARANCE_ISSUED), TENDER_PREPARATION))

    @Test fun `CLOSED cannot go back to FINANCIAL_SETTLEMENT_PENDING`() =
        assertFailure(machine.advance(tx(CLOSED), FINANCIAL_SETTLEMENT_PENDING))

    // ── Terminal state ─────────────────────────────────────────────────────

    @Test fun `TRANSFERRED_TO_WAREHOUSE rejects any further transition`() {
        assertFailure(machine.advance(tx(TRANSFERRED_TO_WAREHOUSE), CLOSED))
        assertFailure(machine.advance(tx(TRANSFERRED_TO_WAREHOUSE), TENDER_PREPARATION))
        assertFailure(machine.advance(tx(TRANSFERRED_TO_WAREHOUSE), BLOCKED))
    }

    // ── Hard Gate: TRANSFERRED_TO_WAREHOUSE requires CLOSED ─────────────────

    @Test fun `Hard Gate - cannot confirm warehouse transfer before financial closing`() {
        val result = machine.advance(tx(FINANCIAL_SETTLEMENT_PENDING), TRANSFERRED_TO_WAREHOUSE)
        assertFailure(result)
    }

    @Test fun `Hard Gate - TRANSFERRED_TO_WAREHOUSE succeeds when current is CLOSED`() =
        assertSuccess(machine.advance(tx(CLOSED), TRANSFERRED_TO_WAREHOUSE))

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
