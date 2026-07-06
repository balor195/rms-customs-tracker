package com.rms.customs.domain.usecase

import com.rms.customs.domain.model.User
import com.rms.customs.domain.model.enums.UserRole
import com.rms.customs.domain.repository.UserRepository
import java.util.UUID

sealed class SetupResult {
    data class Success(val user: User) : SetupResult()
    data class Error(val reason: String) : SetupResult()
}

class SetupAdminUseCase(
    private val userRepository: UserRepository,
) {
    suspend operator fun invoke(
        displayNameAr: String,
        displayNameEn: String,
        username: String,
        password: String,
        confirmPassword: String,
    ): SetupResult {
        if (displayNameAr.isBlank() || username.isBlank()) {
            return SetupResult.Error("الاسم واسم المستخدم مطلوبان / Name and username are required")
        }
        if (password.length < 8) {
            return SetupResult.Error("كلمة المرور يجب أن تكون 8 أحرف على الأقل / Password must be at least 8 characters")
        }
        if (password != confirmPassword) {
            return SetupResult.Error("كلمتا المرور غير متطابقتين / Passwords do not match")
        }
        if (userRepository.getByUsername(username.trim()) != null) {
            return SetupResult.Error("اسم المستخدم مستخدم بالفعل / Username already taken")
        }
        val admin = User(
            id = UUID.randomUUID(),
            username = username.trim().lowercase(),
            displayName = displayNameEn.ifBlank { displayNameAr },
            displayNameAr = displayNameAr,
            role = UserRole.ADMIN,
            department = null,
            isActive = true,
        )
        userRepository.create(admin, password)
        return SetupResult.Success(admin)
    }
}
