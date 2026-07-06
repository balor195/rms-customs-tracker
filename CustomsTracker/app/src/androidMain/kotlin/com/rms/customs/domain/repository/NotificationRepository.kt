package com.rms.customs.domain.repository

import com.rms.customs.domain.model.AppNotification
import com.rms.customs.domain.model.NotificationType
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    fun observeUnread(): Flow<List<AppNotification>>
    fun observeAll(): Flow<List<AppNotification>>
    fun observeUnreadCount(): Flow<Int>
    suspend fun create(notification: AppNotification)
    suspend fun markRead(id: String)
    suspend fun markAllRead()
    suspend fun countUnread(): Int
    suspend fun countRecentForTx(transactionId: String, type: NotificationType, since: Long): Int
}
