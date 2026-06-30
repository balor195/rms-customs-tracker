package com.rms.customs.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.rms.customs.domain.model.PhaseRecord
import com.rms.customs.domain.model.enums.AssignedEntity
import com.rms.customs.domain.model.enums.PhaseStatus
import java.util.UUID

@Entity(
    tableName = "phase_records",
    foreignKeys = [
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transactionId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index(value = ["transactionId"]), Index(value = ["phaseNumber"])]
)
data class PhaseRecordEntity(
    @PrimaryKey val id: String,
    val transactionId: String,
    val phaseNumber: Int,
    val subPhase: String,
    val status: String,               // PhaseStatus.name
    val assignedToEntity: String,     // AssignedEntity.name
    val startedAt: Long?,
    val completedAt: Long?,
    val slaTargetDays: Int?,
    val blockerReason: String?,
    val completedByUserId: String?,
    val notes: String?,
)

fun PhaseRecordEntity.toDomain() = PhaseRecord(
    id = UUID.fromString(id),
    transactionId = UUID.fromString(transactionId),
    phaseNumber = phaseNumber,
    subPhase = subPhase,
    status = PhaseStatus.valueOf(status),
    assignedToEntity = AssignedEntity.valueOf(assignedToEntity),
    startedAt = startedAt,
    completedAt = completedAt,
    slaTargetDays = slaTargetDays,
    blockerReason = blockerReason,
    completedByUserId = completedByUserId?.let { UUID.fromString(it) },
    notes = notes,
)

fun PhaseRecord.toEntity() = PhaseRecordEntity(
    id = id.toString(),
    transactionId = transactionId.toString(),
    phaseNumber = phaseNumber,
    subPhase = subPhase,
    status = status.name,
    assignedToEntity = assignedToEntity.name,
    startedAt = startedAt,
    completedAt = completedAt,
    slaTargetDays = slaTargetDays,
    blockerReason = blockerReason,
    completedByUserId = completedByUserId?.toString(),
    notes = notes,
)
