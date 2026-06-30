package com.rms.customs.domain.model

import java.util.UUID

enum class NotificationType {
    SLA_BREACH,
    SLA_ESCALATED,
    PHASE_BLOCKED,
    DOC_MISSING,
    PHASE_ADVANCED,
    MANUAL;
}

data class AppNotification(
    val id: UUID,
    val transactionId: UUID,
    val type: NotificationType,
    val titleAr: String,
    val titleEn: String,
    val messageAr: String,
    val messageEn: String,
    val isRead: Boolean = false,
    val createdAt: Long,
)
