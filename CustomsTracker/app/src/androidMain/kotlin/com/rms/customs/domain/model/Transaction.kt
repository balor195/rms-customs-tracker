package com.rms.customs.domain.model

import com.rms.customs.domain.model.enums.Beneficiary
import com.rms.customs.domain.model.enums.Department
import com.rms.customs.domain.model.enums.Priority
import com.rms.customs.domain.model.enums.TransactionPhase
import com.rms.customs.domain.model.enums.TransactionStatus

data class Transaction(
    val id: String,
    val transactionRef: String,                    // internal ref e.g. "RMS-2026-0042"
    val title: String,
    val division: Department? = null,               // شعبة الدواء / المستهلكات / الأجهزة
    val accreditationNumber: String? = null,        // رقم الاعتماد
    val billOfLadingNumber: String? = null,         // رقم بوليصة الشحن
    val responsibleOfficer: String = "",            // اسم الضابط المسؤول
    val beneficiary: Beneficiary? = null,           // الجهة المستفيدة: RMS أو البنك
    val tenderRef: String? = null,
    val contractRef: String? = null,
    val supplierName: String,
    val totalValue: Double? = null,
    val currency: String = "JOD",
    val expectedArrivalDate: Long? = null,          // تاريخ الوصول المتوقع
    val actualArrivalDate: Long? = null,            // تاريخ الوصول الفعلي — تُسجَّل تلقائياً عند "وصلت الشحنة للمطار"
    val weightKg: Double? = null,                   // وزن الشحنة (كغم)
    val isRefrigerated: Boolean = false,            // هل الشحنة مبرّدة
    val defaultShelfLife: String? = null,           // العمر الافتراضي (شعبة المستهلكات فقط)
    val currentPhase: TransactionPhase,
    val currentStatus: TransactionStatus,
    val exceptionState: TransactionStatus? = null,  // BLOCKED / ON_HOLD / DISPUTED overlay
    val priority: Priority = Priority.NORMAL,
    val createdAt: Long,
    val createdByUserId: String,
    val updatedAt: Long,
    val closedAt: Long? = null,
    val notes: String? = null,
) {
    val isActive: Boolean get() = !currentStatus.isTerminal && exceptionState == null
    val isBlocked: Boolean get() = exceptionState != null
    val daysSinceUpdate: Long
        get() = (System.currentTimeMillis() - updatedAt) / 86_400_000L
}
