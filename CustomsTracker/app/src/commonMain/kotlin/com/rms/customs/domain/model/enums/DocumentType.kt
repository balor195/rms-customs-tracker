package com.rms.customs.domain.model.enums

enum class DocumentType(val labelAr: String, val labelEn: String, val requiredPhase: Int) {
    // Phase 1 — Transaction Preparation
    PURCHASE_ORDER("أمر الشراء", "Purchase Order", 1),
    CONTRACT("نسخة العقد", "Contract Copy", 1),

    // Phase 2 — Shipment Arrival at Airport (shipping/transport paperwork)
    COMMERCIAL_INVOICE("الفاتورة التجارية", "Commercial Invoice", 2),
    PACKING_LIST("قائمة التعبئة", "Packing List", 2),
    BILL_OF_LADING("بوليصة الشحن", "Bill of Lading", 2),
    AIRWAY_BILL("بوليصة الشحن الجوي", "Airway Bill", 2),

    // Phase 3 — Clearance (all clearance/customs/gov-agency paperwork)
    CERTIFICATE_OF_ORIGIN("شهادة المنشأ", "Certificate of Origin", 3),
    HEALTH_CERTIFICATE("الشهادة الصحية", "Health Certificate", 3),
    REGISTRATION_CERTIFICATE("شهادة تسجيل المنتج", "Product Registration Certificate", 3),
    CUSTOMS_DECLARATION("التصريح الجمركي", "Customs Declaration", 3),
    MILITARY_EXEMPTION_REQUEST("طلب الإعفاء العسكري", "Military Exemption Request", 3),
    MILITARY_EXEMPTION_APPROVAL("موافقة الإعفاء العسكري", "Military Exemption Approval", 3),
    JFDA_IMPORT_PERMIT("إذن الاستيراد - هيئة الغذاء والدواء", "JFDA Import Permit", 3),
    CUSTOMS_INSPECTION_REPORT("تقرير التفتيش الجمركي", "Customs Inspection Report", 3),
    CUSTOMS_RELEASE_ORDER("أمر الإفراج الجمركي", "Customs Release Order", 3),
    MILITARY_RELEASE_ORDER("أمر الإفراج العسكري", "Military Release Order", 3),

    // Phase 4 — Warehouse Transfer Confirmation (final closing documents)
    DELIVERY_NOTE("إشعار التسليم", "Delivery Note", 4),
    RECEIVING_MINUTES("محضر الاستلام", "Receiving Minutes", 4),
    PAYMENT_VOUCHER("سند الدفع", "Payment Voucher", 4),

    OTHER("أخرى", "Other", 0);
}
