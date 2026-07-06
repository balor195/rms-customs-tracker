package com.rms.customs.domain.model

data class SlaConfig(
    val id: String,
    val phaseNumber: Int,
    val subPhase: String,
    val targetDays: Int,
    val escalationAfterDays: Int,
    val isActive: Boolean = true,
)
