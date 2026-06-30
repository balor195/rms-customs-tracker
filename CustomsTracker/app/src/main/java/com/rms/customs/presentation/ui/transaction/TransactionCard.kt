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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rms.customs.domain.model.Transaction
import com.rms.customs.domain.model.enums.Priority
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

    Card(
        modifier  = modifier.fillMaxWidth().clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            // SLA status bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(88.dp)
                    .background(slaColor, RoundedCornerShape(2.dp))
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {

                // Row 1: ref + priority chip
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text       = transaction.transactionRef,
                        style      = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier   = Modifier.weight(1f),
                        color      = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (transaction.priority != Priority.NORMAL) {
                        SuggestionChip(
                            onClick = {},
                            label   = {
                                Text(
                                    transaction.priority.labelAr,
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            },
                        )
                    }
                }

                // Row 2: accreditation number (primary business ID)
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
                )

                // Row 4: officer name (if set)
                if (transaction.responsibleOfficer.isNotBlank()) {
                    Text(
                        text  = transaction.responsibleOfficer,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(Modifier.height(4.dp))

                // Row 5: status chip + division chip + elapsed days
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    AssistChip(
                        onClick = {},
                        label   = {
                            Text(
                                displayStatus.labelAr(),
                                style = MaterialTheme.typography.labelSmall,
                            )
                        },
                        colors  = AssistChipDefaults.assistChipColors(
                            containerColor = displayStatus.statusColor().copy(alpha = 0.12f),
                            labelColor     = displayStatus.statusColor(),
                        ),
                    )
                    transaction.division?.let { div ->
                        SuggestionChip(
                            onClick = {},
                            label   = {
                                Text(div.labelAr, style = MaterialTheme.typography.labelSmall)
                            },
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        text  = "منذ ${transaction.daysSinceUpdate} يوم",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
