package com.rms.customs.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rms.customs.data.local.SessionStore
import com.rms.customs.domain.model.enums.Department
import com.rms.customs.domain.model.enums.UserRole
import com.rms.customs.domain.repository.UserRepository
import com.rms.customs.domain.usecase.LoginResult
import com.rms.customs.domain.usecase.LoginUseCase
import com.rms.customs.domain.usecase.SetupAdminUseCase
import com.rms.customs.domain.usecase.SetupResult
import com.rms.customs.presentation.ui.UserSession
import com.rms.customs.presentation.ui.auth.AuthState
import com.rms.customs.presentation.ui.auth.AuthUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val sessionStore: SessionStore,
    private val loginUseCase: LoginUseCase,
    private val setupAdminUseCase: SetupAdminUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        checkSession()
    }

    private fun checkSession() {
        viewModelScope.launch {
            val stored = sessionStore.load()
            if (stored != null) {
                val (userId, _) = stored
                val user = userRepository.getById(userId)
                if (user != null && user.isActive) {
                    _uiState.update { it.copy(authState = AuthState.LoggedIn(UserSession(user))) }
                    return@launch
                }
                sessionStore.clear()
            }
            val hasUsers = userRepository.observeAll().first().isNotEmpty()
            _uiState.update {
                it.copy(authState = if (hasUsers) AuthState.LoggedOut else AuthState.NeedsSetup)
            }
        }
    }

    fun login(username: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessageAr = null, errorMessageEn = null) }
            when (val result = loginUseCase(username, password)) {
                is LoginResult.Success -> {
                    sessionStore.save(result.user.id)
                    _uiState.update {
                        it.copy(
                            authState = AuthState.LoggedIn(UserSession(result.user)),
                            isSubmitting = false,
                        )
                    }
                }
                LoginResult.InvalidCredentials -> _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessageAr = "اسم المستخدم أو كلمة المرور غير صحيحة",
                        errorMessageEn = "Invalid username or password",
                    )
                }
                LoginResult.AccountInactive -> _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessageAr = "الحساب غير نشط. تواصل مع مسؤول النظام.",
                        errorMessageEn = "Account is inactive. Contact your administrator.",
                    )
                }
            }
        }
    }

    fun setupAdmin(
        displayNameAr: String,
        displayNameEn: String,
        username: String,
        password: String,
        confirmPassword: String,
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessageAr = null, errorMessageEn = null) }
            when (val result = setupAdminUseCase(displayNameAr, displayNameEn, username, password, confirmPassword)) {
                is SetupResult.Success -> {
                    sessionStore.save(result.user.id)
                    _uiState.update {
                        it.copy(
                            authState = AuthState.LoggedIn(UserSession(result.user)),
                            isSubmitting = false,
                        )
                    }
                }
                is SetupResult.Error -> _uiState.update {
                    it.copy(isSubmitting = false, errorMessageAr = result.reason)
                }
            }
        }
    }

    fun logout() {
        sessionStore.clear()
        _uiState.update { it.copy(authState = AuthState.LoggedOut, errorMessageAr = null) }
    }

    /**
     * ADMIN-only, in-memory-only role/division impersonation for testing — never persisted to
     * [SessionStore] or the database, and reset automatically on logout or app restart.
     */
    fun viewAs(role: UserRole, department: Department?) {
        val loggedIn = _uiState.value.authState as? AuthState.LoggedIn ?: return
        val session  = loggedIn.session
        val realUser = session.realUser ?: session.user
        if (realUser.role != UserRole.ADMIN) return

        val simulatedUser = realUser.copy(role = role, department = department)
        _uiState.update {
            it.copy(authState = AuthState.LoggedIn(session.copy(user = simulatedUser, realUser = realUser)))
        }
    }

    fun exitViewAs() {
        val loggedIn = _uiState.value.authState as? AuthState.LoggedIn ?: return
        val session  = loggedIn.session
        val realUser = session.realUser ?: return
        _uiState.update {
            it.copy(authState = AuthState.LoggedIn(session.copy(user = realUser, realUser = null)))
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessageAr = null, errorMessageEn = null) }
    }
}
