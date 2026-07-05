package com.rms.customs.domain.model.enums

enum class TransactionPhase(val number: Int, val labelAr: String, val labelEn: String) {
    PHASE_1_TENDER(1, "تحضير المناقصة وإصدارها", "Tender Preparation & Publication"),
    PHASE_2_CLEARANCE(2, "طلب تخليص", "Clearance Request"),
    PHASE_3_FINANCIAL(3, "إغلاق المعاملة والتسوية المالية", "Transaction Closing & Financial Settlement"),
    PHASE_4_WAREHOUSE_CONFIRMATION(4, "تم النقل الى المستودعات", "Transferred to Warehouses");
}
