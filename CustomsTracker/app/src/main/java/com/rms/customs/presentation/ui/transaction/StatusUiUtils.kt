package com.rms.customs.presentation.ui.transaction

import androidx.compose.ui.graphics.Color
import com.rms.customs.domain.model.enums.LogAction
import com.rms.customs.domain.model.enums.PhaseStatus
import com.rms.customs.domain.model.enums.TransactionStatus
import com.rms.customs.presentation.ui.theme.CustomsColors

fun TransactionStatus.labelAr(): String = when (this) {
    TransactionStatus.DRAFT                        -> "مسودة"
    TransactionStatus.TENDER_PREPARATION           -> "تحضير المعاملة"
    TransactionStatus.ARRIVED_AT_AIRPORT           -> "وصلت الشحنة للمطار"
    TransactionStatus.CLEARANCE_ISSUED             -> "تم التخليص"
    TransactionStatus.TRANSFERRED_TO_WAREHOUSE     -> "تم النقل الى المستودعات"
    TransactionStatus.BLOCKED                      -> "محجوبة"
    TransactionStatus.ON_HOLD                      -> "معلّقة"
    TransactionStatus.DISPUTED                     -> "متنازع عليها"
}

fun TransactionStatus.statusColor(): Color = when (this) {
    TransactionStatus.BLOCKED,
    TransactionStatus.DISPUTED,
    TransactionStatus.TRANSFERRED_TO_WAREHOUSE     -> CustomsColors.Overdue
    TransactionStatus.ON_HOLD                      -> CustomsColors.Warning
    else                                           -> CustomsColors.OnTime
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
