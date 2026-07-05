package com.rms.customs.presentation.viewmodel

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rms.customs.data.export.CsvExporter
import com.rms.customs.data.export.PdfExporter
import com.rms.customs.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ReportType {
    WEEKLY, MONTHLY, EXECUTIVE, BY_OFFICER, EXPECTED_SHIPMENTS, VALUE_BY_DIVISION;

    val labelAr: String get() = when (this) {
        WEEKLY             -> "التقرير الأسبوعي التشغيلي"
        MONTHLY            -> "التقرير الشهري التحليلي"
        EXECUTIVE          -> "التقرير التنفيذي"
        BY_OFFICER         -> "تقرير حسب الضابط المسؤول"
        EXPECTED_SHIPMENTS -> "الشحنات المتوقعة الوصول"
        VALUE_BY_DIVISION  -> "القيمة حسب الشعبة الطبية"
    }

    val descAr: String get() = when (this) {
        WEEKLY             -> "جدول المعاملات النشطة مع حالة SLA والضابط والشعبة"
        MONTHLY            -> "مؤشرات الأداء والمتوسطات الشهرية"
        EXECUTIVE          -> "ملخص تنفيذي مع أبرز نقاط الاختناق"
        BY_OFFICER         -> "توزيع المعاملات والقيم على الضباط المكلفين"
        EXPECTED_SHIPMENTS -> "قائمة الشحنات المنتظرة مرتبةً بتاريخ الوصول"
        VALUE_BY_DIVISION  -> "إجمالي قيمة العطاءات مصنفةً بحسب الشعبة"
    }
}

sealed class ExportState {
    object Idle       : ExportState()
    object Generating : ExportState()
    data class Ready(val uri: Uri, val mimeType: String, val fileName: String) : ExportState()
    data class Error(val message: String) : ExportState()
}

@HiltViewModel
class ReportViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val transactionRepository: TransactionRepository,
    private val pdfExporter: PdfExporter,
    private val csvExporter: CsvExporter,
) : ViewModel() {

    private val _selectedType = MutableStateFlow(ReportType.WEEKLY)
    val selectedType: StateFlow<ReportType> = _selectedType.asStateFlow()

    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState.asStateFlow()

    fun selectType(type: ReportType) { _selectedType.value = type }

    fun exportPdf() = viewModelScope.launch {
        _exportState.value = ExportState.Generating
        runCatching {
            val txs = transactionRepository.observeAll().first()
            val file = when (_selectedType.value) {
                ReportType.WEEKLY             -> pdfExporter.generateWeekly(txs)
                ReportType.MONTHLY            -> pdfExporter.generateMonthly(txs)
                ReportType.EXECUTIVE          -> pdfExporter.generateExecutive(txs)
                ReportType.BY_OFFICER         -> pdfExporter.generateByOfficer(txs)
                ReportType.EXPECTED_SHIPMENTS -> pdfExporter.generateExpectedShipments(txs)
                ReportType.VALUE_BY_DIVISION  -> pdfExporter.generateValueByDivision(txs)
            }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            _exportState.value = ExportState.Ready(uri, "application/pdf", file.name)
        }.onFailure { e ->
            _exportState.value = ExportState.Error(e.message ?: "فشل إنشاء التقرير")
        }
    }

    fun exportCsv() = viewModelScope.launch {
        _exportState.value = ExportState.Generating
        runCatching {
            val txs = transactionRepository.observeAll().first()
            val file = when (_selectedType.value) {
                ReportType.WEEKLY             -> csvExporter.generateWeekly(txs)
                ReportType.MONTHLY,
                ReportType.EXECUTIVE          -> csvExporter.generateAll(txs)
                ReportType.BY_OFFICER         -> csvExporter.generateByOfficer(txs)
                ReportType.EXPECTED_SHIPMENTS -> csvExporter.generateExpectedShipments(txs)
                ReportType.VALUE_BY_DIVISION  -> csvExporter.generateValueByDivision(txs)
            }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            _exportState.value = ExportState.Ready(uri, "text/csv", file.name)
        }.onFailure { e ->
            _exportState.value = ExportState.Error(e.message ?: "فشل إنشاء التقرير")
        }
    }

    fun clearState() { _exportState.value = ExportState.Idle }
}
