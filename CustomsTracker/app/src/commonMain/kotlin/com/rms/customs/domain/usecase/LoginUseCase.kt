package com.rms.customs.domain.usecase

import com.rms.customs.domain.model.User
import com.rms.customs.domain.repository.UserRepository

sealed class LoginResult {
    data class Success(val user: User) : LoginResult()
    object InvalidCredentials : LoginResult()
    object AccountInactive : LoginResult()
}

class LoginUseCase(
    private val userRepository: UserRepository,
) {
    suspend operator fun invoke(username: String, password: String): LoginResult {
        if (username.isBlank() || password.isBlank()) return LoginResult.InvalidCredentials
        val user = userRepository.verifyCredentials(username.trim(), password)
            ?: return LoginResult.InvalidCredentials
        if (!user.isActive) return LoginResult.AccountInactive
        userRepository.updateLastLogin(user.id)
        return LoginResult.Success(user)
    }
}
