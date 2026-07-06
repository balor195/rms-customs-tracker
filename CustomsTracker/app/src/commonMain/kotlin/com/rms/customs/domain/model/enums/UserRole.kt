package com.rms.customs.domain.model.enums

enum class UserRole(val labelAr: String, val labelEn: String) {
    ADMIN("مسؤول النظام", "System Admin"),
    CLEARANCE("التخليص", "Clearance"),
    WAREHOUSE("المستودعات", "Warehouse"),
    SUPERVISOR("مشرف", "Supervisor"),
    TENDER_OFFICER("ضابط العطاء", "Tender Officer"),
    VIEWER("مستعرض", "Viewer");

    /** General create/edit of transaction fields & documents. */
    val canWrite: Boolean get() = this in setOf(ADMIN, SUPERVISOR, TENDER_OFFICER)
    val canCreateTransaction: Boolean get() = this in setOf(ADMIN, SUPERVISOR, TENDER_OFFICER)
    val canApprove: Boolean get() = this in setOf(ADMIN, SUPERVISOR)
    val canManageUsers: Boolean get() = this == ADMIN
    val canExport: Boolean get() = this in setOf(ADMIN, SUPERVISOR)

    /** Exclusive: only Clearance may mark a transaction "تم التخليص". */
    val canMarkClearanceDone: Boolean get() = this in setOf(ADMIN, CLEARANCE)

    /** Exclusive: only Warehouse may confirm "تم النقل الى المستودعات". */
    val canMarkWarehouseTransferred: Boolean get() = this in setOf(ADMIN, WAREHOUSE)

    /** Sees transactions across every division, not just their own; also implies no fixed division. */
    val seesAllDivisions: Boolean get() = this in setOf(ADMIN, CLEARANCE, WAREHOUSE)
}
