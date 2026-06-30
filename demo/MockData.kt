package com.rms.customs.data

import androidx.compose.ui.graphics.Color

// ─── Enums ───────────────────────────────────────────────────────────────────

enum class TrackStatus { PENDING, IN_PROGRESS, BLOCKED, DONE }
enum class Priority    { NORMAL, HIGH, URGENT }

// ─── Domain models ───────────────────────────────────────────────────────────

data class Transaction(
    val ref: String,
    val title: String,
    val supplierName: String,
    val priority: Priority,
    val currentPhaseName: String,
    val slaTargetDays: Int,
    val daysElapsed: Int
) {
    val isDelayed: Boolean get() = daysElapsed > slaTargetDays
    val daysOverdue: Int   get() = if (isDelayed) daysElapsed - slaTargetDays else 0
}

data class GovTrack(
    val entityNameAr: String,
    val entityNameEn: String,
    val status: TrackStatus,
    val startedDaysAgo: Int?,
    val slaTargetDays: Int,
    val notes: String? = null,
    val isBottleneck: Boolean = false
)

data class Phase4Detail(
    val transactionRef: String,
    val supplierName: String,
    val totalDaysInPhase: Int,
    val slaTargetDays: Int,
    val tracks: List<GovTrack>
) {
    val isDelayed: Boolean get() = totalDaysInPhase > slaTargetDays
    val daysOverdue: Int   get() = if (isDelayed) totalDaysInPhase - slaTargetDays else 0
}

// ─── Mock data ────────────────────────────────────────────────────────────────

object MockData {

    val transactions = listOf(
        Transaction(
            ref = "RMS-2026-0031",
            title = "أدوية مزمنة — دفعة Q2",
            supplierName = "شركة الدواء العربية",
            priority = Priority.URGENT,
            currentPhaseName = "مصادقة القيادة العامة",
            slaTargetDays = 15,
            daysElapsed = 27
        ),
        Transaction(
            ref = "RMS-2026-0028",
            title = "مستلزمات جراحية",
            supplierName = "مورّد المستلزمات الطبية",
            priority = Priority.HIGH,
            currentPhaseName = "فحص جمركي",
            slaTargetDays = 10,
            daysElapsed = 18
        ),
        Transaction(
            ref = "RMS-2026-0035",
            title = "أجهزة تصوير طبي",
            supplierName = "الشركة الدولية للمعدات",
            priority = Priority.HIGH,
            currentPhaseName = "موافقة JFDA",
            slaTargetDays = 12,
            daysElapsed = 14
        ),
        Transaction(
            ref = "RMS-2026-0039",
            title = "لقاحات موسمية",
            supplierName = "BioMed International",
            priority = Priority.HIGH,
            currentPhaseName = "تحضير ملف التخليص",
            slaTargetDays = 7,
            daysElapsed = 5
        ),
        Transaction(
            ref = "RMS-2026-0022",
            title = "معدات مختبر",
            supplierName = "LabTech Solutions",
            priority = Priority.NORMAL,
            currentPhaseName = "النقل",
            slaTargetDays = 5,
            daysElapsed = 3
        ),
        Transaction(
            ref = "RMS-2026-0019",
            title = "أجهزة تنفس اصطناعي",
            supplierName = "MedDevice GmbH",
            priority = Priority.NORMAL,
            currentPhaseName = "الإغلاق المالي",
            slaTargetDays = 14,
            daysElapsed = 6
        ),
    )

    // Summary bar
    val totalActive   = 14
    val totalDelayed  = 3
    val closedThisMonth = 7

    val phaseDistribution = listOf(
        "تحضير العطاء"           to 2,
        "التقييم والعقد"         to 1,
        "تحضير التخليص"         to 3,
        "الجهات الحكومية"       to 5,
        "النقل والاستلام"        to 1,
        "الإغلاق المالي"        to 2,
    )

    // Phase 4 detail for RMS-2026-0031 (the worst delayed)
    val phase4Detail = Phase4Detail(
        transactionRef = "RMS-2026-0031",
        supplierName = "شركة الدواء العربية",
        totalDaysInPhase = 27,
        slaTargetDays = 15,
        tracks = listOf(
            GovTrack(
                entityNameAr = "دائرة الجمارك الأردنية",
                entityNameEn = "Jordan Customs",
                status = TrackStatus.DONE,
                startedDaysAgo = 27,
                slaTargetDays = 10,
                notes = "اكتمل الفحص — رقم التصريح: JC-2026-8841"
            ),
            GovTrack(
                entityNameAr = "القيادة العامة للقوات المسلحة",
                entityNameEn = "Military Command HQ",
                status = TrackStatus.IN_PROGRESS,
                startedDaysAgo = 18,
                slaTargetDays = 15,
                notes = "طلب الإعفاء مُرسَل — بانتظار المصادقة",
                isBottleneck = true
            ),
            GovTrack(
                entityNameAr = "مؤسسة الغذاء والدواء",
                entityNameEn = "JFDA",
                status = TrackStatus.DONE,
                startedDaysAgo = 27,
                slaTargetDays = 12,
                notes = "صدر إذن الاستيراد: JFDA-IMP-2026-4471"
            )
        )
    )
}
