package com.rms.customs.presentation.ui.transaction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rms.customs.domain.model.PhaseRecord
import com.rms.customs.domain.model.enums.PhaseStatus
import com.rms.customs.presentation.ui.theme.CustomsColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Phase4TrackSheet(
    track: PhaseRecord,
    canWrite: Boolean,
    onSaveNotes: (notes: String) -> Unit,
    onMarkComplete: () -> Unit,
    onBlockTrack: (reason: String) -> Unit,
    onActivate: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var notes          by rememberSaveable(track.id.toString()) { mutableStateOf(track.notes ?: "") }
    var showBlockInput by rememberSaveable { mutableStateOf(false) }
    var blockerReason  by rememberSaveable { mutableStateOf(track.blockerReason ?: "") }

    val isDone    = track.status == PhaseStatus.DONE
    val isBlocked = track.status == PhaseStatus.BLOCKED
    val isSkipped = track.status == PhaseStatus.SKIPPED
    val slaColor  = when {
        isDone              -> CustomsColors.OnTime
        track.isOverSla     -> CustomsColors.Overdue
        else                -> CustomsColors.Military
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Header
            Text(
                text       = track.assignedToEntity.labelAr,
                style      = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text  = "مسار ${track.subPhase}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // SLA / status row
            val days = track.daysElapsed
            val sla  = track.slaTargetDays
            val slaLabel = when {
                isDone              -> "مكتمل"
                isSkipped           -> "غير مطلوب لهذه المعاملة — اختياري"
                days == null        -> track.status.labelAr()
                track.isOverSla && sla != null -> "متأخر ${days - sla} يوم عن الهدف ($days / $sla يوم)"
                sla != null         -> "بدأت منذ $days يوم — الهدف: $sla يوم"
                else                -> "بدأت منذ $days يوم"
            }
            Text(
                slaLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isSkipped) MaterialTheme.colorScheme.onSurfaceVariant else slaColor,
            )

            if (isBlocked && track.blockerReason != null) {
                Text(
                    text  = "سبب الحجب: ${track.blockerReason}",
                    style = MaterialTheme.typography.bodySmall,
                    color = CustomsColors.Overdue,
                )
            }

            HorizontalDivider()

            if (isSkipped) {
                // Optional track — show activate button for authorized users
                Text(
                    text  = "هذا المسار اختياري ولا ينطبق افتراضياً على هذه المعاملة.\nإذا كانت المعاملة تتطلب موافقة وزارة الصحة، اضغط تفعيل لبدء المتابعة.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (canWrite) {
                    Spacer(Modifier.height(4.dp))
                    Button(
                        onClick  = onActivate,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("تفعيل مسار وزارة الصحة") }
                }
            } else {
                // Notes field
                OutlinedTextField(
                    value         = notes,
                    onValueChange = { notes = it },
                    label         = { Text("ملاحظات المسار") },
                    modifier      = Modifier.fillMaxWidth().height(120.dp),
                    maxLines      = 6,
                    readOnly      = !canWrite,
                )

                if (canWrite && !isDone) {
                    // Save notes
                    Button(
                        onClick  = { onSaveNotes(notes) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("حفظ الملاحظات") }

                    // Mark complete
                    Button(
                        onClick  = onMarkComplete,
                        modifier = Modifier.fillMaxWidth(),
                        colors   = ButtonDefaults.buttonColors(containerColor = CustomsColors.OnTime),
                    ) { Text("إتمام المسار ✓") }

                    // Block track
                    if (!showBlockInput) {
                        TextButton(
                            onClick  = { showBlockInput = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors   = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        ) { Text("إبلاغ عن عائق في هذا المسار") }
                    } else {
                        OutlinedTextField(
                            value         = blockerReason,
                            onValueChange = { blockerReason = it },
                            label         = { Text("سبب العائق *") },
                            modifier      = Modifier.fillMaxWidth(),
                            singleLine    = true,
                        )
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            TextButton(
                                onClick  = { showBlockInput = false; blockerReason = "" },
                                modifier = Modifier.weight(1f),
                            ) { Text("إلغاء") }
                            Button(
                                onClick  = { if (blockerReason.isNotBlank()) onBlockTrack(blockerReason) },
                                enabled  = blockerReason.isNotBlank(),
                                modifier = Modifier.weight(1f),
                                colors   = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            ) { Text("تأكيد الإبلاغ") }
                        }
                    }
                } else if (isDone) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text  = "✓  هذا المسار مكتمل",
                            style = MaterialTheme.typography.bodyLarge,
                            color = CustomsColors.OnTime,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}
