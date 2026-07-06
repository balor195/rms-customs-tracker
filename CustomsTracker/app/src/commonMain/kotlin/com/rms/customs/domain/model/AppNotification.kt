package com.rms.customs.domain.model

import com.rms.customs.domain.model.enums.NotificationType

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
