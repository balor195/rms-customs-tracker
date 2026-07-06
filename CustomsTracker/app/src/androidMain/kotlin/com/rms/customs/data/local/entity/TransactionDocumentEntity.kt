package com.rms.customs.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.rms.customs.domain.model.TransactionDocument
import com.rms.customs.domain.model.enums.DocumentType
import java.util.UUID

@Entity(
    tableName = "transaction_documents",
    foreignKeys = [
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transactionId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index(value = ["transactionId"]), Index(value = ["phaseRef"])]
)
data class TransactionDocumentEntity(
    @PrimaryKey val id: String,
    val transactionId: String,
    val phaseRef: String,
    val documentType: String,          // DocumentType.name
    val filename: String,
    val filePath: String,
    val remoteUrl: String?,
    val uploadedAt: Long,
    val uploadedByUserId: String,
    val isVerified: Boolean,
    val notes: String?,
)

fun TransactionDocumentEntity.toDomain() = TransactionDocument(
    id = UUID.fromString(id),
    transactionId = UUID.fromString(transactionId),
    phaseRef = phaseRef,
    documentType = DocumentType.valueOf(documentType),
    filename = filename,
    filePath = filePath,
    remoteUrl = remoteUrl,
    uploadedAt = uploadedAt,
    uploadedByUserId = UUID.fromString(uploadedByUserId),
    isVerified = isVerified,
    notes = notes,
)

fun TransactionDocument.toEntity() = TransactionDocumentEntity(
    id = id.toString(),
    transactionId = transactionId.toString(),
    phaseRef = phaseRef,
    documentType = documentType.name,
    filename = filename,
    filePath = filePath,
    remoteUrl = remoteUrl,
    uploadedAt = uploadedAt,
    uploadedByUserId = uploadedByUserId.toString(),
    isVerified = isVerified,
    notes = notes,
)
