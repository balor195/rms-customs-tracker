package com.rms.customs.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rms.customs.domain.model.PhaseRecord
import com.rms.customs.domain.model.Transaction
import com.rms.customs.domain.model.enums.Department
import com.rms.customs.domain.model.enums.ShipmentStatus
import com.rms.customs.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class OverdueItem(
    val transactionId: String,
    val transactionRef: String,
    val supplierName: String,
    val phaseLabel: String,
    val overdueDays: Int,
)

data class DivisionValueEntry(
    val labelAr: String,
    val count: Int,
    val totalValue: Double,
)

data class DashboardStats(
    // Existing summary
    val totalActive: Int = 0,
    val overdueSlaCount: Int = 0,
    val closedThisMonth: Int = 0,
    val phaseDistribution: List<Pair<String, Int>> = emptyList(),
    val avgTotalDays: Double? = null,
    val avgMilitaryDays: Double? = null,
    val avgCustomsDays: Double? = null,
    val delayedRatioPct: Float = 0f,
    val overdueItems: List<OverdueItem> = emptyList(),
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

    val stats: StateFlow<DashboardStats> = combine(
        transactionRepository.observeAll(),
        transactionRepository.observeAllInProgressPhases(),
        transactionRepository.observeCompletedPhasesBySubPhase("4.1"),
        transactionRepository.observeCompletedPhasesBySubPhase("4.2"),
    ) { txs, inProgressPhases, militaryPhases, customsPhases ->
        computeStats(txs, inProgressPhases, militaryPhases, customsPhases)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardStats())

    private fun computeStats(
        txs: List<Transaction>,
        inProgressPhases: List<PhaseRecord>,
        militaryPhases: List<PhaseRecord>,
        customsPhases: List<PhaseRecord>,
    ): DashboardStats {
        val now          = System.currentTimeMillis()
        val startOfMonth = startOfCurrentMonth()
        val sevenDaysMs  = 7L * 86_400_000L

        // ── Summary cards ────────────────────────────────────────────────────
        val totalActive     = txs.count { it.isActive }
        val closedThisMonth = txs.count { it.closedAt != null && it.closedAt >= startOfMonth }
        val overdueSlaCount = inProgressPhases.count { p ->
            p.startedAt != null && p.slaTargetDays != null &&
            TimeUnit.MILLISECONDS.toDays(now - p.startedAt) > p.slaTargetDays
        }

        // ── Phase distribution ───────────────────────────────────────────────
        val phaseGroups = listOf(
            "المرحلة 1 — إعداد العطاء"    to setOf("TENDER_PREPARATION", "TENDER_PUBLISHED", "DRAFT"),
            "المرحلة 2 — التقييم والعقد"  to setOf("EVALUATION_IN_PROGRESS", "CONTRACT_PENDING_SIGNATURE", "CONTRACT_SIGNED"),
            "المرحلة 3 — وثائق التخليص"  to setOf("CLEARANCE_DOCS_PREPARATION", "DECLARATION_SUBMITTED"),
            "المرحلة 4 — الجهات الحكومية" to setOf("GOV_PROCESSING", "GOV_APPROVED"),
            "المرحلة 5 — أمر الإفراج"    to setOf("FINAL_RELEASE_ISSUED"),
            "المرحلة 6 — النقل والاستلام" to setOf("IN_TRANSIT", "RECEIVED_AT_WAREHOUSE", "INSPECTION_COMPLETE"),
            "المرحلة 7 — الإغلاق المالي"  to setOf("FINANCIAL_SETTLEMENT_PENDING"),
        )
        val phaseDistribution = phaseGroups.map { (label, statuses) ->
            label to txs.count { it.currentStatus.name in statuses && it.isActive }
        }

        // ── Average durations ────────────────────────────────────────────────
        val closedTxs    = txs.filter { it.closedAt != null }
        val avgTotalDays = closedTxs
            .map { (it.closedAt!! - it.createdAt).toDouble() / 86_400_000.0 }
            .takeIf { it.isNotEmpty() }?.average()

        val avgMilitaryDays = militaryPhases
            .mapNotNull { p ->
                if (p.startedAt != null && p.completedAt != null)
                    (p.completedAt - p.startedAt).toDouble() / 86_400_000.0
                else null
            }
            .takeIf { it.isNotEmpty() }?.average()

        val avgCustomsDays = customsPhases
            .mapNotNull { p ->
                if (p.startedAt != null && p.completedAt != null)
                    (p.completedAt - p.startedAt).toDouble() / 86_400_000.0
                else null
            }
            .takeIf { it.isNotEmpty() }?.average()

        // ── Delayed ratio ────────────────────────────────────────────────────
        val delayedRatioPct = if (totalActive == 0) 0f else
            overdueSlaCount.toFloat() / totalActive * 100f

        // ── Priority overdue list ─────────────────────────────────────────────
        val txById = txs.associateBy { it.id }
        val overdueItems = inProgressPhases
            .filter { p ->
                p.startedAt != null && p.slaTargetDays != null &&
                TimeUnit.MILLISECONDS.toDays(now - p.startedAt) > p.slaTargetDays
            }
            .mapNotNull { phase ->
                val tx          = txById[phase.transactionId] ?: return@mapNotNull null
                val daysElapsed = TimeUnit.MILLISECONDS.toDays(now - phase.startedAt!!).toInt()
                val overdueDays = daysElapsed - phase.slaTargetDays!!
                OverdueItem(
                    transactionId  = tx.id.toString(),
                    transactionRef = tx.transactionRef,
                    supplierName   = tx.supplierName,
                    phaseLabel     = subPhaseLabel(phase.phaseNumber, phase.subPhase),
                    overdueDays    = overdueDays,
                )
            }
            .sortedByDescending { it.overdueDays }

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
            overdueSlaCount      = overdueSlaCount,
            closedThisMonth      = closedThisMonth,
            phaseDistribution    = phaseDistribution,
            avgTotalDays         = avgTotalDays,
            avgMilitaryDays      = avgMilitaryDays,
            avgCustomsDays       = avgCustomsDays,
            delayedRatioPct      = delayedRatioPct,
            overdueItems         = overdueItems,
            shipmentExpected     = shipmentExpected,
            shipmentArrived      = shipmentArrived,
            shipmentCleared      = shipmentCleared,
            upcomingArrivalsCount = upcomingArrivals,
            valueByDivision      = valueByDivision,
            isLoaded             = true,
        )
    }

    private fun subPhaseLabel(phaseNumber: Int, subPhase: String): String = when (subPhase) {
        "4.1" -> "القيادة العامة — الإعفاء"
        "4.2" -> "الجمارك الأردنية — الفحص"
        "4.3" -> "هيئة الغذاء والدواء — الإذن"
        else  -> "المرحلة $phaseNumber"
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
