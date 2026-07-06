package com.rms.customs.domain.usecase

import com.rms.customs.domain.model.SlaConfig
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

object SlaConfigDefaults {

    // Default SLA targets from RMS Jordan customs clearance workflow.
    // All values are in calendar days. Admins can override via Phase 10 settings.
    @OptIn(ExperimentalUuidApi::class)
    val all: List<SlaConfig> = listOf(
        // Phase 1 — Transaction Preparation
        SlaConfig(Uuid.random().toString(), phaseNumber = 1, subPhase = "1.1",
            targetDays = 30, escalationAfterDays = 45),
        SlaConfig(Uuid.random().toString(), phaseNumber = 1, subPhase = "1.2",
            targetDays = 14, escalationAfterDays = 21),

        // Phase 2 — Shipment Arrival at Airport
        SlaConfig(Uuid.random().toString(), phaseNumber = 2, subPhase = "2.1",
            targetDays = 15, escalationAfterDays = 20),

        // Phase 3 — Clearance
        SlaConfig(Uuid.random().toString(), phaseNumber = 3, subPhase = "3.1",
            targetDays = 5, escalationAfterDays = 7),
    )
}
