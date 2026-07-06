package com.rms.customs.presentation.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.rms.customs.presentation.ui.ShimmerBox
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rms.customs.presentation.ui.LocalUserSession
import com.rms.customs.presentation.ui.theme.CustomsColors
import com.rms.customs.presentation.viewmodel.DashboardStats
import com.rms.customs.presentation.viewmodel.DashboardViewModel
import com.rms.customs.presentation.viewmodel.DivisionValueEntry
import org.koin.androidx.compose.koinViewModel
import kotlin.math.roundToInt

@Composable
fun DashboardScreen(
    onTransactionClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = koinViewModel(),
) {
    val stats   by viewModel.stats.collectAsStateWithLifecycle()
    val session = LocalUserSession.current

    LaunchedEffect(session?.user) {
        session?.user?.let { viewModel.setCurrentUser(it) }
    }

    if (!stats.isLoaded) {
        DashboardSkeleton(modifier)
        return
    }

    LazyColumn(
        modifier            = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        // ── Summary cards ────────────────────────────────────────────────────
        item { SectionHeader("ملخص المعاملات") }
        item {
            Row(
                modifier              = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SummaryCard(
                    label       = "نشطة",
                    value       = stats.totalActive.toString(),
                    accentColor = MaterialTheme.colorScheme.primary,
                    icon        = Icons.Default.Assignment,
                    modifier    = Modifier.weight(1f),
                )
                SummaryCard(
                    label       = "مغلقة هذا الشهر",
                    value       = stats.closedThisMonth.toString(),
                    accentColor = CustomsColors.OnTime,
                    icon        = Icons.Default.CheckCircle,
                    modifier    = Modifier.weight(1f),
                )
            }
        }

        // ── Upcoming arrivals ─────────────────────────────────────────────────
        if (stats.upcomingArrivalsCount > 0) {
            item { SectionHeader("الشحنات القادمة") }
            item {
                UpcomingArrivalsBanner(
                    count    = stats.upcomingArrivalsCount,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }

        // ── Phase distribution chart ─────────────────────────────────────────
        item { SectionHeader("توزيع المعاملات حسب المرحلة") }
        item {
            PhaseDistributionChart(
                distribution = stats.phaseDistribution,
                modifier     = Modifier.padding(horizontal = 16.dp),
            )
        }

        // ── Value by Division chart ──────────────────────────────────────────
        if (stats.valueByDivision.any { it.count > 0 }) {
            item { SectionHeader("القيمة حسب الشعبة الطبية") }
            item {
                ValueByDivisionChart(
                    entries  = stats.valueByDivision,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }

        // ── KPIs ─────────────────────────────────────────────────────────────
        item { SectionHeader("مؤشرات الأداء") }
        item {
            KpiGrid(
                stats    = stats,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        item { Spacer(Modifier.height(32.dp)) }
    }
}

// ── Dashboard skeleton (shown while data loads) ───────────────────────────────

@Composable
private fun DashboardSkeleton(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier            = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding      = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Section header placeholder
        item {
            ShimmerBox(Modifier.width(140.dp).height(14.dp).clip(RoundedCornerShape(4.dp)))
        }
        // Summary cards row
        item {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                repeat(3) {
                    ShimmerBox(Modifier.weight(1f).height(80.dp).clip(RoundedCornerShape(16.dp)))
                }
            }
        }
        // Section header placeholder
        item {
            Spacer(Modifier.height(8.dp))
            ShimmerBox(Modifier.width(120.dp).height(14.dp).clip(RoundedCornerShape(4.dp)))
        }
        // Chart area placeholder
        item {
            ShimmerBox(Modifier.fillMaxWidth().height(72.dp).clip(RoundedCornerShape(12.dp)))
        }
        // Section header placeholder
        item {
            Spacer(Modifier.height(8.dp))
            ShimmerBox(Modifier.width(100.dp).height(14.dp).clip(RoundedCornerShape(4.dp)))
        }
        // KPI row placeholders
        items(4) {
            ShimmerBox(Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(8.dp)))
        }
    }
}

// ── Section header ────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Row(
        modifier          = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(16.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(2.dp),
                )
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text       = title,
            style      = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color      = MaterialTheme.colorScheme.onBackground,
        )
    }
}

// ── Summary card ──────────────────────────────────────────────────────────────

@Composable
private fun SummaryCard(
    label: String,
    value: String,
    accentColor: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier  = modifier,
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(
            containerColor = accentColor.copy(alpha = 0.08f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .border(1.dp, accentColor.copy(alpha = 0.18f), RoundedCornerShape(16.dp))
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector        = icon,
                contentDescription = null,
                tint               = accentColor,
                modifier           = Modifier.size(20.dp),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text       = value,
                style      = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color      = accentColor,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text      = label,
                style     = MaterialTheme.typography.labelSmall,
                color     = accentColor.copy(alpha = 0.75f),
                maxLines  = 2,
                overflow  = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// ── Upcoming arrivals banner ───────────────────────────────────────────────────

private val UpcomingArrivalColor = Color(0xFF1565C0)

@Composable
private fun UpcomingArrivalsBanner(count: Int, modifier: Modifier = Modifier) {
    Card(
        modifier  = modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(10.dp),
        colors    = CardDefaults.cardColors(
            containerColor = UpcomingArrivalColor.copy(alpha = 0.08f),
        ),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .border(1.dp, UpcomingArrivalColor.copy(alpha = 0.20f), RoundedCornerShape(10.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector        = Icons.Default.DateRange,
                contentDescription = null,
                tint               = UpcomingArrivalColor,
                modifier           = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text       = "$count شحنة متوقع وصولها خلال 7 أيام القادمة",
                style      = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color      = UpcomingArrivalColor,
            )
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
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            distribution.forEach { (label, count) ->
                val fraction = count.toFloat() / maxCount.toFloat()
                HorizontalBarRow(
                    label    = label.take(12),
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
    val barColor = MaterialTheme.colorScheme.secondary

    Card(
        modifier  = modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
            HorizontalDivider(
                modifier = Modifier.padding(top = 4.dp),
                color    = MaterialTheme.colorScheme.surfaceVariant,
            )
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
            modifier = Modifier.width(84.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(22.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(barColor.copy(alpha = 0.10f)),
        ) {
            if (fraction > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .height(22.dp)
                        .clip(RoundedCornerShape(6.dp))
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
            modifier   = Modifier.width(48.dp),
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
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            KpiRow(
                label = "متوسط وقت إنجاز المعاملة الكلي",
                value = stats.avgTotalDays?.let { "${it.roundToInt()} يوم" } ?: "—",
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
        modifier              = Modifier
            .fillMaxWidth()
            .then(
                if (highlight) Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                else Modifier
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        Row(
            modifier          = Modifier.weight(1f).padding(end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (highlight) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(14.dp)
                        .background(
                            color = CustomsColors.Military,
                            shape = RoundedCornerShape(2.dp),
                        )
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text       = label,
                style      = MaterialTheme.typography.bodySmall,
                color      = if (highlight) CustomsColors.Military
                             else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (highlight) FontWeight.SemiBold else FontWeight.Normal,
            )
        }
        Text(
            text       = value,
            style      = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color      = valueColor ?: MaterialTheme.colorScheme.onSurface,
        )
    }
}
