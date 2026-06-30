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

        // Phase 2 — Evaluation & Contract
        SlaConfig(UUID.randomUUID(), phaseNumber = 2, subPhase = "2.1",
            targetDays = 45, escalationAfterDays = 60),
        SlaConfig(UUID.randomUUID(), phaseNumber = 2, subPhase = "2.2",
            targetDays = 14, escalationAfterDays = 21),

        // Phase 3 — Clearance Documentation
        SlaConfig(UUID.randomUUID(), phaseNumber = 3, subPhase = "3.1",
            targetDays = 7, escalationAfterDays = 10),
        SlaConfig(UUID.randomUUID(), phaseNumber = 3, subPhase = "3.2",
            targetDays = 5, escalationAfterDays = 7),
        SlaConfig(UUID.randomUUID(), phaseNumber = 3, subPhase = "3.3",
            targetDays = 3, escalationAfterDays = 5),

        // Phase 4 — Gov-Agency Processing (parallel tracks)
        // Military Command (القيادة العامة) is the highest-risk bottleneck — mandatory in every transaction
        SlaConfig(UUID.randomUUID(), phaseNumber = 4, subPhase = "4.1",
            targetDays = 15, escalationAfterDays = 20),   // القيادة العامة — Military Command
        SlaConfig(UUID.randomUUID(), phaseNumber = 4, subPhase = "4.2",
            targetDays = 10, escalationAfterDays = 14),   // الجمارك الأردنية — Jordan Customs
        SlaConfig(UUID.randomUUID(), phaseNumber = 4, subPhase = "4.3",
            targetDays = 12, escalationAfterDays = 16),   // مؤسسة الغذاء والدواء — JFDA
        SlaConfig(UUID.randomUUID(), phaseNumber = 4, subPhase = "4.4",
            targetDays = 7,  escalationAfterDays = 10),   // وزارة الصحة — optional, some devices/pharma only

        // Phase 5 — Release Order
        SlaConfig(UUID.randomUUID(), phaseNumber = 5, subPhase = "5.1",
            targetDays = 5, escalationAfterDays = 7),

        // Phase 6 — Transit & Receipt
        SlaConfig(UUID.randomUUID(), phaseNumber = 6, subPhase = "6.1",
            targetDays = 7, escalationAfterDays = 10),
        SlaConfig(UUID.randomUUID(), phaseNumber = 6, subPhase = "6.2",
            targetDays = 5, escalationAfterDays = 7),
        SlaConfig(UUID.randomUUID(), phaseNumber = 6, subPhase = "6.3",
            targetDays = 3, escalationAfterDays = 5),

        // Phase 7 — Financial Settlement
        SlaConfig(UUID.randomUUID(), phaseNumber = 7, subPhase = "7.1",
            targetDays = 30, escalationAfterDays = 45),
    )
}
