package com.rms.customs.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rms.customs.domain.model.AppNotification
import com.rms.customs.domain.model.enums.NotificationType
import com.rms.customs.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class NotifFilter { ALL, UNREAD, SLA }

class NotificationViewModel(
    private val notificationRepository: NotificationRepository,
) : ViewModel() {

    private val _filter = MutableStateFlow(NotifFilter.ALL)
    val filter: StateFlow<NotifFilter> = _filter.asStateFlow()

    val unreadCount: StateFlow<Int> = notificationRepository
        .observeUnreadCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private val allNotifications: StateFlow<List<AppNotification>> = notificationRepository
        .observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val filtered: StateFlow<List<AppNotification>> = combine(allNotifications, _filter) { list, f ->
        when (f) {
            NotifFilter.ALL    -> list
            NotifFilter.UNREAD -> list.filter { !it.isRead }
            NotifFilter.SLA    -> list.filter {
                it.type == NotificationType.SLA_BREACH || it.type == NotificationType.SLA_ESCALATED
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setFilter(f: NotifFilter) { _filter.value = f }

    fun markRead(id: String) = viewModelScope.launch { notificationRepository.markRead(id) }

    fun markAllRead() = viewModelScope.launch { notificationRepository.markAllRead() }
}
