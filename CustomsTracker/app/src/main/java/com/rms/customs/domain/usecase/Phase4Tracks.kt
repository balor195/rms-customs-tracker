package com.rms.customs.domain.usecase

import com.rms.customs.domain.model.PhaseRecord
import com.rms.customs.domain.model.enums.AssignedEntity
import com.rms.customs.domain.model.enums.PhaseStatus
import java.util.UUID

object Phase4Tracks {

    // Returns Phase 4 PhaseRecord stubs when a transaction enters GOV_PROCESSING.
    // Tracks 4.1–4.3 are mandatory (always IN_PROGRESS).
    // Track 4.4 (Ministry of Health) is optional — starts SKIPPED; user activates it if required.
    fun createFor(transactionId: UUID, slaBySubPhase: Map<String, Int>, now: Long): List<PhaseRecord> =
        listOf(
            PhaseRecord(
                id = UUID.randomUUID(),
                transactionId = transactionId,
                phaseNumber = 4,
                subPhase = "4.1",
                status = PhaseStatus.IN_PROGRESS,
                assignedToEntity = AssignedEntity.MILITARY_COMMAND,
                startedAt = now,
                slaTargetDays = slaBySubPhase["4.1"] ?: 15,
            ),
            PhaseRecord(
                id = UUID.randomUUID(),
                transactionId = transactionId,
                phaseNumber = 4,
                subPhase = "4.2",
                status = PhaseStatus.IN_PROGRESS,
                assignedToEntity = AssignedEntity.CUSTOMS,
                startedAt = now,
                slaTargetDays = slaBySubPhase["4.2"] ?: 10,
            ),
            PhaseRecord(
                id = UUID.randomUUID(),
                transactionId = transactionId,
                phaseNumber = 4,
                subPhase = "4.3",
                status = PhaseStatus.IN_PROGRESS,
                assignedToEntity = AssignedEntity.JFDA,
                startedAt = now,
                slaTargetDays = slaBySubPhase["4.3"] ?: 12,
            ),
            // Optional — only applicable for some medical devices and special pharmaceuticals
            PhaseRecord(
                id = UUID.randomUUID(),
                transactionId = transactionId,
                phaseNumber = 4,
                subPhase = "4.4",
                status = PhaseStatus.SKIPPED,
                assignedToEntity = AssignedEntity.HEALTH_MINISTRY,
                startedAt = null,
                slaTargetDays = slaBySubPhase["4.4"] ?: 7,
            ),
        )
}
