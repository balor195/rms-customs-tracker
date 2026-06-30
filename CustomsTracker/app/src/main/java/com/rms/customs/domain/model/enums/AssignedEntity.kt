package com.rms.customs.domain.model.enums

enum class AssignedEntity(val labelAr: String, val labelEn: String) {
    PHARMACY("الصيدلة والتزويد الطبي", "RMS Pharmacy Directorate"),
    TENDERS_COMMITTEE("لجنة العطاءات المركزية", "Central Tenders Committee"),
    CUSTOMS("الجمارك الأردنية", "Jordan Customs"),
    JFDA("مؤسسة الغذاء والدواء", "JFDA"),
    MILITARY_COMMAND("القيادة العامة للقوات المسلحة", "General Command"),
    HEALTH_MINISTRY("وزارة الصحة", "Ministry of Health"),
    ROYAL_JORDANIAN("الملكية الأردنية / المطار العسكري", "Royal Jordanian / Military Airport"),
    FINANCE("الشؤون المالية", "Finance Department"),
    RECEIVING_COMMITTEE("لجنة الاستلام", "Receiving Committee"),
    SUPPLIER("المورد", "Supplier");
}
