package com.rms.customs.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.rms.customs.domain.model.SlaConfig

@Entity(
    tableName = "sla_configs",
    indices = [Index(value = ["phaseNumber", "subPhase"], unique = true)]
)
data class SlaConfigEntity(
    @PrimaryKey val id: String,
    val phaseNumber: Int,
    val subPhase: String,
    val targetDays: Int,
    val escalationAfterDays: Int,
    val isActive: Boolean,
)

fun SlaConfigEntity.toDomain() = SlaConfig(
    id = id,
    phaseNumber = phaseNumber,
    subPhase = subPhase,
    targetDays = targetDays,
    escalationAfterDays = escalationAfterDays,
    isActive = isActive,
)

fun SlaConfig.toEntity() = SlaConfigEntity(
    id = id,
    phaseNumber = phaseNumber,
    subPhase = subPhase,
    targetDays = targetDays,
    escalationAfterDays = escalationAfterDays,
    isActive = isActive,
)
