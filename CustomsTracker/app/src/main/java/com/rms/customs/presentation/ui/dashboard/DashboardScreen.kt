package com.rms.customs.presentation.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rms.customs.presentation.ui.theme.CustomsColors
import com.rms.customs.presentation.viewmodel.DashboardStats
import com.rms.customs.presentation.viewmodel.DashboardViewModel
import com.rms.customs.presentation.viewmodel.DivisionValueEntry
import com.rms.customs.presentation.viewmodel.OverdueItem
import kotlin.math.roundToInt

@Composable
fun DashboardScreen(
    onTransactionClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val stats by viewModel.stats.collectAsStateWithLifecycle()

    if (!stats.isLoaded) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
        modifier            = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        // ── Summary cards ────────────────────────────────────────────────────
        item { SectionHeader("ملخص المعاملات") }
        item {
            Row(
                modifier              = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SummaryCard(
                    label    = "نشطة",
                    value    = stats.totalActive.toString(),
                    color    = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                SummaryCard(
                    label    = "تجاوزت SLA",
                    value    = stats.overdueSlaCount.toString(),
                    color    = if (stats.overdueSlaCount > 0) CustomsColors.Overdue
                               else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.weight(1f),
                )
                SummaryCard(
                    label    = "مغلقة هذا الشهر",
                    value    = stats.closedThisMonth.toString(),
                    color    = CustomsColors.OnTime,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // ── Shipment status breakdown ────────────────────────────────────────
        item { SectionHeader("حالة الشحنات") }
        item {
            ShipmentStatusRow(
                expected         = stats.shipmentExpected,
                arrived          = stats.shipmentArrived,
                cleared          = stats.shipmentCleared,
                upcomingArrivals = stats.upcomingArrivalsCount,
                modifier         = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            )
        }

        // ── Phase distribution chart ─────────────────────────────────────────
        item { SectionHeader("توزيع المعاملات حسب المرحلة") }
        item {
            PhaseDistributionChart(
                distribution = stats.phaseDistribution,
                modifier     = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            )
        }

        // ── Value by Division chart ──────────────────────────────────────────
        if (stats.valueByDivision.any { it.count > 0 }) {
            item { SectionHeader("القيمة حسب الشعبة الطبية") }
            item {
                ValueByDivisionChart(
                    entries  = stats.valueByDivision,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                )
            }
        }

        // ── KPIs ─────────────────────────────────────────────────────────────
        item { SectionHeader("مؤشرات الأداء (KPIs)") }
        item {
            KpiGrid(
                stats    = stats,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            )
        }

        // ── Priority overdue list ─────────────────────────────────────────────
        item { SectionHeader("أعلى المعاملات تأخيراً") }
        if (stats.overdueItems.isNotEmpty()) {
            items(stats.overdueItems, key = { it.transactionId + it.phaseLabel }) { item ->
                OverdueRow(
                    item    = item,
                    onClick = { onTransactionClick(item.transactionId) },
                )
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            }
        } else {
            item {
                Box(
                    modifier         = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text  = "لا توجد معاملات متأخرة عن الجدول الزمني ✓",
                        style = MaterialTheme.typography.bodySmall,
                        color = CustomsColors.OnTime,
                    )
                }
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

// ── Section header ────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Text(
        text       = title,
        style      = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color      = MaterialTheme.colorScheme.primary,
        modifier   = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

// ── Summary card ──────────────────────────────────────────────────────────────

@Composable
private fun SummaryCard(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier  = modifier,
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier            = Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text       = value,
                style      = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color      = color,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text     = label,
                style    = MaterialTheme.typography.labelSmall,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ── Shipment status breakdown ─────────────────────────────────────────────────

private val ShipmentExpectedColor = Color(0xFFF57F17)  // amber
private val ShipmentArrivedColor  = Color(0xFF1565C0)  // blue
private val ShipmentClearedColor  = Color(0xFF1B5E20)  // green

@Composable
private fun ShipmentStatusRow(
    expected: Int,
    arrived: Int,
    cleared: Int,
    upcomingArrivals: Int,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SummaryCard(
                label    = "متوقع وصولها",
                value    = expected.toString(),
                color    = ShipmentExpectedColor,
                modifier = Modifier.weight(1f),
            )
            SummaryCard(
                label    = "وصلت",
                value    = arrived.toString(),
                color    = ShipmentArrivedColor,
                modifier = Modifier.weight(1f),
            )
            SummaryCard(
                label    = "تم التخليص",
                value    = cleared.toString(),
                color    = ShipmentClearedColor,
                modifier = Modifier.weight(1f),
            )
        }
        if (upcomingArrivals > 0) {
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(10.dp),
                colors    = CardDefaults.cardColors(
                    containerColor = ShipmentArrivedColor.copy(alpha = 0.10f),
                ),
                elevation = CardDefaults.cardElevation(0.dp),
            ) {
                Text(
                    text      = "⏰  $upcomingArrivals شحنة متوقع وصولها خلال 7 أيام القادمة",
                    style     = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color     = ShipmentArrivedColor,
                    modifier  = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }
    }
}

// ── Phase distribution chart ──────────────────────────────────────────────────

@Composable
private fun PhaseDistributionChart(
    distribution: List<Pair<String, Int>>,
    modifier: Modifier = Modifier,
) {
    val maxCount = distribution.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1
    val barColor = MaterialTheme.colorScheme.primary

    Card(
        modifier  = modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            distribution.forEach { (label, count) ->
                val fraction = count.toFloat() / maxCount.toFloat()
                HorizontalBarRow(
                    label    = label.take(10),
                    fraction = fraction,
                    count    = count,
                    barColor = barColor,
                    suffix   = count.toString(),
                )
            }
        }
    }
}

// ── Value by Division chart ───────────────────────────────────────────────────

@Composable
private fun ValueByDivisionChart(
    entries: List<DivisionValueEntry>,
    modifier: Modifier = Modifier,
) {
    val maxValue = entries.maxOfOrNull { it.totalValue }?.coerceAtLeast(1.0) ?: 1.0
    val barColor = Color(0xFF1565C0)

    Card(
        modifier  = modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            entries.forEach { entry ->
                if (entry.count == 0 && entry.totalValue == 0.0) return@forEach
                val fraction = (entry.totalValue / maxValue).toFloat()
                HorizontalBarRow(
                    label    = entry.labelAr.replace("شعبة ", "").take(12),
                    fraction = fraction.coerceAtLeast(if (entry.count > 0) 0.02f else 0f),
                    count    = entry.count,
                    barColor = barColor,
                    suffix   = formatValue(entry.totalValue),
                )
            }
            HorizontalDivider(Modifier.padding(top = 4.dp))
            Text(
                text  = "الإجمالي الكلي: ${formatValue(entries.sumOf { it.totalValue })} JOD",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun formatValue(value: Double): String = when {
    value >= 1_000_000 -> "${"%.1f".format(value / 1_000_000)} م"
    value >= 1_000     -> "${"%.1f".format(value / 1_000)} أ"
    value > 0          -> "%.0f".format(value)
    else               -> "—"
}

// ── Shared horizontal bar row ─────────────────────────────────────────────────

@Composable
private fun HorizontalBarRow(
    label: String,
    fraction: Float,
    count: Int,
    barColor: Color,
    suffix: String,
) {
    Row(
        modifier          = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text     = label,
            style    = MaterialTheme.typography.labelSmall,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(80.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(18.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            if (fraction > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .height(18.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(barColor),
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text       = suffix,
            style      = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color      = if (count > 0) MaterialTheme.colorScheme.onSurface
                         else MaterialTheme.colorScheme.outline,
            modifier   = Modifier.width(44.dp),
        )
    }
}

// ── KPI grid ─────────────────────────────────────────────────────────────────

@Composable
private fun KpiGrid(
    stats: DashboardStats,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier  = modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            KpiRow(
                label = "متوسط وقت إنجاز المعاملة الكلي",
                value = stats.avgTotalDays?.let { "${it.roundToInt()} يوم" } ?: "—",
            )
            HorizontalDivider(Modifier.padding(vertical = 6.dp))
            // General Command KPI — highlighted as most important for RMS
            KpiRow(
                label      = "متوسط إنجاز القيادة العامة (4.1)",
                value      = stats.avgMilitaryDays?.let { "${it.roundToInt()} يوم" } ?: "—",
                highlight  = true,
                valueColor = stats.avgMilitaryDays?.let { days ->
                    when {
                        days > 14 -> CustomsColors.Overdue
                        days > 7  -> CustomsColors.Warning
                        else      -> CustomsColors.OnTime
                    }
                },
            )
            HorizontalDivider(Modifier.padding(vertical = 6.dp))
            KpiRow(
                label = "متوسط التخليص الجمركي (4.2)",
                value = stats.avgCustomsDays?.let { "${it.roundToInt()} يوم" } ?: "—",
            )
            HorizontalDivider(Modifier.padding(vertical = 6.dp))
            KpiRow(
                label      = "نسبة المعاملات المتأخرة",
                value      = "${stats.delayedRatioPct.roundToInt()}%",
                valueColor = when {
                    stats.delayedRatioPct >= 50 -> CustomsColors.Overdue
                    stats.delayedRatioPct >= 25 -> CustomsColors.Warning
                    else                        -> CustomsColors.OnTime
                },
            )
        }
    }
}

@Composable
private fun KpiRow(
    label: String,
    value: String,
    highlight: Boolean = false,
    valueColor: Color? = null,
) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        Text(
            text       = label,
            style      = MaterialTheme.typography.bodySmall,
            color      = if (highlight) CustomsColors.Military
                         else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (highlight) FontWeight.Medium else FontWeight.Normal,
            modifier   = Modifier.weight(1f).padding(end = 8.dp),
        )
        Text(
            text       = value,
            style      = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color      = valueColor ?: MaterialTheme.colorScheme.onSurface,
        )
    }
}

// ── Overdue row ───────────────────────────────────────────────────────────────

@Composable
private fun OverdueRow(
    item: OverdueItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isUrgent = item.overdueDays >= 7
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector        = Icons.Default.Warning,
            contentDescription = null,
            tint               = if (isUrgent) CustomsColors.Overdue else CustomsColors.Warning,
            modifier           = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = item.transactionRef,
                style      = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text     = "${item.supplierName} — ${item.phaseLabel}",
                style    = MaterialTheme.typography.bodySmall,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text       = "تأخر ${item.overdueDays} يوم",
            style      = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color      = if (isUrgent) CustomsColors.Overdue else CustomsColors.Warning,
        )
    }
}
