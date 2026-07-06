package com.rms.customs.presentation.ui

import androidx.compose.runtime.compositionLocalOf
import com.rms.customs.domain.model.User

data class UserSession(
    val user: User,
    val realUser: User? = null,   // set only while ADMIN is temporarily "viewing as" another role, for testing
    val token: String = "",       // populated in Phase 9
) {
    val isViewingAs: Boolean get() = realUser != null
}

val LocalUserSession = compositionLocalOf<UserSession?> { null }
