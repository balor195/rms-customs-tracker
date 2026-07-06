package com.rms.customs.presentation.ui

import androidx.compose.runtime.Composable
import com.rms.customs.domain.model.enums.UserRole

@Composable
fun RequireRole(
    vararg roles: UserRole,
    fallback: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    val session = LocalUserSession.current
    if (session != null && session.user.role in roles) {
        content()
    } else {
        fallback()
    }
}
