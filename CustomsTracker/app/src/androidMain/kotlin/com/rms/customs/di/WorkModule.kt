package com.rms.customs.di

import com.rms.customs.work.SyncWorker
import org.koin.androidx.workmanager.dsl.worker
import org.koin.dsl.module

val workModule = module {
    worker { params -> SyncWorker(params.get(), params.get(), get()) }
}
