package com.rms.customs.presentation.ui.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rms.customs.domain.model.PhaseRecord
import com.rms.customs.domain.model.Transaction
import com.rms.customs.domain.model.enums.PhaseStatus
import com.rms.customs.domain.model.enums.TransactionPhase
import com.rms.customs.domain.model.enums.TransactionStatus
import com.rms.customs.domain.model.enums.toPhase
import com.rms.customs.presentation.ui.theme.CustomsColors

private enum class TlStatus { DONE, IN_PROGRESS, PENDING }

private fun TransactionPhase.resolveStatus(tx: Transaction): TlStatus {
    val txPhase = tx.currentStatus.toPhase()
    return when {
        // GOV_APPROVED finalises Phase 4 before currentPhase updates to Phase 5
        this == TransactionPhase.PHASE_4_GOV_AGENCIES
                && tx.currentStatus == TransactionStatus.GOV_APPROVED -> TlStatus.DONE
        number < txPhase.number  -> TlStatus.DONE
        number == txPhase.number -> TlStatus.IN_PROGRESS
        else                     -> TlStatus.PENDING
    }
}

private fun TlStatus.dotColor(): Color = when (this) {
    TlStatus.DONE        -> CustomsColors.OnTime
    TlStatus.IN_PROGRESS -> CustomsColors.Military
    TlStatus.PENDING     -> Color(0xFFBDBDBD)
}

@Composable
fun PhaseTimeline(
    transaction: Transaction,
    phase4Records: List<PhaseRecord>,
    onTrack4Click: ((PhaseRecord) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val phases = TransactionPhase.entries

    Column(modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        phases.forEachIndexed { index, phase ->
            val tlStatus = phase.resolveStatus(transaction)
            val showTracks = phase == TransactionPhase.PHASE_4_GOV_AGENCIES
                    && phase4Records.isNotEmpty()
                    && tlStatus == TlStatus.IN_PROGRESS

            Row(verticalAlignment = Alignment.Top) {
                // Left: dot + connector
                Column(
                    modifier            = Modifier.width(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .size(if (tlStatus == TlStatus.IN_PROGRESS) 14.dp else 10.dp)
                            .background(tlStatus.dotColor(), CircleShape),
                    )
                    if (index < phases.size - 1) {
                        val connectorHeight =
                            if (showTracks) (phase4Records.size * 44 + 8).dp else 20.dp
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(connectorHeight)
                                .background(MaterialTheme.colorScheme.outlineVariant),
                        )
                    }
                }

                Spacer(Modifier.width(10.dp))

                // Right: phase label + optional tracks
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(bottom = 8.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text  = "المرحلة ${phase.number}: ${phase.labelAr}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (tlStatus == TlStatus.IN_PROGRESS) FontWeight.Bold else FontWeight.Normal,
                            color = if (tlStatus == TlStatus.PENDING)
                                MaterialTheme.colorScheme.onSurfaceVariant
                            else
                                MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text  = when (tlStatus) {
                                TlStatus.DONE        -> "✓ مكتملة"
                                TlStatus.IN_PROGRESS -> "⟳ جارية"
                                TlStatus.PENDING     -> "○ معلقة"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = tlStatus.dotColor(),
                        )
                    }

                    if (showTracks) {
                        Spacer(Modifier.height(6.dp))
                        phase4Records.sortedBy { it.subPhase }.forEach { track ->
                            TrackRow(track = track, onClick = onTrack4Click?.let { cb -> { cb(track) } })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackRow(track: PhaseRecord, onClick: (() -> Unit)?) {
    val trackColor: Color = when {
        track.status == PhaseStatus.DONE    -> CustomsColors.OnTime
        track.status == PhaseStatus.BLOCKED -> CustomsColors.Overdue
        track.isOverSla                     -> CustomsColors.Overdue
        else                                -> CustomsColors.Military
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text  = when (track.status) {
                PhaseStatus.DONE    -> "✓"
                PhaseStatus.BLOCKED -> "✗"
                else                -> "⟳"
            },
            color      = trackColor,
            style      = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
        )

        Column(Modifier.weight(1f)) {
            Text(
                text  = track.assignedToEntity.labelAr,
                style = MaterialTheme.typography.bodySmall,
            )
            val days = track.daysElapsed
            val sla  = track.slaTargetDays
            val daysLabel = when {
                track.status == PhaseStatus.DONE     -> "مكتمل"
                days == null                         -> track.status.labelAr()
                track.isOverSla && sla != null       -> "متأخر ${days - sla} يوم عن الهدف ($days / $sla يوم)"
                sla != null                          -> "$days / $sla يوم"
                else                                 -> "$days يوم"
            }
            Text(daysLabel, style = MaterialTheme.typography.labelSmall, color = trackColor)
        }

        if (onClick != null && track.status != PhaseStatus.DONE) {
            Icon(
                imageVector        = Icons.Default.Edit,
                contentDescription = "تعديل",
                modifier           = Modifier.size(16.dp),
                tint               = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
