package com.rms.customs.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.rms.customs.data.local.dao.ActivityLogDao
import com.rms.customs.data.local.dao.DocumentDao
import com.rms.customs.data.local.dao.NotificationDao
import com.rms.customs.data.local.dao.PhaseRecordDao
import com.rms.customs.data.local.dao.SlaConfigDao
import com.rms.customs.data.local.dao.TransactionDao
import com.rms.customs.data.local.dao.UserDao
import com.rms.customs.data.local.db.CustomsDatabase
import com.rms.customs.data.local.entity.toEntity
import com.rms.customs.domain.usecase.SlaConfigDefaults
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): CustomsDatabase {
        var db: CustomsDatabase? = null
        val callback = object : RoomDatabase.Callback() {
            override fun onCreate(sqLiteDb: SupportSQLiteDatabase) {
                // Seed default SLA configs on first database creation
                CoroutineScope(Dispatchers.IO).launch {
                    val dao = db?.slaConfigDao() ?: return@launch
                    SlaConfigDefaults.all.forEach { config ->
                        dao.upsert(config.toEntity())
                    }
                }
            }
        }
        return Room.databaseBuilder(context, CustomsDatabase::class.java, "customs_tracker.db")
            .addMigrations(CustomsDatabase.MIGRATION_1_2)
            .addCallback(callback)
            .build()
            .also { db = it }
    }

    @Provides fun provideTransactionDao(db: CustomsDatabase): TransactionDao = db.transactionDao()
    @Provides fun providePhaseRecordDao(db: CustomsDatabase): PhaseRecordDao = db.phaseRecordDao()
    @Provides fun provideDocumentDao(db: CustomsDatabase): DocumentDao = db.documentDao()
    @Provides fun provideActivityLogDao(db: CustomsDatabase): ActivityLogDao = db.activityLogDao()
    @Provides fun provideSlaConfigDao(db: CustomsDatabase): SlaConfigDao = db.slaConfigDao()
    @Provides fun provideUserDao(db: CustomsDatabase): UserDao = db.userDao()
    @Provides fun provideNotificationDao(db: CustomsDatabase): NotificationDao = db.notificationDao()
}
