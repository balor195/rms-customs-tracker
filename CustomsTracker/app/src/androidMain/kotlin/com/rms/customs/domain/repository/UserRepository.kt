package com.rms.customs.domain.repository

import com.rms.customs.domain.model.User
import com.rms.customs.domain.model.enums.Department
import com.rms.customs.domain.model.enums.UserRole
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface UserRepository {
    fun observeAll(): Flow<List<User>>
    suspend fun getById(id: UUID): User?
    suspend fun getByUsername(username: String): User?
    suspend fun create(user: User, password: String)
    suspend fun update(user: User)
    suspend fun deactivate(id: UUID)
    suspend fun verifyCredentials(username: String, password: String): User?
    suspend fun updateLastLogin(id: UUID)
    suspend fun updateRole(id: UUID, role: UserRole, department: Department?)
}
