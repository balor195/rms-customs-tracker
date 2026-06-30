package com.rms.customs.domain.model

import com.rms.customs.domain.model.enums.AssignedEntity
import com.rms.customs.domain.model.enums.PhaseStatus
import java.util.UUID

data class PhaseRecord(
    val id: UUID,
    val transactionId: UUID,
    val phaseNumber: Int,              // 1–7
    val subPhase: String,              // e.g. "3.3.1", "4.1" (Military track)
    val status: PhaseStatus,
    val assignedToEntity: AssignedEntity,
    val startedAt: Long? = null,
    val completedAt: Long? = null,
    val slaTargetDays: Int? = null,
    val blockerReason: String? = null,
    val completedByUserId: UUID? = null,
    val notes: String? = null,
) {
    val daysElapsed: Long?
        get() = startedAt?.let { (System.currentTimeMillis() - it) / 86_400_000L }

    val isOverSla: Boolean
        get() = slaTargetDays != null && daysElapsed != null && daysElapsed!! > slaTargetDays!!
}
