package com.rms.customs.data.export

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.rms.customs.domain.model.Transaction
import com.rms.customs.domain.model.enums.Department
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class PdfExporter(
    private val context: Context,
) {
    companion object {
        private const val PAGE_W  = 595
        private const val PAGE_H  = 842
        private const val MARGIN  = 36f
        private const val ROW_H   = 24f
        private val USABLE_W get() = PAGE_W - 2 * MARGIN   // 523
    }

    // ── Paints ───────────────────────────────────────────────────────────────

    private fun paint(
        size: Float,
        color: Int = Color.BLACK,
        bold: Boolean = false,
        align: Paint.Align = Paint.Align.CENTER,
    ) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size; this.color = color; isFakeBoldText = bold; textAlign = align
    }

    private val orgPaint      = paint(7.5f, color = Color.GRAY, align = Paint.Align.LEFT)
    private val titlePaint    = paint(13f,  bold = true, align = Paint.Align.CENTER)
    private val subtitlePaint = paint(9f,   color = Color.DKGRAY, align = Paint.Align.CENTER)
    private val datePaint     = paint(7.5f, color = Color.GRAY, align = Paint.Align.RIGHT)
    private val colHdrPaint   = paint(8.5f, color = Color.WHITE, bold = true, align = Paint.Align.CENTER)
    private val bodyPaint     = paint(8f,   align = Paint.Align.CENTER)
    private val footerPaint   = paint(7.5f, color = Color.GRAY, align = Paint.Align.LEFT)
    private val greenFill     = Paint().apply { color = 0xFF1B5E20.toInt(); style = Paint.Style.FILL }
    private val evenFill      = Paint().apply { color = 0xFFF1F8E9.toInt(); style = Paint.Style.FILL }
    private val thinLine      = Paint().apply { color = 0xFFBDBDBD.toInt(); strokeWidth = 0.5f; style = Paint.Style.STROKE }
    private val thickLine     = Paint().apply { color = 0xFF9E9E9E.toInt(); strokeWidth = 1f;   style = Paint.Style.STROKE }

    // ── Public API ────────────────────────────────────────────────────────────

    fun generateWeekly(txs: List<Transaction>): File {
        val active = txs.filter { it.isActive }

        val rows = active.map { tx ->
            listOf(
                tx.accreditationNumber ?: tx.transactionRef,
                clip(tx.supplierName, 18),
                clip(tx.responsibleOfficer.ifBlank { "—" }, 14),
                tx.division?.labelAr ?: "—",
                tx.currentPhase.labelAr,
            )
        }

        return buildPdf(
            typeName    = "weekly",
            title       = "التقرير الأسبوعي التشغيلي",
            subtitle    = "المعاملات النشطة — ${active.size} معاملة",
            colHeaders  = listOf("رقم الاعتماد", "المورد", "الضابط", "الشعبة", "المرحلة"),
            colWidths   = listOf(0.20f, 0.24f, 0.18f, 0.20f, 0.18f),
            rows        = rows,
            footer      = "إجمالي النشطة: ${active.size}",
        )
    }

    fun generateMonthly(txs: List<Transaction>): File {
        val active        = txs.filter { it.isActive }
        val startOfMonth  = startOfMonth()
        val closedMonth   = txs.count { it.closedAt != null && it.closedAt >= startOfMonth }
        val avgTotal      = avgDays(txs.filter { it.closedAt != null }.map { it.closedAt!! - it.createdAt })
        val monthName     = SimpleDateFormat("MMMM yyyy", Locale.forLanguageTag("ar")).format(Date())

        val rows = listOf(
            row("إجمالي المعاملات النشطة",           "${active.size}"),
            row("مغلقة هذا الشهر",                    "$closedMonth"),
            row("متوسط وقت الإنجاز الكلي",           avgTotal?.let { "${it.roundToInt()} يوم" } ?: "—"),
        )

        return buildPdf(
            typeName   = "monthly",
            title      = "التقرير الشهري التحليلي — $monthName",
            subtitle   = "مؤشرات الأداء للشهر الحالي",
            colHeaders = listOf("المؤشر", "القيمة"),
            colWidths  = listOf(0.74f, 0.26f),
            rows       = rows,
            footer     = "تاريخ الإصدار: ${nowStr()}",
        )
    }

    fun generateExecutive(txs: List<Transaction>): File {
        val active       = txs.filter { it.isActive }
        val avgTotal     = avgDays(txs.filter { it.closedAt != null }.map { it.closedAt!! - it.createdAt })

        val summaryRows = listOf(
            row("إجمالي المعاملات النشطة",  "${active.size}"),
            row("متوسط وقت الإنجاز الكلي",  avgTotal?.let { "${it.roundToInt()} يوم" } ?: "—"),
        )

        return buildPdf(
            typeName   = "executive",
            title      = "التقرير التنفيذي",
            subtitle   = "ملخص لمديرية الصيدلة والتزويد الطبّي — الخدمات الطبية الملكية",
            colHeaders = listOf("المؤشر", "القيمة"),
            colWidths  = listOf(0.74f, 0.26f),
            rows       = summaryRows,
            footer     = "سري — للاستخدام الرسمي فقط | ${nowStr()}",
        )
    }

    // ── By Officer ────────────────────────────────────────────────────────────

    fun generateByOfficer(txs: List<Transaction>): File {
        val groups = txs
            .groupBy { it.responsibleOfficer.ifBlank { "غير محدد" } }
            .entries
            .sortedByDescending { (_, list) -> list.size }

        val rows = groups.map { (officer, list) ->
            val totalValue = list.mapNotNull { it.totalValue }.sum()
            val division   = list.firstOrNull { it.division != null }?.division?.labelAr ?: "—"
            listOf(
                clip(officer, 22),
                division,
                "${list.size}",
                if (totalValue > 0) "${"%.0f".format(totalValue)} JOD" else "—",
            )
        }

        return buildPdf(
            typeName   = "by_officer",
            title      = "تقرير الضباط المسؤولين",
            subtitle   = "توزيع المعاملات حسب الضابط المكلف",
            colHeaders = listOf("الضابط المسؤول", "الشعبة", "عدد المعاملات", "القيمة الإجمالية"),
            colWidths  = listOf(0.30f, 0.26f, 0.18f, 0.26f),
            rows       = rows,
            footer     = "إجمالي الضباط المسجلين: ${groups.size}   |   ${nowStr()}",
        )
    }

    // ── Expected Shipments ────────────────────────────────────────────────────

    fun generateExpectedShipments(txs: List<Transaction>): File {
        val expected = txs
            .filter { it.currentPhase.number < 2 }
            .sortedWith(compareBy(nullsLast()) { it.expectedArrivalDate })

        val rows = expected.map { tx ->
            listOf(
                tx.accreditationNumber ?: tx.transactionRef,
                clip(tx.supplierName, 16),
                clip(tx.responsibleOfficer.ifBlank { "—" }, 14),
                tx.expectedArrivalDate?.let {
                    SimpleDateFormat("yyyy/MM/dd", Locale.US).format(Date(it))
                } ?: "غير محدد",
                tx.totalValue?.let { "${"%.0f".format(it)} JOD" } ?: "—",
            )
        }

        return buildPdf(
            typeName   = "expected_shipments",
            title      = "الشحنات المتوقعة الوصول",
            subtitle   = "${expected.size} شحنة — مرتبة بتاريخ الوصول",
            colHeaders = listOf("رقم الاعتماد", "المورد", "الضابط", "الوصول المتوقع", "قيمة العطاء"),
            colWidths  = listOf(0.20f, 0.22f, 0.18f, 0.20f, 0.20f),
            rows       = rows,
            footer     = "إجمالي الشحنات المتوقعة: ${expected.size}   |   ${nowStr()}",
        )
    }

    // ── Value by Division ─────────────────────────────────────────────────────

    fun generateValueByDivision(txs: List<Transaction>): File {
        val rows = Department.entries.map { dept ->
            val list       = txs.filter { it.division == dept }
            val totalValue = list.mapNotNull { it.totalValue }.sum()
            listOf(
                dept.labelAr,
                "${list.size}",
                if (totalValue > 0) "${"%.0f".format(totalValue)} JOD" else "—",
            )
        }
        val grandTotal = txs.mapNotNull { it.totalValue }.sum()

        return buildPdf(
            typeName   = "by_division",
            title      = "القيمة حسب الشعبة الطبية",
            subtitle   = "إجمالي قيمة العطاءات مصنفةً بحسب الشعبة",
            colHeaders = listOf("الشعبة الطبية", "عدد المعاملات", "القيمة الإجمالية"),
            colWidths  = listOf(0.44f, 0.22f, 0.34f),
            rows       = rows,
            footer     = "الإجمالي الكلي: ${"%.0f".format(grandTotal)} JOD   |   ${nowStr()}",
        )
    }

    // ── Core builder ──────────────────────────────────────────────────────────

    private fun buildPdf(
        typeName: String,
        title: String,
        subtitle: String,
        colHeaders: List<String>,
        colWidths: List<Float>,
        rows: List<List<String>>,
        footer: String,
    ): File {
        val dir  = File(context.filesDir, "reports").apply { mkdirs() }
        val ts   = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = File(dir, "rms_${typeName}_$ts.pdf")

        val doc      = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, 1).create()
        val page     = doc.startPage(pageInfo)
        val cv       = page.canvas
        val widths   = colWidths.map { it * USABLE_W }

        var y = MARGIN

        // Org header
        cv.drawText(
            "الخدمات الطبية الملكية — مديرية الصيدلة والتزويد الطبّي",
            MARGIN, y + 10f, orgPaint,
        )
        cv.drawText(nowStr(), PAGE_W - MARGIN, y + 10f, datePaint)
        y += 20f

        cv.drawLine(MARGIN, y, PAGE_W - MARGIN, y, thickLine)
        y += 12f

        // Title + subtitle
        cv.drawText(title,    PAGE_W / 2f, y + 14f, titlePaint)
        y += 24f
        cv.drawText(subtitle, PAGE_W / 2f, y + 11f, subtitlePaint)
        y += 20f
        cv.drawLine(MARGIN, y, PAGE_W - MARGIN, y, thinLine)
        y += 10f

        // Table header row
        var x = MARGIN
        cv.drawRect(MARGIN, y, PAGE_W - MARGIN, y + ROW_H, greenFill)
        widths.forEachIndexed { i, w ->
            cv.drawText(colHeaders[i], x + w / 2f, y + ROW_H - 7f, colHdrPaint)
            x += w
        }
        y += ROW_H

        // Data rows
        rows.forEachIndexed { idx, row ->
            if (y + ROW_H > PAGE_H - MARGIN - 30f) return@forEachIndexed
            if (idx % 2 == 1) cv.drawRect(MARGIN, y, PAGE_W - MARGIN, y + ROW_H, evenFill)
            x = MARGIN
            widths.forEachIndexed { col, w ->
                val cell = if (col < row.size) row[col] else ""
                cv.drawText(clip(cell, (w / 4.5f).toInt().coerceAtLeast(6)), x + w / 2f, y + ROW_H - 7f, bodyPaint)
                x += w
            }
            cv.drawLine(MARGIN, y + ROW_H, PAGE_W - MARGIN, y + ROW_H, thinLine)
            y += ROW_H
        }

        // Bottom border
        cv.drawLine(MARGIN, y, PAGE_W - MARGIN, y, thickLine)

        // Footer
        cv.drawText(footer, MARGIN, PAGE_H - MARGIN, footerPaint)

        doc.finishPage(page)
        file.outputStream().use { doc.writeTo(it) }
        doc.close()
        return file
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun row(label: String, value: String) = listOf(label, value)

    private fun clip(text: String, maxLen: Int): String =
        if (text.length <= maxLen) text else text.take(maxLen - 1) + "…"

    private fun avgDays(durations: List<Long>): Double? =
        durations.takeIf { it.isNotEmpty() }?.average()?.div(86_400_000.0)

    private fun nowStr() = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US).format(Date())

    private fun startOfMonth(): Long = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
