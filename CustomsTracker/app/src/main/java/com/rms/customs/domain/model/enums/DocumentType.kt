package com.rms.customs.domain.model.enums

enum class DocumentType(val labelAr: String, val labelEn: String, val requiredPhase: Int) {
    // Phase 1 — Tender Preparation
    PURCHASE_ORDER("أمر الشراء", "Purchase Order", 1),
    CONTRACT("نسخة العقد", "Contract Copy", 1),

    // Phase 2 — Clearance Request (all clearance/customs/gov-agency paperwork)
    COMMERCIAL_INVOICE("الفاتورة التجارية", "Commercial Invoice", 2),
    PACKING_LIST("قائمة التعبئة", "Packing List", 2),
    BILL_OF_LADING("بوليصة الشحن", "Bill of Lading", 2),
    AIRWAY_BILL("بوليصة الشحن الجوي", "Airway Bill", 2),
    CERTIFICATE_OF_ORIGIN("شهادة المنشأ", "Certificate of Origin", 2),
    HEALTH_CERTIFICATE("الشهادة الصحية", "Health Certificate", 2),
    REGISTRATION_CERTIFICATE("شهادة تسجيل المنتج", "Product Registration Certificate", 2),
    CUSTOMS_DECLARATION("التصريح الجمركي", "Customs Declaration", 2),
    MILITARY_EXEMPTION_REQUEST("طلب الإعفاء العسكري", "Military Exemption Request", 2),
    MILITARY_EXEMPTION_APPROVAL("موافقة الإعفاء العسكري", "Military Exemption Approval", 2),
    JFDA_IMPORT_PERMIT("إذن الاستيراد - هيئة الغذاء والدواء", "JFDA Import Permit", 2),
    CUSTOMS_INSPECTION_REPORT("تقرير التفتيش الجمركي", "Customs Inspection Report", 2),
    CUSTOMS_RELEASE_ORDER("أمر الإفراج الجمركي", "Customs Release Order", 2),
    MILITARY_RELEASE_ORDER("أمر الإفراج العسكري", "Military Release Order", 2),

    // Phase 3 — Financial Settlement
    PAYMENT_VOUCHER("سند الدفع", "Payment Voucher", 3),

    // Phase 4 — Warehouse Transfer Confirmation
    DELIVERY_NOTE("إشعار التسليم", "Delivery Note", 4),
    RECEIVING_MINUTES("محضر الاستلام", "Receiving Minutes", 4),

    OTHER("أخرى", "Other", 0);
}
