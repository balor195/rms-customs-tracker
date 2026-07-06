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
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindTransactionRepository(impl: TransactionRepositoryImpl): TransactionRepository

    @Binds @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    @Binds @Singleton
    abstract fun bindDocumentRepository(impl: DocumentRepositoryImpl): DocumentRepository

    @Binds @Singleton
    abstract fun bindSlaRepository(impl: SlaRepositoryImpl): SlaRepository

    @Binds @Singleton
    abstract fun bindNotificationRepository(impl: NotificationRepositoryImpl): NotificationRepository

    @Binds @Singleton
    abstract fun bindSyncRepository(impl: SyncRepositoryImpl): SyncRepository
}
