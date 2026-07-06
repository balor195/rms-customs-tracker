package com.rms.customs.data.export

import android.content.Context
import com.rms.customs.domain.model.Transaction
import com.rms.customs.domain.model.enums.Department
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CsvExporter(
    private val context: Context,
) {
    private fun reportsDir() = File(context.filesDir, "reports").apply { mkdirs() }
    private fun ts() = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    private fun dateFmt(epoch: Long) = SimpleDateFormat("yyyy/MM/dd", Locale.US).format(Date(epoch))
    private fun String.csvSafe() = "\"${replace("\"", "\"\"")}\""

    // ── Weekly operational report ─────────────────────────────────────────────

    fun generateWeekly(txs: List<Transaction>): File {
        val file = File(reportsDir(), "rms_weekly_${ts()}.csv")

        file.bufferedWriter(Charsets.UTF_8).use { w ->
            w.write("﻿") // BOM for Excel UTF-8
            w.write(
                "رقم الاعتماد,رقم المعاملة,المورد,الضابط المسؤول,الشعبة," +
                "المرحلة الحالية,الحالة,الأولوية," +
                "أيام آخر تحديث,تاريخ الإنشاء\n"
            )
            txs.filter { it.isActive }.forEach { tx ->
                w.write(
                    "${(tx.accreditationNumber ?: "").csvSafe()}," +
                    "${tx.transactionRef}," +
                    "${tx.supplierName.csvSafe()}," +
                    "${tx.responsibleOfficer.csvSafe()}," +
                    "${(tx.division?.labelAr ?: "").csvSafe()}," +
                    "${tx.currentPhase.labelAr.csvSafe()}," +
                    "${tx.currentStatus.name}," +
                    "${tx.priority.name}," +
                    "${tx.daysSinceUpdate}," +
                    "${dateFmt(tx.createdAt)}\n"
                )
            }
        }
        return file
    }

    // ── Full export (all fields) ──────────────────────────────────────────────

    fun generateAll(txs: List<Transaction>): File {
        val file = File(reportsDir(), "rms_all_transactions_${ts()}.csv")

        file.bufferedWriter(Charsets.UTF_8).use { w ->
            w.write("﻿")
            w.write(
                "رقم المعاملة,رقم الاعتماد,المورد,عنوان العطاء,الشعبة,الجهة المستفيدة," +
                "الضابط المسؤول,المرحلة,الحالة,الأولوية," +
                "الوصول المتوقع,الوصول الفعلي,رقم بوليصة الشحن,الوزن (كغم),نوع الشحنة,العمر الافتراضي," +
                "قيمة العطاء (JOD),تاريخ الإنشاء,تاريخ الإغلاق,أيام الإنجاز\n"
            )
            txs.forEach { tx ->
                val durationDays = tx.closedAt?.let { (it - tx.createdAt) / 86_400_000L }?.toString() ?: ""
                val closedDate   = tx.closedAt?.let { dateFmt(it) } ?: ""
                w.write(
                    "${tx.transactionRef}," +
                    "${(tx.accreditationNumber ?: "").csvSafe()}," +
                    "${tx.supplierName.csvSafe()}," +
                    "${tx.title.csvSafe()}," +
                    "${(tx.division?.labelAr ?: "").csvSafe()}," +
                    "${(tx.beneficiary?.labelAr ?: "").csvSafe()}," +
                    "${tx.responsibleOfficer.csvSafe()}," +
                    "${tx.currentPhase.labelAr.csvSafe()}," +
                    "${tx.currentStatus.name}," +
                    "${tx.priority.name}," +
                    "${tx.expectedArrivalDate?.let { dateFmt(it) } ?: ""}," +
                    "${tx.actualArrivalDate?.let { dateFmt(it) } ?: ""}," +
                    "${(tx.billOfLadingNumber ?: "").csvSafe()}," +
                    "${tx.weightKg?.let { "%.2f".format(it) } ?: ""}," +
                    "${(if (tx.isRefrigerated) "مبرّدة" else "غير مبرّدة").csvSafe()}," +
                    "${(tx.defaultShelfLife ?: "").csvSafe()}," +
                    "${"%.2f".format(tx.totalValue ?: 0.0)}," +
                    "${dateFmt(tx.createdAt)}," +
                    "$closedDate," +
                    "$durationDays\n"
                )
            }
        }
        return file
    }

    // ── By Officer ────────────────────────────────────────────────────────────

    fun generateByOfficer(txs: List<Transaction>): File {
        val groups = txs
            .groupBy { it.responsibleOfficer.ifBlank { "غير محدد" } }
            .entries
            .sortedByDescending { (_, list) -> list.size }
        val file = File(reportsDir(), "rms_by_officer_${ts()}.csv")

        file.bufferedWriter(Charsets.UTF_8).use { w ->
            w.write("﻿")
            w.write("الضابط المسؤول,الشعبة الرئيسية,عدد المعاملات,القيمة الإجمالية (JOD),نشطة,مغلقة\n")
            groups.forEach { (officer, list) ->
                val totalValue = list.mapNotNull { it.totalValue }.sum()
                val division   = list.firstOrNull { it.division != null }?.division?.labelAr ?: ""
                val active     = list.count { it.isActive }
                val closed     = list.count { it.currentStatus.isTerminal }
                w.write(
                    "${officer.csvSafe()}," +
                    "${division.csvSafe()}," +
                    "${list.size}," +
                    "${"%.2f".format(totalValue)}," +
                    "$active," +
                    "$closed\n"
                )
            }
        }
        return file
    }

    // ── Expected Shipments ────────────────────────────────────────────────────

    fun generateExpectedShipments(txs: List<Transaction>): File {
        val expected = txs
            .filter { it.currentPhase.number < 2 }
            .sortedWith(compareBy(nullsLast()) { it.expectedArrivalDate })
        val file = File(reportsDir(), "rms_expected_shipments_${ts()}.csv")

        file.bufferedWriter(Charsets.UTF_8).use { w ->
            w.write("﻿")
            w.write(
                "رقم الاعتماد,المورد,الضابط المسؤول,الشعبة," +
                "الوصول المتوقع,قيمة العطاء (JOD),المرحلة الحالية,رقم بوليصة الشحن\n"
            )
            expected.forEach { tx ->
                w.write(
                    "${(tx.accreditationNumber ?: tx.transactionRef).csvSafe()}," +
                    "${tx.supplierName.csvSafe()}," +
                    "${tx.responsibleOfficer.csvSafe()}," +
                    "${(tx.division?.labelAr ?: "").csvSafe()}," +
                    "${tx.expectedArrivalDate?.let { dateFmt(it) } ?: "غير محدد"}," +
                    "${"%.2f".format(tx.totalValue ?: 0.0)}," +
                    "${tx.currentPhase.labelAr.csvSafe()}," +
                    "${(tx.billOfLadingNumber ?: "").csvSafe()}\n"
                )
            }
        }
        return file
    }

    // ── Value by Division ─────────────────────────────────────────────────────

    fun generateValueByDivision(txs: List<Transaction>): File {
        val file = File(reportsDir(), "rms_by_division_${ts()}.csv")

        file.bufferedWriter(Charsets.UTF_8).use { w ->
            w.write("﻿")
            w.write("الشعبة الطبية,عدد المعاملات,القيمة الإجمالية (JOD),نشطة,مغلقة\n")
            Department.entries.forEach { dept ->
                val list       = txs.filter { it.division == dept }
                val totalValue = list.mapNotNull { it.totalValue }.sum()
                val active     = list.count { it.isActive }
                val closed     = list.count { it.currentStatus.isTerminal }
                w.write(
                    "${dept.labelAr.csvSafe()}," +
                    "${list.size}," +
                    "${"%.2f".format(totalValue)}," +
                    "$active," +
                    "$closed\n"
                )
            }
        }
        return file
    }
}
