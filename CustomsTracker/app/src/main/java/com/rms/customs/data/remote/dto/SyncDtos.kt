package com.rms.customs.data.remote.dto

import com.rms.customs.data.local.entity.PhaseRecordEntity
import com.rms.customs.data.local.entity.TransactionEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PhaseRecordSyncDto(
    val id: String,
    @SerialName("transaction_id") val transactionId: String,
    @SerialName("phase_number") val phaseNumber: Int,
    @SerialName("sub_phase") val subPhase: String,
    val status: String,
    @SerialName("assigned_to_entity") val assignedToEntity: String,
    @SerialName("started_at") val startedAt: Long? = null,
    @SerialName("completed_at") val completedAt: Long? = null,
    @SerialName("sla_target_days") val slaTargetDays: Int? = null,
    @SerialName("blocker_reason") val blockerReason: String? = null,
    @SerialName("completed_by_user_id") val completedByUserId: String? = null,
    val notes: String? = null,
)

@Serializable
data class TransactionSyncDto(
    val id: String,
    @SerialName("transaction_ref") val transactionRef: String,
    val title: String,
    val division: String? = null,
    @SerialName("accreditation_number") val accreditationNumber: String? = null,
    @SerialName("bill_of_lading_number") val billOfLadingNumber: String? = null,
    @SerialName("responsible_officer") val responsibleOfficer: String = "",
    val beneficiary: String? = null,
    @SerialName("tender_ref") val tenderRef: String? = null,
    @SerialName("contract_ref") val contractRef: String? = null,
    @SerialName("supplier_name") val supplierName: String,
    @SerialName("total_value") val totalValue: Double? = null,
    val currency: String = "JOD",
    @SerialName("expected_arrival_date") val expectedArrivalDate: Long? = null,
    @SerialName("actual_arrival_date") val actualArrivalDate: Long? = null,
    @SerialName("shipment_status") val shipmentStatus: String = "EXPECTED",
    @SerialName("current_phase") val currentPhase: String,
    @SerialName("current_status") val currentStatus: String,
    @SerialName("exception_state") val exceptionState: String? = null,
    val priority: String,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("created_by_user_id") val createdByUserId: String,
    @SerialName("updated_at") val updatedAt: Long,
    @SerialName("closed_at") val closedAt: Long? = null,
    val notes: String? = null,
    @SerialName("phase_records") val phaseRecords: List<PhaseRecordSyncDto> = emptyList(),
)

@Serializable
data class SyncPushRequest(
    @SerialName("device_id") val deviceId: String,
    @SerialName("pushed_at") val pushedAt: Long,
    val transactions: List<TransactionSyncDto>,
)

@Serializable
data class SyncPushResponse(
    val accepted: Int,
)

@Serializable
data class SyncPullResponse(
    val transactions: List<TransactionSyncDto>,
    @SerialName("server_time_ms") val serverTimeMs: Long,
)

// ── Entity ↔ DTO converters ───────────────────────────────────────────────────

fun TransactionEntity.toSyncDto(phases: List<PhaseRecordEntity> = emptyList()) = TransactionSyncDto(
    id                   = id,
    transactionRef       = transactionRef,
    title                = title,
    division             = division,
    accreditationNumber  = accreditationNumber,
    billOfLadingNumber   = billOfLadingNumber,
    responsibleOfficer   = responsibleOfficer,
    beneficiary          = beneficiary,
    tenderRef            = tenderRef,
    contractRef          = contractRef,
    supplierName         = supplierName,
    totalValue           = totalValue,
    currency             = currency,
    expectedArrivalDate  = expectedArrivalDate,
    actualArrivalDate    = actualArrivalDate,
    shipmentStatus       = shipmentStatus,
    currentPhase         = currentPhase,
    currentStatus        = currentStatus,
    exceptionState       = exceptionState,
    priority             = priority,
    createdAt            = createdAt,
    createdByUserId      = createdByUserId,
    updatedAt            = updatedAt,
    closedAt             = closedAt,
    notes                = notes,
    phaseRecords         = phases.map { it.toSyncDto() },
)

fun PhaseRecordEntity.toSyncDto() = PhaseRecordSyncDto(
    id                 = id,
    transactionId      = transactionId,
    phaseNumber        = phaseNumber,
    subPhase           = subPhase,
    status             = status,
    assignedToEntity   = assignedToEntity,
    startedAt          = startedAt,
    completedAt        = completedAt,
    slaTargetDays      = slaTargetDays,
    blockerReason      = blockerReason,
    completedByUserId  = completedByUserId,
    notes              = notes,
)

fun TransactionSyncDto.toEntity() = TransactionEntity(
    id                   = id,
    transactionRef       = transactionRef,
    title                = title,
    division             = division,
    accreditationNumber  = accreditationNumber,
    billOfLadingNumber   = billOfLadingNumber,
    responsibleOfficer   = responsibleOfficer,
    beneficiary          = beneficiary,
    tenderRef            = tenderRef,
    contractRef          = contractRef,
    supplierName         = supplierName,
    totalValue           = totalValue,
    currency             = currency,
    expectedArrivalDate  = expectedArrivalDate,
    actualArrivalDate    = actualArrivalDate,
    shipmentStatus       = shipmentStatus,
    currentPhase         = currentPhase,
    currentStatus        = currentStatus,
    exceptionState       = exceptionState,
    priority             = priority,
    createdAt            = createdAt,
    createdByUserId      = createdByUserId,
    updatedAt            = updatedAt,
    closedAt             = closedAt,
    notes                = notes,
)

fun PhaseRecordSyncDto.toEntity() = PhaseRecordEntity(
    id                 = id,
    transactionId      = transactionId,
    phaseNumber        = phaseNumber,
    subPhase           = subPhase,
    status             = status,
    assignedToEntity   = assignedToEntity,
    startedAt          = startedAt,
    completedAt        = completedAt,
    slaTargetDays      = slaTargetDays,
    blockerReason      = blockerReason,
    completedByUserId  = completedByUserId,
    notes              = notes,
)
