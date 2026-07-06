package com.rms.customs

import android.app.Application
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.rms.customs.di.coreModule
import com.rms.customs.di.databaseModule
import com.rms.customs.di.networkModule
import com.rms.customs.di.repositoryModule
import com.rms.customs.di.viewModelModule
import com.rms.customs.di.workModule
import com.rms.customs.notifications.CustomsNotificationManager
import com.rms.customs.work.SyncWorker
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.factory.KoinWorkerFactory
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.context.startKoin
import java.util.concurrent.TimeUnit

class CustomsApp : Application(), Configuration.Provider {

    private val notificationManager: CustomsNotificationManager by inject()

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(KoinWorkerFactory())
            .build()

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@CustomsApp)
            workManagerFactory()
            modules(databaseModule, networkModule, repositoryModule, coreModule, viewModelModule, workModule)
        }
        notificationManager.createChannels()
        scheduleSyncWorker()
    }

    private fun scheduleSyncWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
            .build()
        val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            SyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }
}
