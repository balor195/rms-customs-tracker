package com.rms.customs.presentation.ui.auth

import com.rms.customs.presentation.ui.UserSession

sealed class AuthState {
    object Loading    : AuthState()
    object NeedsSetup : AuthState()
    object LoggedOut  : AuthState()
    data class LoggedIn(val session: UserSession) : AuthState()
}

data class AuthUiState(
    val authState: AuthState    = AuthState.Loading,
    val isSubmitting: Boolean   = false,
    val errorMessageAr: String? = null,
    val errorMessageEn: String? = null,
)
