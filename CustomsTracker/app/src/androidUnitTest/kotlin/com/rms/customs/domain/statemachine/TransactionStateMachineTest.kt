package com.rms.customs.domain.statemachine

import com.rms.customs.domain.model.Transaction
import com.rms.customs.domain.model.enums.TransactionPhase
import com.rms.customs.domain.model.enums.TransactionStatus
import com.rms.customs.domain.model.enums.TransactionStatus.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class TransactionStateMachineTest {

    private lateinit var machine: TransactionStateMachine

    @Before
    fun setUp() {
        machine = TransactionStateMachine()
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private fun tx(status: TransactionStatus, exception: TransactionStatus? = null) = Transaction(
        id = Uuid.random().toString(),
        transactionRef = "RMS-TEST-0001",
        title = "Test",
        supplierName = "Test Supplier",
        currentPhase = TransactionPhase.PHASE_1_TENDER,
        currentStatus = status,
        exceptionState = exception,
        createdAt = 0L,
        createdByUserId = Uuid.random().toString(),
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

    @Test fun `TENDER_PREPARATION to ARRIVED_AT_AIRPORT succeeds`() =
        assertSuccess(machine.advance(tx(TENDER_PREPARATION), ARRIVED_AT_AIRPORT))

    @Test fun `ARRIVED_AT_AIRPORT to CLEARANCE_ISSUED succeeds`() =
        assertSuccess(machine.advance(tx(ARRIVED_AT_AIRPORT), CLEARANCE_ISSUED))

    @Test fun `CLEARANCE_ISSUED to TRANSFERRED_TO_WAREHOUSE succeeds`() =
        assertSuccess(machine.advance(tx(CLEARANCE_ISSUED), TRANSFERRED_TO_WAREHOUSE))

    // ── Invalid skip transitions ───────────────────────────────────────────

    @Test fun `DRAFT cannot skip to ARRIVED_AT_AIRPORT`() =
        assertFailure(machine.advance(tx(DRAFT), ARRIVED_AT_AIRPORT))

    @Test fun `TENDER_PREPARATION cannot skip to CLEARANCE_ISSUED`() =
        assertFailure(machine.advance(tx(TENDER_PREPARATION), CLEARANCE_ISSUED))

    @Test fun `ARRIVED_AT_AIRPORT cannot skip to TRANSFERRED_TO_WAREHOUSE`() =
        assertFailure(machine.advance(tx(ARRIVED_AT_AIRPORT), TRANSFERRED_TO_WAREHOUSE))

    @Test fun `TENDER_PREPARATION cannot skip to TRANSFERRED_TO_WAREHOUSE`() =
        assertFailure(machine.advance(tx(TENDER_PREPARATION), TRANSFERRED_TO_WAREHOUSE))

    // ── Invalid reverse transitions ────────────────────────────────────────

    @Test fun `ARRIVED_AT_AIRPORT cannot go back to TENDER_PREPARATION`() =
        assertFailure(machine.advance(tx(ARRIVED_AT_AIRPORT), TENDER_PREPARATION))

    @Test fun `CLEARANCE_ISSUED cannot go back to ARRIVED_AT_AIRPORT`() =
        assertFailure(machine.advance(tx(CLEARANCE_ISSUED), ARRIVED_AT_AIRPORT))

    // ── Terminal state ─────────────────────────────────────────────────────

    @Test fun `TRANSFERRED_TO_WAREHOUSE rejects any further transition`() {
        assertFailure(machine.advance(tx(TRANSFERRED_TO_WAREHOUSE), CLEARANCE_ISSUED))
        assertFailure(machine.advance(tx(TRANSFERRED_TO_WAREHOUSE), TENDER_PREPARATION))
        assertFailure(machine.advance(tx(TRANSFERRED_TO_WAREHOUSE), BLOCKED))
    }

    // ── canAdvance helper ──────────────────────────────────────────────────

    @Test fun `canAdvance returns true for valid transition`() =
        assertTrue(machine.canAdvance(DRAFT, TENDER_PREPARATION))

    @Test fun `canAdvance returns false for invalid transition`() =
        assertTrue(!machine.canAdvance(DRAFT, TRANSFERRED_TO_WAREHOUSE))

    // ── Result content ─────────────────────────────────────────────────────

    @Test fun `Success carries the new status`() {
        val result = machine.advance(tx(DRAFT), TENDER_PREPARATION) as TransitionResult.Success
        assertEquals(TENDER_PREPARATION, result.newStatus)
    }
}
