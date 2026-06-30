package com.rms.customs.data.repository

import com.rms.customs.data.local.dao.UserDao
import com.rms.customs.data.local.entity.toDomain
import com.rms.customs.data.local.entity.toEntity
import com.rms.customs.domain.model.User
import com.rms.customs.domain.model.enums.UserRole
import com.rms.customs.domain.repository.UserRepository
import com.rms.customs.domain.usecase.PasswordHasher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
) : UserRepository {

    override fun observeAll(): Flow<List<User>> =
        userDao.observeAll().map { it.map { e -> e.toDomain() } }

    override suspend fun getById(id: UUID): User? =
        userDao.getById(id.toString())?.toDomain()

    override suspend fun getByUsername(username: String): User? =
        userDao.getByUsername(username)?.toDomain()

    override suspend fun create(user: User, password: String) {
        userDao.insert(user.toEntity(PasswordHasher.hash(password)))
    }

    override suspend fun update(user: User) {
        val existing = userDao.getById(user.id.toString()) ?: error("User not found")
        userDao.update(user.toEntity(existing.passwordHash))
    }

    override suspend fun deactivate(id: UUID) {
        userDao.deactivate(id.toString())
    }

    override suspend fun verifyCredentials(username: String, password: String): User? {
        val entity = userDao.getByUsername(username) ?: return null
        return if (PasswordHasher.verify(password, entity.passwordHash)) entity.toDomain() else null
    }

    override suspend fun updateLastLogin(id: UUID) {
        userDao.updateLastLogin(id.toString(), System.currentTimeMillis())
    }

    override suspend fun updateRole(id: UUID, role: UserRole) {
        userDao.updateRole(id.toString(), role.name)
    }
}
