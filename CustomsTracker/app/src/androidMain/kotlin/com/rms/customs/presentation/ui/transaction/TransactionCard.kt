package com.rms.customs.presentation.ui.transaction

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rms.customs.domain.model.Transaction
import com.rms.customs.domain.model.enums.Priority
import com.rms.customs.domain.model.enums.TransactionPhase
import com.rms.customs.presentation.ui.theme.CustomsColors

@Composable
fun TransactionCard(
    transaction: Transaction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val slaColor: Color = when {
        transaction.isBlocked           -> CustomsColors.Overdue
        transaction.daysSinceUpdate > 7 -> CustomsColors.Warning
        else                            -> CustomsColors.OnTime
    }
    val displayStatus = transaction.exceptionState ?: transaction.currentStatus

    val cardContainerColor = when {
        transaction.isBlocked           -> CustomsColors.Overdue.copy(alpha = 0.04f)
        transaction.daysSinceUpdate > 7 -> CustomsColors.Warning.copy(alpha = 0.03f)
        else                            -> MaterialTheme.colorScheme.surface
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed         by interactionSource.collectIsPressedAsState()
    val scale             by animateFloatAsState(
        targetValue   = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessHigh,
        ),
        label         = "card_scale",
    )

    Card(
        modifier  = modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication        = LocalIndication.current,
                onClick           = onClick,
            ),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = cardContainerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            // ── Left accent bar (dynamic height) ─────────────────────────────
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(slaColor),
            )

            // ── Card content ──────────────────────────────────────────────────
            Column(
                modifier            = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {

                // Row 1: transaction ref + priority badge
                Row(
                    modifier          = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text       = transaction.transactionRef,
                        style      = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color      = MaterialTheme.colorScheme.onSurface,
                        modifier   = Modifier.weight(1f),
                    )
                    if (transaction.priority != Priority.NORMAL) {
                        Spacer(Modifier.width(8.dp))
                        PriorityBadge(priority = transaction.priority)
                    }
                }

                // Row 2: accreditation number (primary business identifier)
                if (transaction.accreditationNumber != null) {
                    Text(
                        text       = transaction.accreditationNumber,
                        style      = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color      = MaterialTheme.colorScheme.primary,
                    )
                }

                // Row 3: supplier name
                Text(
                    text  = transaction.supplierName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                // Row 4: responsible officer
                if (transaction.responsibleOfficer.isNotBlank()) {
                    Text(
                        text  = transaction.responsibleOfficer,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(Modifier.height(2.dp))

                // Row 5: status badge + division badge + days indicator
                Row(
                    modifier          = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    MiniChip(
                        label = displayStatus.labelAr(),
                        color = displayStatus.statusColor(),
                    )
                    transaction.division?.let { div ->
                        MiniChip(
                            label = div.labelAr.replace("شعبة ", ""),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            bgColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                    }
                    if (transaction.isRefrigerated) {
                        MiniChip(
                            label = "❄ مبرّدة",
                            color = Color(0xFF1565C0),
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    val daysColor = when {
                        transaction.daysSinceUpdate > 7 -> CustomsColors.Warning
                        transaction.daysSinceUpdate > 3 -> MaterialTheme.colorScheme.onSurfaceVariant
                        else                            -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Text(
                        text       = "منذ ${transaction.daysSinceUpdate} يوم",
                        style      = MaterialTheme.typography.labelSmall,
                        fontWeight = if (transaction.daysSinceUpdate > 7) FontWeight.SemiBold
                                     else FontWeight.Normal,
                        color      = daysColor,
                    )
                }

                Spacer(Modifier.height(4.dp))

                // Row 6: phase progress mini-bar
                PhaseProgressBar(
                    currentPhase = transaction.currentPhase,
                    modifier     = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

// ── Priority badge ─────────────────────────────────────────────────────────────

@Composable
private fun PriorityBadge(priority: Priority) {
    val bgColor = when (priority) {
        Priority.URGENT -> CustomsColors.Overdue.copy(alpha = 0.12f)
        Priority.HIGH   -> CustomsColors.Warning.copy(alpha = 0.12f)
        Priority.NORMAL -> MaterialTheme.colorScheme.surfaceVariant
    }
    val fgColor = when (priority) {
        Priority.URGENT -> CustomsColors.Overdue
        Priority.HIGH   -> CustomsColors.Warning
        Priority.NORMAL -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp),
    ) {
        Text(
            text       = priority.labelAr,
            style      = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color      = fgColor,
        )
    }
}

// ── Mini chip (status / division) ─────────────────────────────────────────────

@Composable
private fun MiniChip(
    label: String,
    color: Color,
    bgColor: Color = color.copy(alpha = 0.11f),
) {
    Box(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(5.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text       = label,
            style      = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color      = color,
        )
    }
}

// ── Phase progress mini-bar ───────────────────────────────────────────────────

@Composable
private fun PhaseProgressBar(
    currentPhase: TransactionPhase,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier            = modifier,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        TransactionPhase.entries.forEach { phase ->
            val segmentColor = when {
                phase.number < currentPhase.number  -> CustomsColors.OnTime
                phase.number == currentPhase.number -> MaterialTheme.colorScheme.primary
                else                                -> MaterialTheme.colorScheme.surfaceVariant
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .background(segmentColor, RoundedCornerShape(2.dp)),
            )
        }
    }
}
