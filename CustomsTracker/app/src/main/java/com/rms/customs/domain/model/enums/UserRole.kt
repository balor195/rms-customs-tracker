package com.rms.customs.domain.model.enums

enum class UserRole(val labelAr: String, val labelEn: String) {
    ADMIN("مسؤول النظام", "System Admin"),
    COORDINATOR("منسق التخليص", "Clearance Coordinator"),
    SUPERVISOR("مشرف", "Supervisor"),
    VIEWER("مستعرض", "Viewer");

    val canWrite: Boolean get() = this in setOf(ADMIN, COORDINATOR)
    val canApprove: Boolean get() = this in setOf(ADMIN, SUPERVISOR)
    val canManageUsers: Boolean get() = this == ADMIN
    val canExport: Boolean get() = this in setOf(ADMIN, SUPERVISOR)
}
