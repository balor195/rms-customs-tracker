package com.rms.customs.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rms.customs.data.local.db.CustomsDatabase
import com.rms.customs.data.local.entity.PhaseRecordEntity
import com.rms.customs.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class PhaseRecordDaoTest {

    private lateinit var db: CustomsDatabase
    private lateinit var dao: PhaseRecordDao
    private lateinit var txDao: TransactionDao
    private lateinit var txId: String

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, CustomsDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.phaseRecordDao()
        txDao = db.transactionDao()

        // Insert a parent transaction required by FK
        txId = UUID.randomUUID().toString()
        txDao.insert(
            TransactionEntity(
                id = txId, transactionRef = "RMS-PH-0001", title = "Phase Test",
                tenderRef = null, contractRef = null, supplierName = "Supplier",
                totalValue = null, currency = "JOD",
                currentPhase = "PHASE_4_GOV_AGENCIES",
                currentStatus = "GOV_PROCESSING",
                exceptionState = null, priority = "NORMAL",
                createdAt = 0L, createdByUserId = UUID.randomUUID().toString(),
                updatedAt = 0L, closedAt = null, notes = null,
            )
        )
    }

    @After
    fun teardown() { db.close() }

    private fun phase4Record(subPhase: String, status: String = "IN_PROGRESS") = PhaseRecordEntity(
        id = UUID.randomUUID().toString(),
        transactionId = txId,
        phaseNumber = 4,
        subPhase = subPhase,
        status = status,
        assignedToEntity = when (subPhase) {
            "4.1" -> "MILITARY_COMMAND"
            "4.2" -> "CUSTOMS"
            else  -> "JFDA"
        },
        startedAt = System.currentTimeMillis(),
        completedAt = null,
        slaTargetDays = when (subPhase) { "4.1" -> 15; "4.2" -> 10; else -> 12 },
        blockerReason = null,
        completedByUserId = null,
        notes = null,
    )

    @Test
    fun insertAndObserve_returnsAllPhaseRecordsForTransaction() = runTest {
        dao.insertAll(listOf(phase4Record("4.1"), phase4Record("4.2"), phase4Record("4.3")))

        val records = dao.observeForTransaction(txId).first()
        assertEquals(3, records.size)
    }

    @Test
    fun observeForPhase_filtersCorrectly() = runTest {
        dao.insertAll(listOf(phase4Record("4.1"), phase4Record("4.2"), phase4Record("4.3")))

        val phase4 = dao.observeForPhase(txId, 4).first()
        assertEquals(3, phase4.size)
    }

    @Test
    fun countPhase4Incomplete_returnsCorrectCount() = runTest {
        dao.insertAll(listOf(phase4Record("4.1"), phase4Record("4.2"), phase4Record("4.3")))

        assertEquals(3, dao.countPhase4Incomplete(txId))
    }

    @Test
    fun countPhase4Incomplete_decreasesWhenRecordCompleted() = runTest {
        val military = phase4Record("4.1", status = "IN_PROGRESS")
        val customs  = phase4Record("4.2", status = "IN_PROGRESS")
        val jfda     = phase4Record("4.3", status = "IN_PROGRESS")
        dao.insertAll(listOf(military, customs, jfda))

        dao.markComplete(
            id = military.id,
            status = "DONE",
            completedAt = System.currentTimeMillis(),
            userId = UUID.randomUUID().toString(),
        )

        assertEquals(2, dao.countPhase4Incomplete(txId))
    }

    @Test
    fun countPhase4Incomplete_isZeroWhenAllDone() = runTest {
        val now = System.currentTimeMillis()
        val userId = UUID.randomUUID().toString()
        val records = listOf(phase4Record("4.1"), phase4Record("4.2"), phase4Record("4.3"))
        dao.insertAll(records)

        records.forEach { dao.markComplete(it.id, "DONE", now, userId) }

        assertEquals(0, dao.countPhase4Incomplete(txId))
    }

    @Test
    fun getPhase4Records_returnsOnlyPhase4() = runTest {
        dao.insertAll(listOf(phase4Record("4.1"), phase4Record("4.2"), phase4Record("4.3")))

        val records = dao.getPhase4Records(txId)
        assertEquals(3, records.size)
        records.forEach { assertEquals(4, it.phaseNumber) }
    }
}
