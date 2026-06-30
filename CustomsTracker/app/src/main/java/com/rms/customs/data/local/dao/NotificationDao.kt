package com.rms.customs.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.rms.customs.data.local.entity.NotificationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {

    @Query("SELECT * FROM notifications WHERE isRead = 0 ORDER BY createdAt DESC")
    fun observeUnread(): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<NotificationEntity>>

    @Insert
    suspend fun insert(entity: NotificationEntity)

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markRead(id: String)

    @Query("UPDATE notifications SET isRead = 1")
    suspend fun markAllRead()

    @Query("SELECT COUNT(*) FROM notifications WHERE isRead = 0")
    suspend fun countUnread(): Int

    @Query("SELECT COUNT(*) FROM notifications WHERE isRead = 0")
    fun observeUnreadCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM notifications WHERE transactionId = :txId AND type = :type AND createdAt > :since")
    suspend fun countRecentForTx(txId: String, type: String, since: Long): Int
}
