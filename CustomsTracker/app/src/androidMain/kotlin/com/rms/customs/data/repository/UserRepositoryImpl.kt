package com.rms.customs.data.repository

import com.rms.customs.data.local.dao.UserDao
import com.rms.customs.data.local.entity.toDomain
import com.rms.customs.data.local.entity.toEntity
import com.rms.customs.domain.model.User
import com.rms.customs.domain.model.enums.Department
import com.rms.customs.domain.model.enums.UserRole
import com.rms.customs.domain.repository.UserRepository
import com.rms.customs.domain.usecase.PasswordHasher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UserRepositoryImpl(
    private val userDao: UserDao,
) : UserRepository {

    override fun observeAll(): Flow<List<User>> =
        userDao.observeAll().map { it.map { e -> e.toDomain() } }

    override suspend fun getById(id: String): User? =
        userDao.getById(id)?.toDomain()

    override suspend fun getByUsername(username: String): User? =
        userDao.getByUsername(username)?.toDomain()

    override suspend fun create(user: User, password: String) {
        userDao.insert(user.toEntity(PasswordHasher.hash(password)))
    }

    override suspend fun update(user: User) {
        val existing = userDao.getById(user.id) ?: error("User not found")
        userDao.update(user.toEntity(existing.passwordHash))
    }

    override suspend fun deactivate(id: String) {
        userDao.deactivate(id)
    }

    override suspend fun verifyCredentials(username: String, password: String): User? {
        val entity = userDao.getByUsername(username) ?: return null
        return if (PasswordHasher.verify(password, entity.passwordHash)) entity.toDomain() else null
    }

    override suspend fun updateLastLogin(id: String) {
        userDao.updateLastLogin(id, System.currentTimeMillis())
    }

    override suspend fun updateRole(id: String, role: UserRole, department: Department?) {
        userDao.updateRole(id, role.name, department?.name)
    }
}
