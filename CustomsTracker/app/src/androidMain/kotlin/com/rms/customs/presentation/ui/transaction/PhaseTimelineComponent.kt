package com.rms.customs.presentation.ui.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rms.customs.domain.model.Transaction
import com.rms.customs.domain.model.enums.TransactionPhase
import com.rms.customs.domain.model.enums.toPhase
import com.rms.customs.presentation.ui.theme.CustomsColors

// ── Timeline status ───────────────────────────────────────────────────────────

private enum class TlStatus { DONE, IN_PROGRESS, PENDING }

private fun TransactionPhase.resolveStatus(tx: Transaction): TlStatus {
    val txPhase = tx.currentStatus.toPhase()
    return when {
        number < txPhase.number  -> TlStatus.DONE
        number == txPhase.number -> TlStatus.IN_PROGRESS
        else                     -> TlStatus.PENDING
    }
}

// ── Node sizes ────────────────────────────────────────────────────────────────

private val NodeSizeDone     = 22.dp
private val NodeSizeActive   = 26.dp
private val NodeSizePending  = 16.dp
private val ConnectorWidth   = 2.dp
private val ColumnWidth      = 36.dp

// ── Main composable ───────────────────────────────────────────────────────────

@Composable
fun PhaseTimeline(
    transaction: Transaction,
    modifier: Modifier = Modifier,
) {
    val phases = TransactionPhase.entries

    Column(modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        phases.forEachIndexed { index, phase ->
            val tlStatus  = phase.resolveStatus(transaction)
            val isLast    = index == phases.size - 1

            val connectorColor = when (tlStatus) {
                TlStatus.DONE        -> CustomsColors.OnTime
                TlStatus.IN_PROGRESS -> MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                TlStatus.PENDING     -> MaterialTheme.colorScheme.outlineVariant
            }

            // Highlight row for active phase
            val rowBackground = if (tlStatus == TlStatus.IN_PROGRESS)
                MaterialTheme.colorScheme.primary.copy(alpha = 0.04f)
            else Color.Transparent

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(rowBackground, RoundedCornerShape(12.dp)),
                verticalAlignment = Alignment.Top,
            ) {
                // ── Left column: node + connector ────────────────────────────
                Column(
                    modifier            = Modifier.width(ColumnWidth),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(Modifier.height(2.dp))
                    PhaseNode(status = tlStatus)
                    if (!isLast) {
                        Box(
                            modifier = Modifier
                                .width(ConnectorWidth)
                                .height(24.dp)
                                .background(
                                    color = connectorColor,
                                    shape = RoundedCornerShape(1.dp),
                                ),
                        )
                    }
                }

                // ── Right column: label + badge ──────────────────────────────
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 6.dp, bottom = 10.dp, top = 2.dp, end = 8.dp),
                ) {
                    Row(
                        modifier          = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text       = "المرحلة ${phase.number}: ${phase.labelAr}",
                            style      = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (tlStatus == TlStatus.IN_PROGRESS) FontWeight.Bold
                                         else FontWeight.Normal,
                            color      = when (tlStatus) {
                                TlStatus.DONE        -> MaterialTheme.colorScheme.onSurface
                                TlStatus.IN_PROGRESS -> MaterialTheme.colorScheme.onSurface
                                TlStatus.PENDING     -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier   = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(6.dp))
                        StatusBadge(status = tlStatus)
                    }
                }
            }
        }
    }
}

// ── Phase node ────────────────────────────────────────────────────────────────

@Composable
private fun PhaseNode(status: TlStatus) {
    when (status) {
        TlStatus.DONE -> {
            Box(
                modifier         = Modifier
                    .size(NodeSizeDone)
                    .background(CustomsColors.OnTime, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector        = Icons.Default.Check,
                    contentDescription = null,
                    tint               = Color.White,
                    modifier           = Modifier.size(13.dp),
                )
            }
        }

        TlStatus.IN_PROGRESS -> {
            // Outer glow ring + inner filled circle
            Box(
                modifier         = Modifier.size(NodeSizeActive + 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(NodeSizeActive + 6.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            shape = CircleShape,
                        )
                )
                Box(
                    modifier = Modifier
                        .size(NodeSizeActive)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                )
            }
        }

        TlStatus.PENDING -> {
            Box(
                modifier = Modifier
                    .size(NodeSizePending)
                    .border(1.5.dp, Color(0xFFBDBDBD), CircleShape),
            )
        }
    }
}

// ── Status badge ──────────────────────────────────────────────────────────────

@Composable
private fun StatusBadge(status: TlStatus) {
    val bgColor = when (status) {
        TlStatus.DONE        -> CustomsColors.OnTime.copy(alpha = 0.12f)
        TlStatus.IN_PROGRESS -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        TlStatus.PENDING     -> Color(0xFFBDBDBD).copy(alpha = 0.18f)
    }
    val fgColor = when (status) {
        TlStatus.DONE        -> CustomsColors.OnTime
        TlStatus.IN_PROGRESS -> MaterialTheme.colorScheme.primary
        TlStatus.PENDING     -> Color(0xFF9E9E9E)
    }
    val label = when (status) {
        TlStatus.DONE        -> "مكتملة"
        TlStatus.IN_PROGRESS -> "جارية"
        TlStatus.PENDING     -> "معلقة"
    }
    Box(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp),
    ) {
        Text(
            text       = label,
            style      = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color      = fgColor,
        )
    }
}
