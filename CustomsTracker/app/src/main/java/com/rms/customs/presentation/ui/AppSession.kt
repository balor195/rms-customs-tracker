package com.rms.customs.presentation.ui

import androidx.compose.runtime.compositionLocalOf
import com.rms.customs.domain.model.User

data class UserSession(
    val user: User,
    val token: String = "",       // populated in Phase 9
)

val LocalUserSession = compositionLocalOf<UserSession?> { null }
