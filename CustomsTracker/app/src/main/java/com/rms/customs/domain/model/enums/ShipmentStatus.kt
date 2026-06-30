package com.rms.customs.domain.model.enums

enum class ShipmentStatus(val labelAr: String, val labelEn: String) {
    EXPECTED("متوقع وصولها", "Expected"),
    ARRIVED("وصلت", "Arrived"),
    CLEARED("تم التخليص", "Cleared");
}
