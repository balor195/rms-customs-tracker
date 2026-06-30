package com.rms.customs.domain.model.enums

enum class DocumentType(val labelAr: String, val labelEn: String, val requiredPhase: Int) {
    // Phase 3 — Clearance preparation
    PURCHASE_ORDER("أمر الشراء", "Purchase Order", 3),
    CONTRACT("نسخة العقد", "Contract Copy", 3),
    COMMERCIAL_INVOICE("الفاتورة التجارية", "Commercial Invoice", 3),
    PACKING_LIST("قائمة التعبئة", "Packing List", 3),
    BILL_OF_LADING("بوليصة الشحن", "Bill of Lading", 3),
    AIRWAY_BILL("بوليصة الشحن الجوي", "Airway Bill", 3),
    CERTIFICATE_OF_ORIGIN("شهادة المنشأ", "Certificate of Origin", 3),
    HEALTH_CERTIFICATE("الشهادة الصحية", "Health Certificate", 3),
    REGISTRATION_CERTIFICATE("شهادة تسجيل المنتج", "Product Registration Certificate", 3),

    // Phase 4 — Gov-agency documents
    CUSTOMS_DECLARATION("التصريح الجمركي", "Customs Declaration", 4),
    MILITARY_EXEMPTION_REQUEST("طلب الإعفاء العسكري", "Military Exemption Request", 4),
    MILITARY_EXEMPTION_APPROVAL("موافقة الإعفاء العسكري", "Military Exemption Approval", 4),
    JFDA_IMPORT_PERMIT("إذن الاستيراد - هيئة الغذاء والدواء", "JFDA Import Permit", 4),
    CUSTOMS_INSPECTION_REPORT("تقرير التفتيش الجمركي", "Customs Inspection Report", 4),

    // Phase 5 — Release
    CUSTOMS_RELEASE_ORDER("أمر الإفراج الجمركي", "Customs Release Order", 5),
    MILITARY_RELEASE_ORDER("أمر الإفراج العسكري", "Military Release Order", 5),

    // Phase 6 — Receipt
    DELIVERY_NOTE("إشعار التسليم", "Delivery Note", 6),
    RECEIVING_MINUTES("محضر الاستلام", "Receiving Minutes", 6),

    // Phase 7 — Financial
    PAYMENT_VOUCHER("سند الدفع", "Payment Voucher", 7),
    OTHER("أخرى", "Other", 0);
}
