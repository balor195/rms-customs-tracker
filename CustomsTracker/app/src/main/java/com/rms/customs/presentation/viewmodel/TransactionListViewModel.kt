package com.rms.customs.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rms.customs.domain.model.Transaction
import com.rms.customs.domain.model.User
import com.rms.customs.domain.model.enums.Beneficiary
import com.rms.customs.domain.model.enums.Department
import com.rms.customs.domain.repository.TransactionRepository
import com.rms.customs.domain.usecase.isVisibleTo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

enum class TxFilter(val labelAr: String) {
    ALL("الكل"),
    ACTIVE("نشطة"),
    BLOCKED("محجوبة"),
    CLOSED("مغلقة"),
}

data class TransactionListUiState(
    val transactions: List<Transaction> = emptyList(),
    val searchQuery: String = "",
    val selectedFilter: TxFilter = TxFilter.ALL,
    val selectedDivision: Department? = null,
    val selectedBeneficiary: Beneficiary? = null,
    val isLoading: Boolean = true,
)

@HiltViewModel
class TransactionListViewModel @Inject constructor(
    transactionRepository: TransactionRepository,
) : ViewModel() {

    private val _searchQuery        = MutableStateFlow("")
    private val _filter             = MutableStateFlow(TxFilter.ALL)
    private val _divisionFilter     = MutableStateFlow<Department?>(null)
    private val _beneficiaryFilter  = MutableStateFlow<Beneficiary?>(null)
    private val _currentUser        = MutableStateFlow<User?>(null)

    val uiState: StateFlow<TransactionListUiState> = combine(
        transactionRepository.observeAll(),
        _searchQuery,
        _filter,
        _beneficiaryFilter,
    ) { transactions, query, filter, beneficiary ->
        Triple(transactions, query, Pair(filter, beneficiary))
    }.combine(_divisionFilter) { (transactions, query, filterPair), division ->
        Triple(transactions, query, Triple(filterPair.first, filterPair.second, division))
    }.combine(_currentUser) { (transactions, query, rest), user ->
        val (filter, beneficiary, division) = rest
        val filtered = transactions
            .filter { tx ->
                matchesSearch(tx, query)
                && matchesStatusFilter(tx, filter)
                && matchesAccessScope(tx, user)
                && (division == null || tx.division == division)
                && (beneficiary == null || tx.beneficiary == beneficiary)
            }
            .sortedByDescending { it.updatedAt }
        TransactionListUiState(
            transactions       = filtered,
            searchQuery        = query,
            selectedFilter     = filter,
            selectedDivision   = division,
            selectedBeneficiary = beneficiary,
            isLoading          = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TransactionListUiState())

    fun setCurrentUser(user: User) { _currentUser.value = user }
    fun onSearchChanged(q: String) { _searchQuery.value = q }
    fun onFilterChanged(f: TxFilter) { _filter.value = f }
    fun onDivisionChanged(d: Department?) { _divisionFilter.value = d }
    fun onBeneficiaryChanged(b: Beneficiary?) { _beneficiaryFilter.value = b }
}

private fun matchesAccessScope(tx: Transaction, user: User?): Boolean =
    user == null || tx.isVisibleTo(user)

private fun matchesSearch(tx: Transaction, query: String): Boolean {
    if (query.isBlank()) return true
    val q = query.trim()
    return tx.transactionRef.contains(q, ignoreCase = true)
        || tx.supplierName.contains(q, ignoreCase = true)
        || tx.title.contains(q, ignoreCase = true)
        || tx.accreditationNumber?.contains(q, ignoreCase = true) == true
        || tx.billOfLadingNumber?.contains(q, ignoreCase = true) == true
        || tx.responsibleOfficer.contains(q, ignoreCase = true)
}

private fun matchesStatusFilter(tx: Transaction, filter: TxFilter): Boolean = when (filter) {
    TxFilter.ALL     -> true
    TxFilter.ACTIVE  -> tx.isActive
    TxFilter.BLOCKED -> tx.isBlocked
    TxFilter.CLOSED  -> tx.currentStatus.isTerminal
}
