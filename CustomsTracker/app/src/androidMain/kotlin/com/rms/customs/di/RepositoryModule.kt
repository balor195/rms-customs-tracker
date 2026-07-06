package com.rms.customs.di

import com.rms.customs.data.repository.DocumentRepositoryImpl
import com.rms.customs.data.repository.NotificationRepositoryImpl
import com.rms.customs.data.repository.SlaRepositoryImpl
import com.rms.customs.data.repository.SyncRepositoryImpl
import com.rms.customs.data.repository.TransactionRepositoryImpl
import com.rms.customs.data.repository.UserRepositoryImpl
import com.rms.customs.domain.repository.DocumentRepository
import com.rms.customs.domain.repository.NotificationRepository
import com.rms.customs.domain.repository.SlaRepository
import com.rms.customs.domain.repository.SyncRepository
import com.rms.customs.domain.repository.TransactionRepository
import com.rms.customs.domain.repository.UserRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val repositoryModule = module {

    single<TransactionRepository> {
        TransactionRepositoryImpl(get(), get(), get(), get(), get())
    }
    single<UserRepository> { UserRepositoryImpl(get()) }
    single<DocumentRepository> { DocumentRepositoryImpl(get(), get()) }
    single<SlaRepository> { SlaRepositoryImpl(get()) }
    single<NotificationRepository> { NotificationRepositoryImpl(get()) }
    single<SyncRepository> { SyncRepositoryImpl(androidContext(), get(), get()) }
}
