package com.rms.customs.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.rms.customs.domain.model.User
import com.rms.customs.domain.model.enums.Department
import com.rms.customs.domain.model.enums.UserRole

@Entity(
    tableName = "users",
    indices = [Index(value = ["username"], unique = true)]
)
data class UserEntity(
    @PrimaryKey val id: String,
    val username: String,
    val displayName: String,
    val displayNameAr: String,
    val role: String,                 // UserRole.name
    val department: String?,          // Department.name, or null (ADMIN/CLEARANCE/WAREHOUSE)
    val passwordHash: String,
    val isActive: Boolean,
    val lastLoginAt: Long?,
)

fun UserEntity.toDomain() = User(
    id = id,
    username = username,
    displayName = displayName,
    displayNameAr = displayNameAr,
    role = UserRole.valueOf(role),
    department = department?.let { Department.valueOf(it) },
    isActive = isActive,
    lastLoginAt = lastLoginAt,
)

fun User.toEntity(passwordHash: String) = UserEntity(
    id = id,
    username = username,
    displayName = displayName,
    displayNameAr = displayNameAr,
    role = role.name,
    department = department?.name,
    passwordHash = passwordHash,
    isActive = isActive,
    lastLoginAt = lastLoginAt,
)
