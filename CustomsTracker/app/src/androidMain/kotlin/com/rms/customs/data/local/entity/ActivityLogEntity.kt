package com.rms.customs.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.rms.customs.domain.model.ActivityLog
import com.rms.customs.domain.model.enums.LogAction
import java.util.UUID

@Entity(
    tableName = "activity_logs",
    foreignKeys = [
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transactionId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index(value = ["transactionId"]), Index(value = ["occurredAt"])]
)
data class ActivityLogEntity(
    @PrimaryKey val id: String,
    val transactionId: String,
    val userId: String,
    val action: String,               // LogAction.name
    val fromStatus: String?,
    val toStatus: String?,
    val payload: String,
    val occurredAt: Long,
)

fun ActivityLogEntity.toDomain() = ActivityLog(
    id = UUID.fromString(id),
    transactionId = UUID.fromString(transactionId),
    userId = UUID.fromString(userId),
    action = LogAction.valueOf(action),
    fromStatus = fromStatus,
    toStatus = toStatus,
    payload = payload,
    occurredAt = occurredAt,
)

fun ActivityLog.toEntity() = ActivityLogEntity(
    id = id.toString(),
    transactionId = transactionId.toString(),
    userId = userId.toString(),
    action = action.name,
    fromStatus = fromStatus,
    toStatus = toStatus,
    payload = payload,
    occurredAt = occurredAt,
)
