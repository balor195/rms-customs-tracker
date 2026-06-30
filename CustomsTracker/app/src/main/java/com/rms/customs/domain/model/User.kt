package com.rms.customs.domain.model

import com.rms.customs.domain.model.enums.Department
import com.rms.customs.domain.model.enums.UserRole
import java.util.UUID

data class User(
    val id: UUID,
    val username: String,
    val displayName: String,
    val displayNameAr: String,
    val role: UserRole,
    val department: Department,
    val isActive: Boolean = true,
    val lastLoginAt: Long? = null,
)
