package com.rms.customs.domain.model

import java.util.UUID

data class SlaConfig(
    val id: UUID,
    val phaseNumber: Int,
    val subPhase: String,
    val targetDays: Int,
    val escalationAfterDays: Int,
    val isActive: Boolean = true,
)
