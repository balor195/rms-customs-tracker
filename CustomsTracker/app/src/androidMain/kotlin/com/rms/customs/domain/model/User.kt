package com.rms.customs.domain.model

import com.rms.customs.domain.model.enums.Department
import com.rms.customs.domain.model.enums.UserRole

data class User(
    val id: String,
    val username: String,
    val displayName: String,
    val displayNameAr: String,
    val role: UserRole,
    val department: Department?,      // null for roles with seesAllDivisions (ADMIN/CLEARANCE/WAREHOUSE)
    val isActive: Boolean = true,
    val lastLoginAt: Long? = null,
)
