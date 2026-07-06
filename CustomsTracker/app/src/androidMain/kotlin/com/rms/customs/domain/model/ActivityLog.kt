package com.rms.customs.domain.model

import com.rms.customs.domain.model.enums.LogAction

data class ActivityLog(
    val id: String,
    val transactionId: String,
    val userId: String,
    val action: LogAction,
    val fromStatus: String? = null,
    val toStatus: String? = null,
    val payload: String = "{}",        // JSON blob for action-specific data
    val occurredAt: Long,
)
