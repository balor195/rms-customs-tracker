package com.rms.customs.presentation.ui.transaction

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rms.customs.domain.model.ActivityLog
import com.rms.customs.domain.model.Transaction
import com.rms.customs.domain.model.enums.Department
import com.rms.customs.domain.model.enums.ShipmentStatus
import com.rms.customs.domain.model.enums.TransactionStatus
import com.rms.customs.domain.usecase.isVisibleTo
import com.rms.customs.presentation.ui.LocalUserSession
import com.rms.customs.presentation.ui.document.DocumentsTab
import com.rms.customs.presentation.ui.theme.CustomsColors
import com.rms.customs.presentation.viewmodel.TransactionDetailViewModel
import com.rms.customs.presentation.viewmodel.TransitionUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale as JavaLocale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailScreen(
    onBack: () -> Unit,
    viewModel: TransactionDetailViewModel = hiltViewModel(),
) {
    val transaction  by viewModel.transaction.collectAsStateWithLifecycle()
    val activityLog  by viewModel.activityLog.collectAsStateWithLifecycle()
    val transState   by viewModel.transitionState.collectAsStateWithLifecycle()
    val session      = LocalUserSession.current
    val snackbar     = remember { SnackbarHostState() }

    var selectedTab       by rememberSaveable { mutableIntStateOf(0) }
    var showAdvanceDialog by remember { mutableStateOf(false) }
    var showBlockerDialog by remember { mutableStateOf(false) }

    LaunchedEffect(transState) {
        when (val s = transState) {
            is TransitionUiState.Success -> {
                snackbar.showSnackbar("تم بنجاح")
                viewModel.resetTransitionState()
                showAdvanceDialog = false
                showBlockerDialog = false
            }
            is TransitionUiState.Error -> {
                snackbar.showSnackbar(s.message)
                viewModel.resetTransitionState()
            }
            else -> Unit
        }
    }

    val tx         = transaction
    val nextStatus = viewModel.nextForwardStatus()
    val role       = session?.user?.role
    val canWrite   = role?.canWrite == true
    val isClosed   = tx?.currentStatus?.isTerminal == true
    val isBlocked  = tx?.isBlocked == true
    val isVisible  = tx == null || session?.user?.let { tx.isVisibleTo(it) } != false
    val canAdvanceNext = when (nextStatus) {
        TransactionStatus.CLEARANCE_ISSUED          -> role?.canMarkClearanceDone == true
        TransactionStatus.TRANSFERRED_TO_WAREHOUSE  -> role?.canMarkWarehouseTransferred == true
        else                                        -> canWrite
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tx?.accreditationNumber ?: tx?.transactionRef ?: "…") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
            )
        },
        bottomBar = {
            if (tx != null && isVisible && !isClosed && (canWrite || canAdvanceNext)) {
                BottomAppBar {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (isBlocked) {
                            if (canWrite) {
                                Button(
                                    onClick  = { session?.let { viewModel.clearBlocker(it.user.id) } },
                                    enabled  = transState !is TransitionUiState.Loading,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    if (transState is TransitionUiState.Loading) {
                                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                    } else {
                                        Text("رفع الحجب")
                                    }
                                }
                            }
                        } else {
                            if (nextStatus == TransactionStatus.TRANSFERRED_TO_WAREHOUSE) {
                                if (canAdvanceNext) {
                                    Row(
                                        modifier          = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Checkbox(
                                            checked = false,
                                            onCheckedChange = { checked ->
                                                if (checked) session?.let {
                                                    viewModel.advanceStatus(nextStatus, it.user.id)
                                                }
                                            },
                                            enabled = transState !is TransitionUiState.Loading,
                                        )
                                        Text(nextStatus.labelAr())
                                    }
                                }
                            } else if (nextStatus != null && canAdvanceNext) {
                                Button(
                                    onClick  = { showAdvanceDialog = true },
                                    enabled  = transState !is TransitionUiState.Loading,
                                    modifier = Modifier.weight(1f),
                                ) { Text("تقديم للمرحلة التالية") }
                            }
                            if (canWrite) {
                                TextButton(
                                    onClick = { showBlockerDialog = true },
                                    colors  = ButtonDefaults.textButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error,
                                    ),
                                ) {
                                    Icon(Icons.Default.Warning, contentDescription = null,
                                        modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("إبلاغ عن عائق")
                                }
                            }
                        }
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { innerPadding ->
        if (tx == null) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        if (!isVisible) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text(
                    text  = "ليس لديك صلاحية لعرض هذه المعاملة",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            TransactionHeaderCard(tx)
            ShipmentStatusCard(
                tx             = tx,
                canWrite       = canWrite && !isClosed,
                canMarkCleared = role?.canMarkClearanceDone == true && !isClosed,
                onMarkArrived  = { viewModel.updateShipmentStatus(ShipmentStatus.ARRIVED) },
                onMarkCleared  = { viewModel.updateShipmentStatus(ShipmentStatus.CLEARED) },
            )
            PhaseTimeline(transaction = tx)

            TabRow(selectedTabIndex = selectedTab) {
                listOf("التفاصيل", "المستندات", "سجل النشاط", "ملاحظات").forEachIndexed { i, label ->
                    Tab(
                        selected = selectedTab == i,
                        onClick  = { selectedTab = i },
                        text     = { Text(label) },
                    )
                }
            }

            when (selectedTab) {
                0 -> DetailsTab(
                    tx                 = tx,
                    canWrite           = canWrite,
                    onSaveBillOfLading = { viewModel.updateBillOfLading(it) },
                )
                1 -> DocumentsTab(
                    currentPhaseNumber = tx.currentPhase.number,
                    canUpload          = canWrite,
                )
                2 -> ActivityLogTab(activityLog)
                3 -> NotesTab(tx, viewModel)
            }
        }
    }

    if (showAdvanceDialog && nextStatus != null && tx != null) {
        PhaseTransitionDialog(
            fromStatus = tx.exceptionState ?: tx.currentStatus,
            toStatus   = nextStatus,
            isLoading  = transState is TransitionUiState.Loading,
            onConfirm  = { session?.let { viewModel.advanceStatus(nextStatus, it.user.id) } },
            onDismiss  = { showAdvanceDialog = false },
        )
    }

    if (showBlockerDialog) {
        BlockerDialog(
            isLoading = transState is TransitionUiState.Loading,
            onConfirm = { reason -> session?.let { viewModel.setBlocker(reason, it.user.id) } },
            onDismiss = { showBlockerDialog = false },
        )
    }
}

// ── Header card ─────────────────────────────────────────────────────────────

@Composable
private fun TransactionHeaderCard(tx: Transaction) {
    val displayStatus = tx.exceptionState ?: tx.currentStatus
    Card(
        modifier  = Modifier.fillMaxWidth().padding(16.dp),
        elevation = CardDefaults.cardElevation(4.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {

            // Accreditation number + workflow status chip
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text  = "رقم الاعتماد",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text       = tx.accreditationNumber ?: tx.transactionRef,
                        style      = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color      = MaterialTheme.colorScheme.primary,
                    )
                }
                AssistChip(
                    onClick = {},
                    label   = { Text(displayStatus.labelAr()) },
                    colors  = AssistChipDefaults.assistChipColors(
                        containerColor = displayStatus.statusColor().copy(alpha = 0.15f),
                        labelColor     = displayStatus.statusColor(),
                    ),
                )
            }

            // Chips row: division + beneficiary
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                tx.division?.let { div ->
                    SuggestionChip(onClick = {}, label = { Text(div.labelAr, style = MaterialTheme.typography.labelSmall) })
                }
                tx.beneficiary?.let { ben ->
                    SuggestionChip(onClick = {}, label = { Text(ben.labelAr, style = MaterialTheme.typography.labelSmall) })
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Supplier + title
            Text(tx.supplierName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            if (tx.title.isNotBlank()) {
                Text(
                    text  = tx.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Officer
            if (tx.responsibleOfficer.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text  = "الضابط المسؤول: ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text  = tx.responsibleOfficer,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            // Tender value
            if (tx.totalValue != null) {
                Text(
                    text  = "قيمة العطاء: ${"%.2f".format(tx.totalValue)} ${tx.currency}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Phase + updated
            Text(
                text  = "المرحلة: ${tx.currentPhase.labelAr}",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text  = "آخر تحديث منذ ${tx.daysSinceUpdate} يوم",
                style = MaterialTheme.typography.bodySmall,
                color = if (tx.daysSinceUpdate > 7) CustomsColors.Warning
                        else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ── Shipment status card ─────────────────────────────────────────────────────

@Composable
private fun ShipmentStatusCard(
    tx: Transaction,
    canWrite: Boolean,
    canMarkCleared: Boolean,
    onMarkArrived: () -> Unit,
    onMarkCleared: () -> Unit,
) {
    val status = tx.shipmentStatus
    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors    = CardDefaults.cardColors(
            containerColor = status.statusColor().copy(alpha = 0.07f),
        ),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text       = "حالة الشحنة",
                style      = MaterialTheme.typography.labelMedium,
                color      = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Step indicator: 3 circles + connecting lines
            ShipmentStepIndicator(current = status)

            // Dates
            tx.expectedArrivalDate?.let {
                Text(
                    text  = "الوصول المتوقع: ${it.toDateString()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            tx.actualArrivalDate?.let {
                Text(
                    text  = "تاريخ الوصول الفعلي: ${it.toDateString()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = CustomsColors.OnTime,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            // Action button
            when (status) {
                ShipmentStatus.EXPECTED -> if (canWrite) {
                    Button(
                        onClick  = onMarkArrived,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("تسجيل وصول الشحنة") }
                }

                ShipmentStatus.ARRIVED -> if (canMarkCleared) {
                    Button(
                        onClick  = onMarkCleared,
                        modifier = Modifier.fillMaxWidth(),
                        colors   = ButtonDefaults.buttonColors(containerColor = CustomsColors.OnTime),
                    ) { Text("تأكيد اكتمال التخليص") }
                }

                ShipmentStatus.CLEARED -> Text(
                    text  = "✓  تم التخليص النهائي",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CustomsColors.OnTime,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun ShipmentStepIndicator(current: ShipmentStatus) {
    val steps = ShipmentStatus.entries
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        steps.forEachIndexed { index, step ->
            val reached = step.ordinal <= current.ordinal
            val color   = if (reached) step.statusColor() else Color(0xFFBDBDBD)

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f),
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(
                            color  = if (reached) color else Color.Transparent,
                            shape  = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (reached) {
                        Text("✓", style = MaterialTheme.typography.labelSmall, color = Color.White)
                    } else {
                        Box(
                            Modifier
                                .size(22.dp)
                                .background(Color(0xFFBDBDBD), CircleShape)
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text  = step.labelAr,
                    style = MaterialTheme.typography.labelSmall,
                    color = color,
                )
            }

            if (index < steps.size - 1) {
                Box(
                    Modifier
                        .weight(0.5f)
                        .height(2.dp)
                        .background(
                            if (step.ordinal < current.ordinal) current.statusColor()
                            else Color(0xFFBDBDBD)
                        )
                )
            }
        }
    }
}

// ── Details tab ──────────────────────────────────────────────────────────────

@Composable
private fun DetailsTab(
    tx: Transaction,
    canWrite: Boolean,
    onSaveBillOfLading: (String) -> Unit,
) {
    var editingBol       by rememberSaveable { mutableStateOf(false) }
    var bolDraft         by rememberSaveable(tx.billOfLadingNumber) {
        mutableStateOf(tx.billOfLadingNumber ?: "")
    }

    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {

        // ── Core identifiers ──────────────────────────────────────────────
        if (tx.accreditationNumber != null) {
            DetailRow("رقم الاعتماد", tx.accreditationNumber)
        }
        DetailRow("الرقم المرجعي الداخلي", tx.transactionRef)

        // ── Division & beneficiary ────────────────────────────────────────
        if (tx.division != null) DetailRow("الشعبة", tx.division.labelAr)
        if (tx.division == Department.MEDICAL_CONSUMABLES && tx.defaultShelfLife != null) {
            DetailRow("العمر الافتراضي", tx.defaultShelfLife)
        }
        if (tx.beneficiary != null) DetailRow("الجهة المستفيدة", tx.beneficiary.labelAr)

        // ── Personnel ─────────────────────────────────────────────────────
        if (tx.responsibleOfficer.isNotBlank()) {
            DetailRow("الضابط المسؤول", tx.responsibleOfficer)
        }

        // ── Supplier / contract ───────────────────────────────────────────
        DetailRow("اسم المورد", tx.supplierName)
        DetailRow("وصف المعاملة", tx.title)
        if (tx.tenderRef != null) DetailRow("رقم العطاء الداخلي", tx.tenderRef)
        if (tx.contractRef != null) DetailRow("رقم العقد", tx.contractRef)
        if (tx.totalValue != null) {
            DetailRow("قيمة العطاء", "${"%.2f".format(tx.totalValue)} ${tx.currency}")
        }

        // ── Shipment ──────────────────────────────────────────────────────
        DetailRow("حالة الشحنة", tx.shipmentStatus.labelAr)
        if (tx.weightKg != null) DetailRow("الوزن", "${"%.2f".format(tx.weightKg)} كغم")
        DetailRow("نوع الشحنة", if (tx.isRefrigerated) "مبرّدة" else "غير مبرّدة")
        if (tx.expectedArrivalDate != null) {
            DetailRow("الوصول المتوقع", tx.expectedArrivalDate.toDateString())
        }
        if (tx.actualArrivalDate != null) {
            DetailRow("الوصول الفعلي", tx.actualArrivalDate.toDateString())
        }

        // Bill of lading — editable
        if (editingBol) {
            OutlinedTextField(
                value         = bolDraft,
                onValueChange = { bolDraft = it },
                label         = { Text("رقم بوليصة الشحن") },
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick  = { editingBol = false; bolDraft = tx.billOfLadingNumber ?: "" },
                    modifier = Modifier.weight(1f),
                ) { Text("إلغاء") }
                Button(
                    onClick  = { onSaveBillOfLading(bolDraft); editingBol = false },
                    modifier = Modifier.weight(1f),
                ) { Text("حفظ") }
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text  = "رقم بوليصة الشحن",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text  = tx.billOfLadingNumber ?: "—",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                if (canWrite) {
                    IconButton(onClick = { editingBol = true }) {
                        Icon(
                            imageVector        = Icons.Default.Edit,
                            contentDescription = "تعديل رقم بوليصة الشحن",
                            modifier           = Modifier.size(18.dp),
                            tint               = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
            HorizontalDivider(Modifier.padding(top = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)
        }

        // ── Workflow ──────────────────────────────────────────────────────
        DetailRow("الأولوية", tx.priority.labelAr)
        DetailRow("المرحلة الحالية", tx.currentPhase.labelAr)
        DetailRow("الحالة الإجرائية", (tx.exceptionState ?: tx.currentStatus).labelAr())
        DetailRow("تاريخ الإنشاء", tx.createdAt.toDateString())
        DetailRow("آخر تحديث", tx.updatedAt.toDateString())
        if (tx.closedAt != null) DetailRow("تاريخ الإغلاق", tx.closedAt.toDateString())
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
        HorizontalDivider(Modifier.padding(top = 4.dp),
            color = MaterialTheme.colorScheme.outlineVariant)
    }
}

// ── Activity log tab ─────────────────────────────────────────────────────────

@Composable
private fun ActivityLogTab(log: List<ActivityLog>) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (log.isEmpty()) {
            Text("لا توجد سجلات نشاط.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            log.forEach { entry ->
                Card(
                    modifier  = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(1.dp),
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(entry.action.labelAr(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold)
                        if (entry.fromStatus != null && entry.toStatus != null) {
                            val from = runCatching {
                                com.rms.customs.domain.model.enums.TransactionStatus.valueOf(entry.fromStatus)
                            }.getOrNull()
                            val to = runCatching {
                                com.rms.customs.domain.model.enums.TransactionStatus.valueOf(entry.toStatus)
                            }.getOrNull()
                            Text(
                                text  = "${from?.labelAr() ?: entry.fromStatus} ← ${to?.labelAr() ?: entry.toStatus}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            text  = entry.occurredAt.toDateString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

// ── Notes tab ────────────────────────────────────────────────────────────────

@Composable
private fun NotesTab(tx: Transaction, viewModel: TransactionDetailViewModel) {
    var notes    by rememberSaveable(tx.notes) { mutableStateOf(tx.notes ?: "") }
    val canWrite = LocalUserSession.current?.user?.role?.canWrite == true

    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value         = notes,
            onValueChange = { notes = it },
            label         = { Text("ملاحظات") },
            modifier      = Modifier.fillMaxWidth().height(160.dp),
            maxLines      = 8,
            readOnly      = !canWrite,
        )
        if (canWrite) {
            Button(
                onClick  = { viewModel.updateNotes(notes) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("حفظ الملاحظات") }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun Long.toDateString(): String =
    SimpleDateFormat("dd/MM/yyyy HH:mm", JavaLocale.forLanguageTag("ar")).format(Date(this))
