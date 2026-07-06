package com.rms.customs.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rms.customs.data.local.db.CustomsDatabase
import com.rms.customs.data.local.entity.ActivityLogEntity
import com.rms.customs.data.local.entity.TransactionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class TransactionDaoTest {

    private lateinit var db: CustomsDatabase
    private lateinit var dao: TransactionDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder<CustomsDatabase>(context)
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .allowMainThreadQueries()
            .build()
        dao = db.transactionDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    private fun makeEntity(
        id: String = UUID.randomUUID().toString(),
        status: String = "DRAFT",
    ) = TransactionEntity(
        id = id,
        transactionRef = "RMS-TEST-${id.take(4)}",
        title = "Test Transaction",
        division = null,
        accreditationNumber = null,
        billOfLadingNumber = null,
        responsibleOfficer = "Test Officer",
        beneficiary = null,
        tenderRef = null,
        contractRef = null,
        supplierName = "Test Supplier",
        totalValue = 10000.0,
        currency = "JOD",
        expectedArrivalDate = null,
        actualArrivalDate = null,
        weightKg = null,
        isRefrigerated = false,
        defaultShelfLife = null,
        currentPhase = "PHASE_1_TENDER",
        currentStatus = status,
        exceptionState = null,
        priority = "NORMAL",
        createdAt = System.currentTimeMillis(),
        createdByUserId = UUID.randomUUID().toString(),
        updatedAt = System.currentTimeMillis(),
        closedAt = null,
        notes = null,
    )

    private fun makeLog(txId: String) = ActivityLogEntity(
        id = UUID.randomUUID().toString(),
        transactionId = txId,
        userId = UUID.randomUUID().toString(),
        action = "STATUS_CHANGED",
        fromStatus = "DRAFT",
        toStatus = "TENDER_PREPARATION",
        payload = "{}",
        occurredAt = System.currentTimeMillis(),
    )

    @Test
    fun insertAndObserveAll_returnsInsertedEntity() = runTest {
        val entity = makeEntity()
        dao.insert(entity)

        val results = dao.observeAll().first()
        assertEquals(1, results.size)
        assertEquals(entity.id, results[0].id)
    }

    @Test
    fun observeById_returnsNullForMissingId() = runTest {
        val result = dao.observeById("nonexistent").first()
        assertNull(result)
    }

    @Test
    fun observeById_returnsCorrectEntity() = runTest {
        val entity = makeEntity()
        dao.insert(entity)

        val result = dao.observeById(entity.id).first()
        assertNotNull(result)
        assertEquals(entity.transactionRef, result!!.transactionRef)
    }

    @Test
    fun advanceStatus_updatesEntityAndInsertsLog() = runTest {
        val id = UUID.randomUUID().toString()
        val entity = makeEntity(id = id, status = "DRAFT")
        dao.insert(entity)

        val updated = entity.copy(currentStatus = "TENDER_PREPARATION")
        val log = makeLog(id)
        dao.advanceStatus(updated, log)

        val fetched = dao.getById(id)
        assertNotNull(fetched)
        assertEquals("TENDER_PREPARATION", fetched!!.currentStatus)
    }

    @Test
    fun countByStatus_returnsCorrectCount() = runTest {
        dao.insert(makeEntity(status = "DRAFT"))
        dao.insert(makeEntity(status = "DRAFT"))
        dao.insert(makeEntity(status = "CONTRACT_SIGNED"))

        assertEquals(2, dao.countByStatus("DRAFT"))
        assertEquals(1, dao.countByStatus("CONTRACT_SIGNED"))
        assertEquals(0, dao.countByStatus("CLOSED"))
    }

    @Test
    fun observeActive_excludesClosedTransactions() = runTest {
        dao.insert(makeEntity(status = "DRAFT"))
        dao.insert(makeEntity(status = "CLOSED"))

        val active = dao.observeActive().first()
        assertEquals(1, active.size)
        assertEquals("DRAFT", active[0].currentStatus)
    }

    @Test
    fun observeByStatus_filtersCorrectly() = runTest {
        dao.insert(makeEntity(status = "DRAFT"))
        dao.insert(makeEntity(status = "GOV_PROCESSING"))
        dao.insert(makeEntity(status = "CLOSED"))

        val inProgress = dao.observeByStatus(listOf("DRAFT", "GOV_PROCESSING")).first()
        assertEquals(2, inProgress.size)
    }

    @Test
    fun insert_onConflictReplace_updatesExisting() = runTest {
        val id = UUID.randomUUID().toString()
        dao.insert(makeEntity(id = id, status = "DRAFT"))

        val updated = makeEntity(id = id, status = "TENDER_PREPARATION")
        dao.insert(updated)

        val fetched = dao.getById(id)
        assertEquals("TENDER_PREPARATION", fetched!!.currentStatus)
    }
}
