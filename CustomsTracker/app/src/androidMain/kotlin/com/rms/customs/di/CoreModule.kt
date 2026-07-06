package com.rms.customs.di

import com.rms.customs.data.export.CsvExporter
import com.rms.customs.data.export.PdfExporter
import com.rms.customs.data.local.SessionStore
import com.rms.customs.domain.statemachine.TransactionStateMachine
import com.rms.customs.domain.usecase.LoginUseCase
import com.rms.customs.domain.usecase.SetupAdminUseCase
import com.rms.customs.notifications.CustomsNotificationManager
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val coreModule = module {
    single { SessionStore(androidContext()) }
    single { CustomsNotificationManager(androidContext()) }
    single { CsvExporter(androidContext()) }
    single { PdfExporter(androidContext()) }
    single { TransactionStateMachine() }
    single { LoginUseCase(get()) }
    single { SetupAdminUseCase(get()) }
}
