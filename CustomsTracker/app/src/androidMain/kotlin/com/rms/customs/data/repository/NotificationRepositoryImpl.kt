package com.rms.customs.data.repository

import com.rms.customs.data.local.dao.NotificationDao
import com.rms.customs.data.local.entity.toDomain
import com.rms.customs.data.local.entity.toEntity
import com.rms.customs.domain.model.AppNotification
import com.rms.customs.domain.model.enums.NotificationType
import com.rms.customs.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class NotificationRepositoryImpl(
    private val notificationDao: NotificationDao,
) : NotificationRepository {

    override fun observeUnread(): Flow<List<AppNotification>> =
        notificationDao.observeUnread().map { it.map { e -> e.toDomain() } }

    override fun observeAll(): Flow<List<AppNotification>> =
        notificationDao.observeAll().map { it.map { e -> e.toDomain() } }

    override suspend fun create(notification: AppNotification) {
        notificationDao.insert(notification.toEntity())
    }

    override suspend fun markRead(id: String) {
        notificationDao.markRead(id)
    }

    override suspend fun markAllRead() {
        notificationDao.markAllRead()
    }

    override fun observeUnreadCount(): Flow<Int> =
        notificationDao.observeUnreadCount()

    override suspend fun countUnread(): Int =
        notificationDao.countUnread()

    override suspend fun countRecentForTx(transactionId: String, type: NotificationType, since: Long): Int =
        notificationDao.countRecentForTx(transactionId, type.name, since)
}
