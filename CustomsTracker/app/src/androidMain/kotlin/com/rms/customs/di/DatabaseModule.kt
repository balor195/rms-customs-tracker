package com.rms.customs.di

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.rms.customs.data.local.db.CustomsDatabase
import com.rms.customs.data.local.entity.toEntity
import com.rms.customs.domain.usecase.SlaConfigDefaults
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModule = module {

    single {
        var db: CustomsDatabase? = null
        val callback = object : RoomDatabase.Callback() {
            override fun onOpen(sqLiteDb: SupportSQLiteDatabase) {
                // Seed default SLA configs whenever the table is empty (fresh install,
                // or right after a migration that cleared stale phase-numbered rows)
                CoroutineScope(Dispatchers.IO).launch {
                    val dao = db?.slaConfigDao() ?: return@launch
                    if (dao.count() == 0) {
                        SlaConfigDefaults.all.forEach { config ->
                            dao.upsert(config.toEntity())
                        }
                    }
                }
            }
        }
        Room.databaseBuilder(androidContext(), CustomsDatabase::class.java, "customs_tracker.db")
            .addMigrations(
                CustomsDatabase.MIGRATION_1_2,
                CustomsDatabase.MIGRATION_2_3,
                CustomsDatabase.MIGRATION_3_4,
                CustomsDatabase.MIGRATION_4_5,
                CustomsDatabase.MIGRATION_5_6,
                CustomsDatabase.MIGRATION_6_7,
            )
            .addCallback(callback)
            .build()
            .also { db = it }
    }

    single { get<CustomsDatabase>().transactionDao() }
    single { get<CustomsDatabase>().documentDao() }
    single { get<CustomsDatabase>().activityLogDao() }
    single { get<CustomsDatabase>().slaConfigDao() }
    single { get<CustomsDatabase>().userDao() }
    single { get<CustomsDatabase>().notificationDao() }
}
