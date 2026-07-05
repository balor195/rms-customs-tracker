package com.rms.customs.domain.usecase

import com.rms.customs.domain.model.SlaConfig
import java.util.UUID

object SlaConfigDefaults {

    // Default SLA targets from RMS Jordan customs clearance workflow.
    // All values are in calendar days. Admins can override via Phase 10 settings.
    val all: List<SlaConfig> = listOf(
        // Phase 1 — Tender Preparation
        SlaConfig(UUID.randomUUID(), phaseNumber = 1, subPhase = "1.1",
            targetDays = 30, escalationAfterDays = 45),
        SlaConfig(UUID.randomUUID(), phaseNumber = 1, subPhase = "1.2",
            targetDays = 14, escalationAfterDays = 21),

        // Phase 2 — Clearance Request
        SlaConfig(UUID.randomUUID(), phaseNumber = 2, subPhase = "2.1",
            targetDays = 5, escalationAfterDays = 7),

        // Phase 3 — Financial Settlement
        SlaConfig(UUID.randomUUID(), phaseNumber = 3, subPhase = "3.1",
            targetDays = 30, escalationAfterDays = 45),
    )
}
