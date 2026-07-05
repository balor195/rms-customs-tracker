package com.rms.customs.domain.usecase

import com.rms.customs.domain.model.Transaction
import com.rms.customs.domain.model.User
import com.rms.customs.domain.model.enums.UserRole
import com.rms.customs.domain.model.enums.toPhase

/**
 * Division/role visibility rule shared by the transaction list, dashboard, and detail screens:
 * Admin/Clearance see everything, Warehouse sees only transactions past clearance, and
 * Supervisor/Viewer/TenderOfficer are confined to their own division.
 */
fun Transaction.isVisibleTo(user: User): Boolean = when (user.role) {
    UserRole.WAREHOUSE -> currentStatus.toPhase().number >= 2
    else -> if (user.role.seesAllDivisions) true else division == user.department
}
