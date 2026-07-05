package com.rms.customs.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.rms.customs.domain.model.Transaction
import com.rms.customs.domain.model.enums.Beneficiary
import com.rms.customs.domain.model.enums.Department
import com.rms.customs.domain.model.enums.Priority
import com.rms.customs.domain.model.enums.TransactionPhase
import com.rms.customs.domain.model.enums.TransactionStatus
import java.util.UUID

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["transactionRef"], unique = true),
        Index(value = ["currentStatus"]),
        Index(value = ["updatedAt"]),
        Index(value = ["accreditationNumber"]),
    ]
)
data class TransactionEntity(
    @PrimaryKey val id: String,
    val transactionRef: String,
    val title: String,
    val division: String?,                  // Department.name or null
    val accreditationNumber: String?,       // رقم الاعتماد
    val billOfLadingNumber: String?,        // رقم بوليصة الشحن
    val responsibleOfficer: String,         // اسم الضابط المسؤول
    val beneficiary: String?,               // Beneficiary.name or null
    val tenderRef: String?,
    val contractRef: String?,
    val supplierName: String,
    val totalValue: Double?,
    val currency: String,
    val expectedArrivalDate: Long?,         // millis
    val actualArrivalDate: Long?,           // millis
    val weightKg: Double?,                  // وزن الشحنة (كغم)
    val isRefrigerated: Boolean,            // هل الشحنة مبرّدة
    val defaultShelfLife: String?,          // العمر الافتراضي (شعبة المستهلكات فقط)
    val currentPhase: String,              // TransactionPhase.name
    val currentStatus: String,             // TransactionStatus.name
    val exceptionState: String?,           // TransactionStatus.name or null
    val priority: String,                  // Priority.name
    val createdAt: Long,
    val createdByUserId: String,
    val updatedAt: Long,
    val closedAt: Long?,
    val notes: String?,
)

fun TransactionEntity.toDomain() = Transaction(
    id = UUID.fromString(id),
    transactionRef = transactionRef,
    title = title,
    division = division?.let { Department.valueOf(it) },
    accreditationNumber = accreditationNumber,
    billOfLadingNumber = billOfLadingNumber,
    responsibleOfficer = responsibleOfficer,
    beneficiary = beneficiary?.let { Beneficiary.valueOf(it) },
    tenderRef = tenderRef,
    contractRef = contractRef,
    supplierName = supplierName,
    totalValue = totalValue,
    currency = currency,
    expectedArrivalDate = expectedArrivalDate,
    actualArrivalDate = actualArrivalDate,
    weightKg = weightKg,
    isRefrigerated = isRefrigerated,
    defaultShelfLife = defaultShelfLife,
    currentPhase = TransactionPhase.valueOf(currentPhase),
    currentStatus = TransactionStatus.valueOf(currentStatus),
    exceptionState = exceptionState?.let { TransactionStatus.valueOf(it) },
    priority = Priority.valueOf(priority),
    createdAt = createdAt,
    createdByUserId = UUID.fromString(createdByUserId),
    updatedAt = updatedAt,
    closedAt = closedAt,
    notes = notes,
)

fun Transaction.toEntity() = TransactionEntity(
    id = id.toString(),
    transactionRef = transactionRef,
    title = title,
    division = division?.name,
    accreditationNumber = accreditationNumber,
    billOfLadingNumber = billOfLadingNumber,
    responsibleOfficer = responsibleOfficer,
    beneficiary = beneficiary?.name,
    tenderRef = tenderRef,
    contractRef = contractRef,
    supplierName = supplierName,
    totalValue = totalValue,
    currency = currency,
    expectedArrivalDate = expectedArrivalDate,
    actualArrivalDate = actualArrivalDate,
    weightKg = weightKg,
    isRefrigerated = isRefrigerated,
    defaultShelfLife = defaultShelfLife,
    currentPhase = currentPhase.name,
    currentStatus = currentStatus.name,
    exceptionState = exceptionState?.name,
    priority = priority.name,
    createdAt = createdAt,
    createdByUserId = createdByUserId.toString(),
    updatedAt = updatedAt,
    closedAt = closedAt,
    notes = notes,
)
