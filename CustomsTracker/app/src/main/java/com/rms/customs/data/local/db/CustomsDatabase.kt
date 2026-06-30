package com.rms.customs.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.rms.customs.data.local.dao.ActivityLogDao
import com.rms.customs.data.local.dao.DocumentDao
import com.rms.customs.data.local.dao.NotificationDao
import com.rms.customs.data.local.dao.PhaseRecordDao
import com.rms.customs.data.local.dao.SlaConfigDao
import com.rms.customs.data.local.dao.TransactionDao
import com.rms.customs.data.local.dao.UserDao
import com.rms.customs.data.local.entity.ActivityLogEntity
import com.rms.customs.data.local.entity.NotificationEntity
import com.rms.customs.data.local.entity.PhaseRecordEntity
import com.rms.customs.data.local.entity.SlaConfigEntity
import com.rms.customs.data.local.entity.TransactionDocumentEntity
import com.rms.customs.data.local.entity.TransactionEntity
import com.rms.customs.data.local.entity.UserEntity

@Database(
    entities = [
        TransactionEntity::class,
        PhaseRecordEntity::class,
        TransactionDocumentEntity::class,
        ActivityLogEntity::class,
        SlaConfigEntity::class,
        UserEntity::class,
        NotificationEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class CustomsDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun phaseRecordDao(): PhaseRecordDao
    abstract fun documentDao(): DocumentDao
    abstract fun activityLogDao(): ActivityLogDao
    abstract fun slaConfigDao(): SlaConfigDao
    abstract fun userDao(): UserDao
    abstract fun notificationDao(): NotificationDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Fix users whose department was stored with old enum values
                // (TENDERS, MANAGEMENT are removed; PHARMACY stays, new ones are MEDICAL_CONSUMABLES / MEDICAL_DEVICES)
                db.execSQL("UPDATE users SET department = 'PHARMACY' WHERE department NOT IN ('PHARMACY', 'MEDICAL_CONSUMABLES', 'MEDICAL_DEVICES')")

                // Add new RMS-specific columns to transactions
                db.execSQL("ALTER TABLE transactions ADD COLUMN division TEXT")
                db.execSQL("ALTER TABLE transactions ADD COLUMN accreditationNumber TEXT")
                db.execSQL("ALTER TABLE transactions ADD COLUMN billOfLadingNumber TEXT")
                db.execSQL("ALTER TABLE transactions ADD COLUMN responsibleOfficer TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE transactions ADD COLUMN beneficiary TEXT")
                db.execSQL("ALTER TABLE transactions ADD COLUMN expectedArrivalDate INTEGER")
                db.execSQL("ALTER TABLE transactions ADD COLUMN actualArrivalDate INTEGER")
                db.execSQL("ALTER TABLE transactions ADD COLUMN shipmentStatus TEXT NOT NULL DEFAULT 'EXPECTED'")
                // Create index declared on the entity — must match or Room schema validation crashes
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_accreditationNumber` ON `transactions` (`accreditationNumber`)")
            }
        }
    }
}
