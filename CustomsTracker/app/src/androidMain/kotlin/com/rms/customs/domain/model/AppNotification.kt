package com.rms.customs.domain.model

enum class NotificationType {
    SLA_BREACH,
    SLA_ESCALATED,
    PHASE_BLOCKED,
    DOC_MISSING,
    PHASE_ADVANCED,
    TRANSACTION_CLOSED,
    MANUAL;
}

data class AppNotification(
    val id: String,
    val transactionId: String,
    val type: NotificationType,
    val titleAr: String,
    val titleEn: String,
    val messageAr: String,
    val messageEn: String,
    val isRead: Boolean = false,
    val createdAt: Long,
)
