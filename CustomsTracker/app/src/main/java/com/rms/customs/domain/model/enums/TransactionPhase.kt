package com.rms.customs.domain.model.enums

enum class TransactionPhase(val number: Int, val labelAr: String, val labelEn: String) {
    PHASE_1_TENDER(1, "تحضير المناقصة وإصدارها", "Tender Preparation & Publication"),
    PHASE_2_EVALUATION(2, "إحالة المناقصة وتوقيع العقد", "Tender Award & Contract Signing"),
    PHASE_3_CLEARANCE_PREP(3, "تقديم طلب التخليص الجمركي", "Customs Clearance Request"),
    PHASE_4_GOV_AGENCIES(4, "إجراءات الجهات الحكومية والعسكرية", "Gov & Military Agency Processing"),
    PHASE_5_RELEASE(5, "الإفراج عن الشحنة", "Shipment Release"),
    PHASE_6_TRANSIT(6, "النقل والاستلام في المستودعات الطبية", "Transport & Warehouse Receipt"),
    PHASE_7_FINANCIAL(7, "إغلاق المعاملة والتسوية المالية", "Transaction Closing & Financial Settlement");
}
