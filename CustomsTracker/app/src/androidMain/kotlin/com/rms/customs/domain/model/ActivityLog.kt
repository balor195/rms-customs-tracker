package com.rms.customs.domain.model

import com.rms.customs.domain.model.enums.LogAction
import java.util.UUID

data class ActivityLog(
    val id: UUID,
    val transactionId: UUID,
    val userId: UUID,
    val action: LogAction,
    val fromStatus: String? = null,
    val toStatus: String? = null,
    val payload: String = "{}",        // JSON blob for action-specific data
    val occurredAt: Long,
)
