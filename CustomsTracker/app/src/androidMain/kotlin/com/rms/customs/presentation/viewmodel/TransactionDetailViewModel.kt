package com.rms.customs.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rms.customs.domain.model.ActivityLog
import com.rms.customs.domain.model.Transaction
import com.rms.customs.domain.model.enums.TransactionStatus
import com.rms.customs.domain.model.enums.UserRole
import com.rms.customs.domain.repository.TransactionRepository
import com.rms.customs.domain.statemachine.TransactionStateMachine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class TransitionUiState {
    object Idle    : TransitionUiState()
    object Loading : TransitionUiState()
    object Success : TransitionUiState()
    data class Error(val message: String) : TransitionUiState()
}

class TransactionDetailViewModel(
    private val transactionRepository: TransactionRepository,
    private val stateMachine: TransactionStateMachine,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val txId: String = requireNotNull(savedStateHandle["id"])

    val transaction: StateFlow<Transaction?> = transactionRepository.observeById(txId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val activityLog: StateFlow<List<ActivityLog>> = transactionRepository.observeActivityLog(txId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _transitionState = MutableStateFlow<TransitionUiState>(TransitionUiState.Idle)
    val transitionState: StateFlow<TransitionUiState> = _transitionState

    fun nextForwardStatus(): TransactionStatus? {
        val tx = transaction.value ?: return null
        val from = tx.exceptionState ?: tx.currentStatus
        return stateMachine.nextForwardStatus(from)
    }

    fun advanceStatus(newStatus: TransactionStatus, actorUserId: String, actorRole: UserRole) {
        viewModelScope.launch {
            _transitionState.value = TransitionUiState.Loading
            runCatching { transactionRepository.advanceStatus(txId, newStatus, actorUserId, actorRole) }
                .onSuccess { _transitionState.value = TransitionUiState.Success }
                .onFailure { _transitionState.value = TransitionUiState.Error(it.message ?: "خطأ غير معروف") }
        }
    }

    fun setBlocker(reason: String, actorUserId: String) {
        viewModelScope.launch {
            _transitionState.value = TransitionUiState.Loading
            runCatching {
                transactionRepository.setExceptionState(txId, TransactionStatus.BLOCKED, reason, actorUserId)
            }
                .onSuccess { _transitionState.value = TransitionUiState.Success }
                .onFailure { _transitionState.value = TransitionUiState.Error(it.message ?: "خطأ غير معروف") }
        }
    }

    fun clearBlocker(actorUserId: String) {
        viewModelScope.launch {
            _transitionState.value = TransitionUiState.Loading
            runCatching { transactionRepository.clearExceptionState(txId, actorUserId) }
                .onSuccess { _transitionState.value = TransitionUiState.Success }
                .onFailure { _transitionState.value = TransitionUiState.Error(it.message ?: "خطأ غير معروف") }
        }
    }

    fun updateNotes(notes: String) {
        viewModelScope.launch {
            val tx = transaction.value ?: return@launch
            transactionRepository.update(
                tx.copy(notes = notes.ifBlank { null }, updatedAt = System.currentTimeMillis())
            )
        }
    }

    fun resetTransitionState() { _transitionState.value = TransitionUiState.Idle }

    fun updateBillOfLading(number: String) {
        viewModelScope.launch {
            val tx = transaction.value ?: return@launch
            transactionRepository.update(
                tx.copy(
                    billOfLadingNumber = number.trim().ifBlank { null },
                    updatedAt          = System.currentTimeMillis(),
                )
            )
        }
    }
}
