package com.rms.customs.presentation.ui.transaction

import androidx.compose.ui.graphics.Color
import com.rms.customs.domain.model.enums.LogAction
import com.rms.customs.domain.model.enums.PhaseStatus
import com.rms.customs.domain.model.enums.ShipmentStatus
import com.rms.customs.domain.model.enums.TransactionStatus
import com.rms.customs.presentation.ui.theme.CustomsColors

fun TransactionStatus.labelAr(): String = when (this) {
    TransactionStatus.DRAFT                        -> "مسودة"
    TransactionStatus.TENDER_PREPARATION           -> "تحضير المناقصة"
    TransactionStatus.TENDER_PUBLISHED             -> "نشر المناقصة"
    TransactionStatus.EVALUATION_IN_PROGRESS       -> "تقييم العروض"
    TransactionStatus.CONTRACT_PENDING_SIGNATURE   -> "انتظار توقيع العقد"
    TransactionStatus.CONTRACT_SIGNED              -> "العقد موقَّع"
    TransactionStatus.CLEARANCE_DOCS_PREPARATION   -> "تحضير ملف التخليص"
    TransactionStatus.DECLARATION_SUBMITTED        -> "التصريح مقدَّم"
    TransactionStatus.GOV_PROCESSING               -> "قيد المعالجة الحكومية"
    TransactionStatus.GOV_APPROVED                 -> "اعتماد حكومي"
    TransactionStatus.FINAL_RELEASE_ISSUED         -> "أمر الإفراج النهائي"
    TransactionStatus.IN_TRANSIT                   -> "في الطريق"
    TransactionStatus.RECEIVED_AT_WAREHOUSE        -> "تم الاستلام بالمستودع"
    TransactionStatus.INSPECTION_COMPLETE          -> "اكتملت المعاينة"
    TransactionStatus.FINANCIAL_SETTLEMENT_PENDING -> "انتظار التسوية المالية"
    TransactionStatus.CLOSED                       -> "مغلقة"
    TransactionStatus.BLOCKED                      -> "محجوبة"
    TransactionStatus.ON_HOLD                      -> "معلّقة"
    TransactionStatus.DISPUTED                     -> "متنازع عليها"
}

fun TransactionStatus.statusColor(): Color = when (this) {
    TransactionStatus.BLOCKED,
    TransactionStatus.DISPUTED                     -> CustomsColors.Overdue
    TransactionStatus.ON_HOLD                      -> CustomsColors.Warning
    TransactionStatus.GOV_PROCESSING               -> CustomsColors.Military
    TransactionStatus.CLOSED                       -> Color(0xFF757575)
    else                                           -> CustomsColors.OnTime
}

fun ShipmentStatus.statusColor(): Color = when (this) {
    ShipmentStatus.EXPECTED -> CustomsColors.Warning
    ShipmentStatus.ARRIVED  -> Color(0xFF1565C0)
    ShipmentStatus.CLEARED  -> CustomsColors.OnTime
}

fun PhaseStatus.labelAr(): String = when (this) {
    PhaseStatus.PENDING     -> "لم يبدأ"
    PhaseStatus.IN_PROGRESS -> "جارٍ"
    PhaseStatus.BLOCKED     -> "محجوب"
    PhaseStatus.DONE        -> "مكتمل"
    PhaseStatus.SKIPPED     -> "متجاوز"
}

fun LogAction.labelAr(): String = when (this) {
    LogAction.CREATED           -> "إنشاء المعاملة"
    LogAction.STATUS_CHANGED    -> "تغيير الحالة"
    LogAction.PHASE_ADVANCED    -> "تقديم المرحلة"
    LogAction.DOC_UPLOADED      -> "رفع مستند"
    LogAction.DOC_DELETED       -> "حذف مستند"
    LogAction.NOTE_ADDED        -> "إضافة ملاحظة"
    LogAction.PHASE_BLOCKED     -> "إيقاف المرحلة"
    LogAction.PHASE_UNBLOCKED   -> "استئناف المرحلة"
    LogAction.SLA_BREACHED      -> "تجاوز المهلة الزمنية"
    LogAction.PRIORITY_CHANGED  -> "تغيير الأولوية"
    LogAction.USER_ASSIGNED     -> "تعيين مستخدم"
    LogAction.EXCEPTION_SET     -> "رفع استثناء"
    LogAction.EXCEPTION_CLEARED -> "إلغاء الاستثناء"
    LogAction.CLOSED            -> "إغلاق المعاملة"
}
