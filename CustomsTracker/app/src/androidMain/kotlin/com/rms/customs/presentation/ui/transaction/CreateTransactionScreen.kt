package com.rms.customs.presentation.ui.transaction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rms.customs.domain.model.enums.Beneficiary
import com.rms.customs.domain.model.enums.Department
import com.rms.customs.domain.model.enums.Priority
import com.rms.customs.presentation.ui.LocalUserSession
import com.rms.customs.presentation.viewmodel.CreateTransactionViewModel
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTransactionScreen(
    onBack: () -> Unit,
    onCreated: () -> Unit,
    viewModel: CreateTransactionViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val session = LocalUserSession.current

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onCreated()
    }

    // ── Form state ──────────────────────────────────────────────────────────
    var accreditationNumber  by rememberSaveable { mutableStateOf("") }
    var responsibleOfficer   by rememberSaveable { mutableStateOf("") }
    var supplierName         by rememberSaveable { mutableStateOf("") }
    var title                by rememberSaveable { mutableStateOf("") }
    var tenderRef            by rememberSaveable { mutableStateOf("") }
    var billOfLadingNumber   by rememberSaveable { mutableStateOf("") }
    var totalValue           by rememberSaveable { mutableStateOf("") }
    var notes                by rememberSaveable { mutableStateOf("") }
    var weightKg             by rememberSaveable { mutableStateOf("") }
    var isRefrigerated       by rememberSaveable { mutableStateOf(false) }
    var defaultShelfLife     by rememberSaveable { mutableStateOf("") }
    val canPickDivision      = session?.user?.role?.seesAllDivisions == true
    var division             by remember { mutableStateOf(session?.user?.department ?: Department.PHARMACY) }
    var beneficiary          by remember { mutableStateOf(Beneficiary.RMS) }
    var priority             by remember { mutableStateOf(Priority.NORMAL) }
    var expectedArrivalDate  by remember { mutableStateOf<Long?>(null) }

    var divisionExpanded     by remember { mutableStateOf(false) }
    var showDatePicker       by remember { mutableStateOf(false) }

    // "عاجل" مسموحة فقط لشحنة خاصة بالخدمات الطبية الملكية ومبرّدة
    val canUseUrgent = beneficiary == Beneficiary.RMS && isRefrigerated
    LaunchedEffect(canUseUrgent) {
        if (!canUseUrgent && priority == Priority.URGENT) priority = Priority.NORMAL
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("إنشاء معاملة جديدة") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {

            // Section: بيانات العطاء
            SectionHeader("بيانات العطاء")

            // رقم الاعتماد — Primary business identifier
            OutlinedTextField(
                value         = accreditationNumber,
                onValueChange = { accreditationNumber = it },
                label         = { Text("رقم الاعتماد *") },
                placeholder   = { Text("الرقم المرجعي للاعتماد من لجنة العطاءات") },
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true,
            )

            // اسم الشعبة — Division dropdown (locked to own division for scoped roles)
            if (canPickDivision) {
                ExposedDropdownMenuBox(
                    expanded        = divisionExpanded,
                    onExpandedChange = { divisionExpanded = it },
                ) {
                    OutlinedTextField(
                        value         = division.labelAr,
                        onValueChange = {},
                        readOnly      = true,
                        label         = { Text("الشعبة *") },
                        trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(divisionExpanded) },
                        modifier      = Modifier.fillMaxWidth().menuAnchor(),
                    )
                    ExposedDropdownMenu(
                        expanded        = divisionExpanded,
                        onDismissRequest = { divisionExpanded = false },
                    ) {
                        Department.entries.forEach { dept ->
                            DropdownMenuItem(
                                text    = { Text(dept.labelAr) },
                                onClick = { division = dept; divisionExpanded = false },
                            )
                        }
                    }
                }
            } else {
                OutlinedTextField(
                    value         = division.labelAr,
                    onValueChange = {},
                    readOnly      = true,
                    enabled       = false,
                    label         = { Text("الشعبة") },
                    modifier      = Modifier.fillMaxWidth(),
                )
            }

            // العمر الافتراضي — خاص بشعبة المستهلكات الطبية فقط
            if (division == Department.MEDICAL_CONSUMABLES) {
                OutlinedTextField(
                    value         = defaultShelfLife,
                    onValueChange = { defaultShelfLife = it },
                    label         = { Text("العمر الافتراضي") },
                    modifier      = Modifier.fillMaxWidth(),
                    singleLine    = true,
                )
            }

            // اسم الشركة الموردة
            OutlinedTextField(
                value         = supplierName,
                onValueChange = { supplierName = it },
                label         = { Text("اسم الشركة الموردة *") },
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true,
            )

            // قيمة العطاء
            OutlinedTextField(
                value           = totalValue,
                onValueChange   = { totalValue = it },
                label           = { Text("قيمة العطاء (JOD)") },
                modifier        = Modifier.fillMaxWidth(),
                singleLine      = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )

            // الجهة المستفيدة — RMS or Bank
            Text("الجهة المستفيدة *", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Beneficiary.entries.forEach { b ->
                    FilterChip(
                        selected = beneficiary == b,
                        onClick  = { beneficiary = b },
                        label    = { Text(b.labelAr) },
                    )
                }
            }

            // وزن الشحنة
            OutlinedTextField(
                value           = weightKg,
                onValueChange   = { weightKg = it },
                label           = { Text("الوزن (كغم)") },
                modifier        = Modifier.fillMaxWidth(),
                singleLine      = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )

            // هل الشحنة مبرّدة
            Text("نوع الشحنة *", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = isRefrigerated,
                    onClick  = { isRefrigerated = true },
                    label    = { Text("مبرّدة") },
                )
                FilterChip(
                    selected = !isRefrigerated,
                    onClick  = { isRefrigerated = false },
                    label    = { Text("غير مبرّدة") },
                )
            }

            HorizontalDivider()

            // Section: بيانات الشحنة
            SectionHeader("بيانات الشحنة")

            // اسم الضابط المسؤول
            OutlinedTextField(
                value         = responsibleOfficer,
                onValueChange = { responsibleOfficer = it },
                label         = { Text("اسم الضابط المسؤول *") },
                placeholder   = { Text("الضابط المكلف بمتابعة هذه المعاملة") },
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true,
            )

            // رقم بوليصة الشحن
            OutlinedTextField(
                value         = billOfLadingNumber,
                onValueChange = { billOfLadingNumber = it },
                label         = { Text("رقم بوليصة الشحن (اختياري)") },
                placeholder   = { Text("Airway Bill / Bill of Lading") },
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true,
            )

            // تاريخ الوصول المتوقع — Date picker
            OutlinedTextField(
                value         = expectedArrivalDate?.let {
                    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(it))
                } ?: "",
                onValueChange = {},
                readOnly      = true,
                label         = { Text("تاريخ الوصول المتوقع (اختياري)") },
                trailingIcon  = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.DateRange, contentDescription = "اختر التاريخ")
                    }
                },
                modifier      = Modifier.fillMaxWidth(),
            )

            HorizontalDivider()

            // Section: بيانات إضافية
            SectionHeader("بيانات إضافية")

            // وصف المعاملة
            OutlinedTextField(
                value         = title,
                onValueChange = { title = it },
                label         = { Text("وصف / عنوان المعاملة *") },
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true,
            )

            // رقم العطاء (internal ref)
            OutlinedTextField(
                value         = tenderRef,
                onValueChange = { tenderRef = it },
                label         = { Text("رقم العطاء الداخلي (اختياري)") },
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true,
            )

            // الأولوية
            Text("الأولوية", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Priority.entries.forEach { p ->
                    val enabled = p != Priority.URGENT || canUseUrgent
                    FilterChip(
                        selected = priority == p,
                        onClick  = { priority = p },
                        label    = { Text(p.labelAr) },
                        enabled  = enabled,
                    )
                }
            }
            if (!canUseUrgent) {
                Text(
                    text  = "الأولوية \"عاجل\" متاحة فقط للشحنات الخاصة بالخدمات الطبية الملكية والمبرّدة",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // ملاحظات
            OutlinedTextField(
                value         = notes,
                onValueChange = { notes = it },
                label         = { Text("ملاحظات (اختياري)") },
                modifier      = Modifier.fillMaxWidth().height(100.dp),
                maxLines      = 4,
            )

            // Error
            if (uiState.error != null) {
                Text(
                    text  = uiState.error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick  = {
                    session?.let {
                        viewModel.create(
                            title               = title,
                            accreditationNumber = accreditationNumber,
                            responsibleOfficer  = responsibleOfficer,
                            division            = division,
                            beneficiary         = beneficiary,
                            supplierName        = supplierName,
                            totalValue          = totalValue,
                            tenderRef           = tenderRef,
                            billOfLadingNumber  = billOfLadingNumber,
                            expectedArrivalDate = expectedArrivalDate,
                            notes               = notes,
                            priority            = priority,
                            weightKg            = weightKg,
                            isRefrigerated      = isRefrigerated,
                            defaultShelfLife    = if (division == Department.MEDICAL_CONSUMABLES) defaultShelfLife else "",
                            createdByUserId     = it.user.id,
                        )
                    }
                },
                enabled  = !uiState.isLoading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("إنشاء المعاملة")
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    // Date picker dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = expectedArrivalDate
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    expectedArrivalDate = datePickerState.selectedDateMillis
                    showDatePicker = false
                }) { Text("تأكيد") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("إلغاء") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text  = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
    )
}
