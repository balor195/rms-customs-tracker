package com.rms.customs.domain.model

import com.rms.customs.domain.model.enums.DocumentType
import java.util.UUID

data class TransactionDocument(
    val id: UUID,
    val transactionId: UUID,
    val phaseRef: String,              // e.g. "3.3.1"
    val documentType: DocumentType,
    val filename: String,
    val filePath: String,              // local URI
    val remoteUrl: String? = null,     // set after Phase 9 sync
    val uploadedAt: Long,
    val uploadedByUserId: UUID,
    val isVerified: Boolean = false,
    val notes: String? = null,
)
