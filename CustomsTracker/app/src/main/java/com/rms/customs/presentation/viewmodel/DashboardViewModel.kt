package com.rms.customs.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rms.customs.domain.model.Transaction
import com.rms.customs.domain.model.User
import com.rms.customs.domain.model.enums.Department
import com.rms.customs.domain.model.enums.ShipmentStatus
import com.rms.customs.domain.repository.TransactionRepository
import com.rms.customs.domain.usecase.isVisibleTo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import javax.inject.Inject

data class DivisionValueEntry(
    val labelAr: String,
    val count: Int,
    val totalValue: Double,
)

data class DashboardStats(
    // Existing summary
    val totalActive: Int = 0,
    val closedThisMonth: Int = 0,
    val phaseDistribution: List<Pair<String, Int>> = emptyList(),
    val avgTotalDays: Double? = null,
    // Shipment status breakdown
    val shipmentExpected: Int = 0,
    val shipmentArrived: Int = 0,
    val shipmentCleared: Int = 0,
    val upcomingArrivalsCount: Int = 0,
    // Value by division
    val valueByDivision: List<DivisionValueEntry> = emptyList(),
    val isLoaded: Boolean = false,
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
) : ViewModel() {

    private val _currentUser = MutableStateFlow<User?>(null)

    val stats: StateFlow<DashboardStats> = combine(
        transactionRepository.observeAll(),
        _currentUser,
    ) { txs, user ->
        val scoped = if (user == null) txs else txs.filter { it.isVisibleTo(user) }
        computeStats(scoped)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardStats())

    fun setCurrentUser(user: User) { _currentUser.value = user }

    private fun computeStats(txs: List<Transaction>): DashboardStats {
        val startOfMonth = startOfCurrentMonth()
        val now          = System.currentTimeMillis()
        val sevenDaysMs  = 7L * 86_400_000L

        // ── Summary cards ────────────────────────────────────────────────────
        val totalActive     = txs.count { it.isActive }
        val closedThisMonth = txs.count { it.closedAt != null && it.closedAt >= startOfMonth }

        // ── Phase distribution ───────────────────────────────────────────────
        val phaseGroups = listOf(
            "المرحلة 1 — إعداد العطاء"       to setOf("TENDER_PREPARATION", "TENDER_PUBLISHED", "DRAFT"),
            "المرحلة 2 — طلب تخليص"          to setOf("CLEARANCE_ISSUED"),
            "المرحلة 3 — الإغلاق المالي"     to setOf("FINANCIAL_SETTLEMENT_PENDING", "CLOSED"),
            "المرحلة 4 — تم النقل للمستودعات" to setOf("TRANSFERRED_TO_WAREHOUSE"),
        )
        val phaseDistribution = phaseGroups.map { (label, statuses) ->
            label to txs.count { it.currentStatus.name in statuses && it.isActive }
        }

        // ── Average durations ────────────────────────────────────────────────
        val closedTxs    = txs.filter { it.closedAt != null }
        val avgTotalDays = closedTxs
            .map { (it.closedAt!! - it.createdAt).toDouble() / 86_400_000.0 }
            .takeIf { it.isNotEmpty() }?.average()

        // ── Shipment status breakdown ─────────────────────────────────────────
        val shipmentExpected = txs.count { it.shipmentStatus == ShipmentStatus.EXPECTED && it.isActive }
        val shipmentArrived  = txs.count { it.shipmentStatus == ShipmentStatus.ARRIVED }
        val shipmentCleared  = txs.count { it.shipmentStatus == ShipmentStatus.CLEARED }
        val upcomingArrivals = txs.count { tx ->
            tx.shipmentStatus == ShipmentStatus.EXPECTED &&
            tx.expectedArrivalDate != null &&
            tx.expectedArrivalDate in now..(now + sevenDaysMs)
        }

        // ── Value by Division ─────────────────────────────────────────────────
        val valueByDivision = Department.entries.map { dept ->
            val list = txs.filter { it.division == dept }
            DivisionValueEntry(
                labelAr    = dept.labelAr,
                count      = list.size,
                totalValue = list.mapNotNull { it.totalValue }.sum(),
            )
        }

        return DashboardStats(
            totalActive          = totalActive,
            closedThisMonth      = closedThisMonth,
            phaseDistribution    = phaseDistribution,
            avgTotalDays         = avgTotalDays,
            shipmentExpected     = shipmentExpected,
            shipmentArrived      = shipmentArrived,
            shipmentCleared      = shipmentCleared,
            upcomingArrivalsCount = upcomingArrivals,
            valueByDivision      = valueByDivision,
            isLoaded             = true,
        )
    }

    private fun startOfCurrentMonth(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
