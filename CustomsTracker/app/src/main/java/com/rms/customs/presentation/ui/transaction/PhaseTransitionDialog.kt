package com.rms.customs.presentation.ui.transaction

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rms.customs.domain.model.enums.TransactionStatus

@Composable
fun PhaseTransitionDialog(
    fromStatus: TransactionStatus,
    toStatus: TransactionStatus,
    isLoading: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title   = { Text("تأكيد تقديم المرحلة") },
        text    = {
            Column {
                Text("سيتم تغيير حالة المعاملة من:")
                Spacer(Modifier.height(4.dp))
                Text(
                    text  = fromStatus.labelAr(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = fromStatus.statusColor(),
                )
                Text("إلى:")
                Text(
                    text  = toStatus.labelAr(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = toStatus.statusColor(),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text  = "لا يمكن التراجع عن هذا الإجراء.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(8.dp))
            } else {
                TextButton(onClick = onConfirm) { Text("تأكيد") }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) { Text("إلغاء") }
        },
    )
}

@Composable
fun BlockerDialog(
    isLoading: Boolean,
    onConfirm: (reason: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var reason by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title   = { Text("الإبلاغ عن عائق") },
        text    = {
            Column(horizontalAlignment = Alignment.Start) {
                Text("أدخل سبب الحجب أو العائق الذي يمنع التقدم:")
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value         = reason,
                    onValueChange = { reason = it },
                    label         = { Text("السبب *") },
                    modifier      = Modifier.fillMaxWidth().height(100.dp),
                    maxLines      = 4,
                )
            }
        },
        confirmButton = {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(8.dp))
            } else {
                TextButton(
                    onClick  = { if (reason.isNotBlank()) onConfirm(reason) },
                    enabled  = reason.isNotBlank(),
                ) { Text("إبلاغ") }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) { Text("إلغاء") }
        },
    )
}
