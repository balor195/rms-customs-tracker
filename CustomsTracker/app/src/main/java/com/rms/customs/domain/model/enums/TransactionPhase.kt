package com.rms.customs.domain.model.enums

enum class TransactionPhase(val number: Int, val labelAr: String, val labelEn: String) {
    PHASE_1_TENDER(1, "تحضير المعاملة", "Transaction Preparation"),
    PHASE_2_AIRPORT_ARRIVAL(2, "وصلت الشحنة للمطار", "Shipment Arrived at Airport"),
    PHASE_3_CLEARANCE(3, "تم التخليص", "Clearance Issued"),
    PHASE_4_WAREHOUSE_CONFIRMATION(4, "تم النقل الى المستودعات", "Transferred to Warehouses");
}
