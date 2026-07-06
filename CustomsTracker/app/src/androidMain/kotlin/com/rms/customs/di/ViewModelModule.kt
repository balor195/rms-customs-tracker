package com.rms.customs.di

import com.rms.customs.presentation.viewmodel.AuthViewModel
import com.rms.customs.presentation.viewmodel.CreateTransactionViewModel
import com.rms.customs.presentation.viewmodel.DashboardViewModel
import com.rms.customs.presentation.viewmodel.DocumentViewModel
import com.rms.customs.presentation.viewmodel.NotificationViewModel
import com.rms.customs.presentation.viewmodel.ReportViewModel
import com.rms.customs.presentation.viewmodel.SettingsViewModel
import com.rms.customs.presentation.viewmodel.SlaAdminViewModel
import com.rms.customs.presentation.viewmodel.SyncViewModel
import com.rms.customs.presentation.viewmodel.TransactionDetailViewModel
import com.rms.customs.presentation.viewmodel.TransactionListViewModel
import com.rms.customs.presentation.viewmodel.UserManagementViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel { AuthViewModel(get(), get(), get(), get()) }
    viewModel { CreateTransactionViewModel(get()) }
    viewModel { DashboardViewModel(get()) }
    viewModel { DocumentViewModel(get(), androidContext(), get()) }
    viewModel { NotificationViewModel(get()) }
    viewModel { ReportViewModel(androidContext(), get(), get(), get()) }
    viewModel { SettingsViewModel(get(), get()) }
    viewModel { SlaAdminViewModel(get()) }
    viewModel { SyncViewModel(get()) }
    viewModel { TransactionDetailViewModel(get(), get(), get()) }
    viewModel { TransactionListViewModel(get()) }
    viewModel { UserManagementViewModel(get()) }
}
