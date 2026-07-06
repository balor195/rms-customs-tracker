package com.rms.customs.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rms.customs.domain.model.User
import com.rms.customs.domain.model.enums.Department
import com.rms.customs.domain.model.enums.UserRole
import com.rms.customs.domain.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class CreateUserForm(
    val displayNameEn: String = "",
    val displayNameAr: String = "",
    val username: String      = "",
    val password: String      = "",
    val role: UserRole        = UserRole.SUPERVISOR,
    val department: Department = Department.PHARMACY,
)

class UserManagementViewModel(
    private val userRepository: UserRepository,
) : ViewModel() {

    val users: StateFlow<List<User>> = userRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    @OptIn(ExperimentalUuidApi::class)
    fun createUser(form: CreateUserForm) = viewModelScope.launch {
        if (form.displayNameAr.isBlank() || form.username.isBlank() || form.password.length < 8) {
            _errorMessage.value = "الاسم والمستخدم مطلوبان. كلمة المرور 8 أحرف على الأقل."
            return@launch
        }
        if (form.username.contains(" ")) {
            _errorMessage.value = "اسم المستخدم لا يحتوي على مسافات."
            return@launch
        }
        runCatching {
            val user = User(
                id            = Uuid.random().toString(),
                username      = form.username.lowercase().trim(),
                displayName   = form.displayNameEn.ifBlank { form.displayNameAr },
                displayNameAr = form.displayNameAr,
                role          = form.role,
                department    = if (form.role.seesAllDivisions) null else form.department,
                isActive      = true,
            )
            userRepository.create(user, form.password)
        }.onSuccess {
            _successMessage.value = "تم إنشاء المستخدم بنجاح"
        }.onFailure { e ->
            _errorMessage.value = if (e.message?.contains("UNIQUE", ignoreCase = true) == true)
                "اسم المستخدم موجود مسبقاً" else "خطأ: ${e.message}"
        }
    }

    fun updateRole(userId: String, role: UserRole, department: Department?) = viewModelScope.launch {
        val resolvedDepartment = if (role.seesAllDivisions) null else department
        runCatching { userRepository.updateRole(userId, role, resolvedDepartment) }
            .onSuccess  { _successMessage.value = "تم تحديث الدور" }
            .onFailure  { _errorMessage.value   = "فشل تحديث الدور" }
    }

    fun deactivate(userId: String) = viewModelScope.launch {
        runCatching { userRepository.deactivate(userId) }
            .onSuccess  { _successMessage.value = "تم تعطيل المستخدم" }
            .onFailure  { _errorMessage.value   = "فشل تعطيل المستخدم" }
    }

    fun clearMessages() {
        _errorMessage.value   = null
        _successMessage.value = null
    }
}
