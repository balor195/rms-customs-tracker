package com.rms.customs.presentation.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rms.customs.domain.model.SlaConfig
import com.rms.customs.presentation.viewmodel.SlaAdminViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SlaAdminScreen(
    onBack: () -> Unit,
    viewModel: SlaAdminViewModel = hiltViewModel(),
) {
    val configs by viewModel.configs.collectAsStateWithLifecycle()
    var editing  by remember { mutableStateOf<SlaConfig?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("إعدادات أهداف الوقت (SLA)") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor            = MaterialTheme.colorScheme.primary,
                    titleContentColor         = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Text(
                text     = "اضغط على أي مرحلة لتعديل هدفها الزمني",
                style    = MaterialTheme.typography.bodySmall,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            HorizontalDivider()

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(configs, key = { it.id.toString() }) { config ->
                    SlaConfigRow(
                        config       = config,
                        onEdit       = { editing = config },
                        onToggleActive = { viewModel.setActive(config.id, !config.isActive) },
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }

    editing?.let { config ->
        SlaEditDialog(
            config    = config,
            onSave    = { updated ->
                viewModel.save(updated)
                editing = null
            },
            onDismiss = { editing = null },
        )
    }
}

@Composable
private fun SlaConfigRow(
    config: SlaConfig,
    onEdit: () -> Unit,
    onToggleActive: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text  = subPhaseNameAr(config.phaseNumber, config.subPhase),
                style = MaterialTheme.typography.bodyMedium,
                color = if (config.isActive) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.outline,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text  = "الهدف: ${config.targetDays} يوم  |  التصعيد: ${config.escalationAfterDays} يوم",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        TextButton(onClick = onEdit) {
            Text("تعديل", style = MaterialTheme.typography.labelMedium)
        }

        Spacer(Modifier.width(8.dp))

        Switch(
            checked  = config.isActive,
            onCheckedChange = { onToggleActive() },
        )
    }
}

@Composable
private fun SlaEditDialog(
    config: SlaConfig,
    onSave: (SlaConfig) -> Unit,
    onDismiss: () -> Unit,
) {
    var targetText     by remember { mutableStateOf(config.targetDays.toString()) }
    var escalationText by remember { mutableStateOf(config.escalationAfterDays.toString()) }
    val targetError    = targetText.toIntOrNull() == null || (targetText.toIntOrNull() ?: 0) < 1
    val escalError     = escalationText.toIntOrNull() == null ||
                         (escalationText.toIntOrNull() ?: 0) <= (targetText.toIntOrNull() ?: 0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text  = subPhaseNameAr(config.phaseNumber, config.subPhase),
                style = MaterialTheme.typography.titleMedium,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value         = targetText,
                    onValueChange = { targetText = it },
                    label         = { Text("هدف الأيام") },
                    isError       = targetError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine    = true,
                    supportingText = if (targetError) {{ Text("أدخل رقماً صحيحاً موجباً") }} else null,
                )
                OutlinedTextField(
                    value         = escalationText,
                    onValueChange = { escalationText = it },
                    label         = { Text("أيام التصعيد") },
                    isError       = escalError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine    = true,
                    supportingText = if (escalError) {{ Text("يجب أن يكون أكبر من هدف الأيام") }} else null,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val target = targetText.toIntOrNull() ?: return@TextButton
                    val escal  = escalationText.toIntOrNull() ?: return@TextButton
                    if (target < 1 || escal <= target) return@TextButton
                    onSave(config.copy(targetDays = target, escalationAfterDays = escal))
                },
                enabled = !targetError && !escalError,
            ) {
                Text("حفظ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        },
    )
}

private fun subPhaseNameAr(phase: Int, sub: String): String = when (sub) {
    "1.1" -> "المرحلة 1 — تحضير المناقصة واعتماد الوثيقة"
    "1.2" -> "المرحلة 1 — إصدار المناقصة والإعلان"
    "2.1" -> "المرحلة 2 — طلب تخليص"
    "3.1" -> "المرحلة 3 — إغلاق المعاملة والتسوية المالية للمورد"
    else  -> "المرحلة $phase — $sub"
}
