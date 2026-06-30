package com.rms.customs.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.rms.customs.domain.model.AppNotification
import com.rms.customs.domain.model.NotificationType
import java.util.UUID

@Entity(
    tableName = "notifications",
    foreignKeys = [
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transactionId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index(value = ["transactionId"]), Index(value = ["isRead"]), Index(value = ["createdAt"])]
)
data class NotificationEntity(
    @PrimaryKey val id: String,
    val transactionId: String,
    val type: String,                 // NotificationType.name
    val titleAr: String,
    val titleEn: String,
    val messageAr: String,
    val messageEn: String,
    val isRead: Boolean,
    val createdAt: Long,
)

fun NotificationEntity.toDomain() = AppNotification(
    id = UUID.fromString(id),
    transactionId = UUID.fromString(transactionId),
    type = NotificationType.valueOf(type),
    titleAr = titleAr,
    titleEn = titleEn,
    messageAr = messageAr,
    messageEn = messageEn,
    isRead = isRead,
    createdAt = createdAt,
)

fun AppNotification.toEntity() = NotificationEntity(
    id = id.toString(),
    transactionId = transactionId.toString(),
    type = type.name,
    titleAr = titleAr,
    titleEn = titleEn,
    messageAr = messageAr,
    messageEn = messageEn,
    isRead = isRead,
    createdAt = createdAt,
)
