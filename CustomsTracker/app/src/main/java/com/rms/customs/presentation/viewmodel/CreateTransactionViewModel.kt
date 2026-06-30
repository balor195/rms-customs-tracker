package com.rms.customs.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rms.customs.domain.model.Transaction
import com.rms.customs.domain.model.enums.Beneficiary
import com.rms.customs.domain.model.enums.Department
import com.rms.customs.domain.model.enums.Priority
import com.rms.customs.domain.model.enums.TransactionPhase
import com.rms.customs.domain.model.enums.TransactionStatus
import com.rms.customs.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class CreateTransactionUiState(
    val generatedRef: String = "",
    val isLoading: Boolean   = true,
    val isSaved: Boolean     = false,
    val error: String?       = null,
)

@HiltViewModel
class CreateTransactionViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateTransactionUiState())
    val uiState: StateFlow<CreateTransactionUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val ref = transactionRepository.generateRef()
            _uiState.update { it.copy(generatedRef = ref, isLoading = false) }
        }
    }

    fun create(
        title: String,
        accreditationNumber: String,
        responsibleOfficer: String,
        division: Department,
        beneficiary: Beneficiary,
        supplierName: String,
        totalValue: String,
        tenderRef: String,
        billOfLadingNumber: String,
        expectedArrivalDate: Long?,
        notes: String,
        priority: Priority,
        createdByUserId: UUID,
    ) {
        if (accreditationNumber.isBlank()) {
            _uiState.update { it.copy(error = "الرجاء إدخال رقم الاعتماد") }
            return
        }
        if (responsibleOfficer.isBlank()) {
            _uiState.update { it.copy(error = "الرجاء إدخال اسم الضابط المسؤول") }
            return
        }
        if (supplierName.isBlank()) {
            _uiState.update { it.copy(error = "الرجاء إدخال اسم الشركة الموردة") }
            return
        }
        if (title.isBlank()) {
            _uiState.update { it.copy(error = "الرجاء إدخال وصف المعاملة") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching {
                val now = System.currentTimeMillis()
                transactionRepository.create(
                    Transaction(
                        id                  = UUID.randomUUID(),
                        transactionRef      = _uiState.value.generatedRef,
                        title               = title.trim(),
                        division            = division,
                        accreditationNumber = accreditationNumber.trim(),
                        billOfLadingNumber  = billOfLadingNumber.trim().ifBlank { null },
                        responsibleOfficer  = responsibleOfficer.trim(),
                        beneficiary         = beneficiary,
                        tenderRef           = tenderRef.trim().ifBlank { null },
                        supplierName        = supplierName.trim(),
                        totalValue          = totalValue.trim().toDoubleOrNull(),
                        expectedArrivalDate = expectedArrivalDate,
                        currentPhase        = TransactionPhase.PHASE_1_TENDER,
                        currentStatus       = TransactionStatus.TENDER_PREPARATION,
                        priority            = priority,
                        createdAt           = now,
                        createdByUserId     = createdByUserId,
                        updatedAt           = now,
                        notes               = notes.trim().ifBlank { null },
                    )
                )
            }
                .onSuccess { _uiState.update { it.copy(isSaved = true, isLoading = false) } }
                .onFailure { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
        }
    }

    fun clearError() { _uiState.update { it.copy(error = null) } }
}
