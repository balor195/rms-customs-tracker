@file:OptIn(ExperimentalMaterial3Api::class)

package com.rms.customs.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rms.customs.data.MockData
import com.rms.customs.data.Transaction
import com.rms.customs.ui.theme.*

@Composable
fun DashboardScreen(onTransactionClick: (String) -> Unit = {}) {
    val sorted = MockData.transactions.sortedByDescending { it.daysOverdue }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorSurface),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { DashboardHeader() }
        item { SummaryRow() }
        item { SectionLabel("توزيع المعاملات حسب المرحلة") }
        item { PhaseDistributionCard() }
        item { SectionLabel("المعاملات — مرتبة حسب التأخر") }
        items(sorted) { tx ->
            TransactionRow(tx, onClick = { onTransactionClick(tx.ref) })
        }
    }
}

// ─── Header ──────────────────────────────────────────────────────────────────

@Composable
private fun DashboardHeader() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "لوحة متابعة التخليص الجمركي",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.End
        )
        Text(
            text = "مديرية الصيدلة والتجهيزات الطبية — الخدمات الطبية الملكية",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.End
        )
    }
}

// ─── Summary cards ───────────────────────────────────────────────────────────

@Composable
private fun SummaryRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SummaryCard(
            modifier   = Modifier.weight(1f),
            value      = MockData.totalActive.toString(),
            label      = "معاملة نشطة",
            valueColor = MaterialTheme.colorScheme.primary
        )
        SummaryCard(
            modifier   = Modifier.weight(1f),
            value      = MockData.totalDelayed.toString(),
            label      = "متأخرة عن SLA",
            valueColor = ColorDelayed
        )
        SummaryCard(
            modifier   = Modifier.weight(1f),
            value      = MockData.closedThisMonth.toString(),
            label      = "مغلقة هذا الشهر",
            valueColor = ColorOk
        )
    }
}

@Composable
private fun SummaryCard(
    modifier: Modifier,
    value: String,
    label: String,
    valueColor: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = valueColor.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ─── Phase distribution ───────────────────────────────────────────────────────

@Composable
private fun PhaseDistributionCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MockData.phaseDistribution.forEach { (phase, count) ->
                PhaseRow(label = phase, count = count, total = MockData.totalActive)
            }
        }
    }
}

@Composable
private fun PhaseRow(label: String, count: Int, total: Int) {
    val fraction = (count.toFloat() / total.coerceAtLeast(1)).coerceIn(0f, 1f)
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(modifier = Modifier.height(2.dp))
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

// ─── Transaction row ─────────────────────────────────────────────────────────

@Composable
private fun TransactionRow(tx: Transaction, onClick: () -> Unit) {
    val urgencyColor = when {
        tx.daysOverdue > 7 -> ColorDelayed
        tx.daysOverdue > 0 -> ColorWarning
        else               -> ColorOk
    }
    val urgencyEmoji = when {
        tx.daysOverdue > 7 -> "🔴"
        tx.daysOverdue > 0 -> "🟡"
        else               -> "🟢"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side: day counter
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${tx.daysElapsed}",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = urgencyColor
                )
                Text(text = "يوم", style = MaterialTheme.typography.labelSmall, color = urgencyColor)
                if (tx.isDelayed) {
                    Text(
                        text = "+${tx.daysOverdue}",
                        style = MaterialTheme.typography.labelSmall,
                        color = ColorDelayed,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Right side: details
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End
            ) {
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tx.ref,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = urgencyEmoji)
                }
                Text(
                    text = tx.supplierName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.End
                )
                Text(
                    text = tx.currentPhaseName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ─── Utility ─────────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.End
    )
}

// ─── Preview ─────────────────────────────────────────────────────────────────

@Preview(showBackground = true, widthDp = 400, heightDp = 800, locale = "ar")
@Composable
fun DashboardPreview() {
    com.rms.customs.ui.theme.CustomsTheme {
        DashboardScreen()
    }
}
