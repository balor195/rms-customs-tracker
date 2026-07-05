package com.rms.customs.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.rms.customs.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Query("SELECT * FROM users WHERE isActive = 1 ORDER BY displayName")
    fun observeAll(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getById(id: String): UserEntity?

    @Query("SELECT * FROM users WHERE username = :username AND isActive = 1 LIMIT 1")
    suspend fun getByUsername(username: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: UserEntity)

    @Update
    suspend fun update(entity: UserEntity)

    @Query("UPDATE users SET isActive = 0 WHERE id = :id")
    suspend fun deactivate(id: String)

    @Query("UPDATE users SET lastLoginAt = :timestamp WHERE id = :id")
    suspend fun updateLastLogin(id: String, timestamp: Long)

    @Query("UPDATE users SET role = :role, department = :department WHERE id = :id")
    suspend fun updateRole(id: String, role: String, department: String?)
}
