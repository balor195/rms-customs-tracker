package com.rms.customs.data.local.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.rms.customs.data.local.dao.ActivityLogDao
import com.rms.customs.data.local.dao.DocumentDao
import com.rms.customs.data.local.dao.NotificationDao
import com.rms.customs.data.local.dao.SlaConfigDao
import com.rms.customs.data.local.dao.TransactionDao
import com.rms.customs.data.local.dao.UserDao
import com.rms.customs.data.local.entity.ActivityLogEntity
import com.rms.customs.data.local.entity.NotificationEntity
import com.rms.customs.data.local.entity.SlaConfigEntity
import com.rms.customs.data.local.entity.TransactionDocumentEntity
import com.rms.customs.data.local.entity.TransactionEntity
import com.rms.customs.data.local.entity.UserEntity

@Database(
    entities = [
        TransactionEntity::class,
        TransactionDocumentEntity::class,
        ActivityLogEntity::class,
        SlaConfigEntity::class,
        UserEntity::class,
        NotificationEntity::class,
    ],
    version = 7,
    exportSchema = false,
)
@ConstructedBy(CustomsDatabaseConstructor::class)
abstract class CustomsDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun documentDao(): DocumentDao
    abstract fun activityLogDao(): ActivityLogDao
    abstract fun slaConfigDao(): SlaConfigDao
    abstract fun userDao(): UserDao
    abstract fun notificationDao(): NotificationDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(connection: SQLiteConnection) {
                // Fix users whose department was stored with old enum values
                // (TENDERS, MANAGEMENT are removed; PHARMACY stays, new ones are MEDICAL_CONSUMABLES / MEDICAL_DEVICES)
                connection.execSQL("UPDATE users SET department = 'PHARMACY' WHERE department NOT IN ('PHARMACY', 'MEDICAL_CONSUMABLES', 'MEDICAL_DEVICES')")

                // Add new RMS-specific columns to transactions
                connection.execSQL("ALTER TABLE transactions ADD COLUMN division TEXT")
                connection.execSQL("ALTER TABLE transactions ADD COLUMN accreditationNumber TEXT")
                connection.execSQL("ALTER TABLE transactions ADD COLUMN billOfLadingNumber TEXT")
                connection.execSQL("ALTER TABLE transactions ADD COLUMN responsibleOfficer TEXT NOT NULL DEFAULT ''")
                connection.execSQL("ALTER TABLE transactions ADD COLUMN beneficiary TEXT")
                connection.execSQL("ALTER TABLE transactions ADD COLUMN expectedArrivalDate INTEGER")
                connection.execSQL("ALTER TABLE transactions ADD COLUMN actualArrivalDate INTEGER")
                connection.execSQL("ALTER TABLE transactions ADD COLUMN shipmentStatus TEXT NOT NULL DEFAULT 'EXPECTED'")
                // Create index declared on the entity — must match or Room schema validation crashes
                connection.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_accreditationNumber` ON `transactions` (`accreditationNumber`)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(connection: SQLiteConnection) {
                // Phase 4 (gov-agency parallel tracks) removed — its supporting table goes with it.
                connection.execSQL("DROP TABLE IF EXISTS phase_records")

                // Statuses/phase numbers were renumbered when phases 2-4 were dropped and a
                // new final phase was added — clear seeded SLA config so it reseeds cleanly.
                connection.execSQL("DELETE FROM sla_configs")

                // New tender-intake fields
                connection.execSQL("ALTER TABLE transactions ADD COLUMN weightKg REAL")
                connection.execSQL("ALTER TABLE transactions ADD COLUMN isRefrigerated INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(connection: SQLiteConnection) {
                // العمر الافتراضي — free-text shelf life, entered only for المستهلكات transactions
                connection.execSQL("ALTER TABLE transactions ADD COLUMN defaultShelfLife TEXT")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(connection: SQLiteConnection) {
                // department becomes nullable — ADMIN/CLEARANCE/WAREHOUSE no longer belong to a division.
                // SQLite can't drop a NOT NULL constraint via ALTER TABLE, so the table is recreated.
                connection.execSQL(
                    """
                    CREATE TABLE users_new (
                        id TEXT NOT NULL PRIMARY KEY,
                        username TEXT NOT NULL,
                        displayName TEXT NOT NULL,
                        displayNameAr TEXT NOT NULL,
                        role TEXT NOT NULL,
                        department TEXT,
                        passwordHash TEXT NOT NULL,
                        isActive INTEGER NOT NULL,
                        lastLoginAt INTEGER
                    )
                    """.trimIndent()
                )
                connection.execSQL(
                    """
                    INSERT INTO users_new
                    SELECT id, username, displayName, displayNameAr, role, department, passwordHash, isActive, lastLoginAt
                    FROM users
                    """.trimIndent()
                )
                connection.execSQL("DROP TABLE users")
                connection.execSQL("ALTER TABLE users_new RENAME TO users")
                connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_users_username` ON `users` (`username`)")

                // Roles that see every division no longer carry a fixed one.
                connection.execSQL("UPDATE users SET department = NULL WHERE role IN ('ADMIN', 'CLEARANCE', 'WAREHOUSE')")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(connection: SQLiteConnection) {
                // Phase 3 (Transit & Receipt) merged into Phase 4 (Warehouse Transfer Confirmation) —
                // they described the same real-world event. Remap any transactions sitting in the
                // removed transit sub-statuses forward to financial settlement (the next remaining phase).
                connection.execSQL(
                    "UPDATE transactions SET currentStatus = 'FINANCIAL_SETTLEMENT_PENDING' " +
                    "WHERE currentStatus IN ('IN_TRANSIT', 'RECEIVED_AT_WAREHOUSE', 'INSPECTION_COMPLETE')"
                )
                // Old phase enum constants renamed/renumbered.
                connection.execSQL("UPDATE transactions SET currentPhase = 'PHASE_3_FINANCIAL' WHERE currentPhase IN ('PHASE_3_TRANSIT', 'PHASE_4_FINANCIAL')")
                connection.execSQL("UPDATE transactions SET currentPhase = 'PHASE_4_WAREHOUSE_CONFIRMATION' WHERE currentPhase = 'PHASE_5_WAREHOUSE_CONFIRMATION'")

                // SLA sub-phase numbering shifted (old phase 3 removed, old phase 4 is now phase 3) — reseed cleanly.
                connection.execSQL("DELETE FROM sla_configs")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(connection: SQLiteConnection) {
                // Workflow simplified again: "نشر المناقصة" and the whole "التسوية المالية" phase
                // removed; a new "وصلت الشحنة للمطار" step inserted between tender prep and clearance.
                // Remap any existing rows in now-removed statuses forward.
                connection.execSQL("UPDATE transactions SET currentStatus = 'TENDER_PREPARATION' WHERE currentStatus = 'TENDER_PUBLISHED'")
                connection.execSQL("UPDATE transactions SET currentStatus = 'CLEARANCE_ISSUED' WHERE currentStatus IN ('FINANCIAL_SETTLEMENT_PENDING', 'CLOSED')")

                // Old phase enum constants renamed/renumbered (clearance shifted from phase 2 to phase 3;
                // the removed financial phase's transactions now sit at the clearance phase too).
                connection.execSQL("UPDATE transactions SET currentPhase = 'PHASE_3_CLEARANCE' WHERE currentPhase IN ('PHASE_2_CLEARANCE', 'PHASE_3_FINANCIAL')")

                // Drop the now-redundant shipmentStatus column (SQLite can't drop a column via
                // ALTER TABLE on this SQLite version, so the table is recreated).
                connection.execSQL(
                    """
                    CREATE TABLE transactions_new (
                        id TEXT NOT NULL PRIMARY KEY,
                        transactionRef TEXT NOT NULL,
                        title TEXT NOT NULL,
                        division TEXT,
                        accreditationNumber TEXT,
                        billOfLadingNumber TEXT,
                        responsibleOfficer TEXT NOT NULL,
                        beneficiary TEXT,
                        tenderRef TEXT,
                        contractRef TEXT,
                        supplierName TEXT NOT NULL,
                        totalValue REAL,
                        currency TEXT NOT NULL,
                        expectedArrivalDate INTEGER,
                        actualArrivalDate INTEGER,
                        weightKg REAL,
                        isRefrigerated INTEGER NOT NULL,
                        defaultShelfLife TEXT,
                        currentPhase TEXT NOT NULL,
                        currentStatus TEXT NOT NULL,
                        exceptionState TEXT,
                        priority TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        createdByUserId TEXT NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        closedAt INTEGER,
                        notes TEXT
                    )
                    """.trimIndent()
                )
                connection.execSQL(
                    """
                    INSERT INTO transactions_new (
                        id, transactionRef, title, division, accreditationNumber, billOfLadingNumber,
                        responsibleOfficer, beneficiary, tenderRef, contractRef, supplierName, totalValue,
                        currency, expectedArrivalDate, actualArrivalDate, weightKg, isRefrigerated,
                        defaultShelfLife, currentPhase, currentStatus, exceptionState, priority,
                        createdAt, createdByUserId, updatedAt, closedAt, notes
                    )
                    SELECT
                        id, transactionRef, title, division, accreditationNumber, billOfLadingNumber,
                        responsibleOfficer, beneficiary, tenderRef, contractRef, supplierName, totalValue,
                        currency, expectedArrivalDate, actualArrivalDate, weightKg, isRefrigerated,
                        defaultShelfLife, currentPhase, currentStatus, exceptionState, priority,
                        createdAt, createdByUserId, updatedAt, closedAt, notes
                    FROM transactions
                    """.trimIndent()
                )
                connection.execSQL("DROP TABLE transactions")
                connection.execSQL("ALTER TABLE transactions_new RENAME TO transactions")
                connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_transactions_transactionRef` ON `transactions` (`transactionRef`)")
                connection.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_currentStatus` ON `transactions` (`currentStatus`)")
                connection.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_updatedAt` ON `transactions` (`updatedAt`)")
                connection.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_accreditationNumber` ON `transactions` (`accreditationNumber`)")

                // SLA sub-phase numbering shifted again — reseed cleanly.
                connection.execSQL("DELETE FROM sla_configs")
            }
        }
    }
}

@Suppress("KotlinNoActualForExpect")
expect object CustomsDatabaseConstructor : RoomDatabaseConstructor<CustomsDatabase> {
    override fun initialize(): CustomsDatabase
}
