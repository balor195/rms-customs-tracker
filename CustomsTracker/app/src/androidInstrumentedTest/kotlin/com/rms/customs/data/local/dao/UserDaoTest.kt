package com.rms.customs.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rms.customs.data.local.db.CustomsDatabase
import com.rms.customs.data.local.entity.UserEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class UserDaoTest {

    private lateinit var db: CustomsDatabase
    private lateinit var dao: UserDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder<CustomsDatabase>(context)
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .allowMainThreadQueries()
            .build()
        dao = db.userDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    private fun makeUser(
        username: String = "user_${UUID.randomUUID().toString().take(4)}",
        role: String = "COORDINATOR",
        passwordHash: String = "hash_abc123",
    ) = UserEntity(
        id = UUID.randomUUID().toString(),
        username = username,
        displayName = "Test User",
        displayNameAr = "مستخدم تجريبي",
        role = role,
        department = "PHARMACY",
        passwordHash = passwordHash,
        isActive = true,
        lastLoginAt = null,
    )

    @Test
    fun insertAndObserveAll_returnsUser() = runTest {
        dao.insert(makeUser(username = "alice"))

        val users = dao.observeAll().first()
        assertEquals(1, users.size)
        assertEquals("alice", users[0].username)
    }

    @Test
    fun getByUsername_returnsCorrectUser() = runTest {
        dao.insert(makeUser(username = "bob"))

        val user = dao.getByUsername("bob")
        assertNotNull(user)
        assertEquals("bob", user!!.username)
    }

    @Test
    fun getByUsername_returnsNullForMissingUser() = runTest {
        val user = dao.getByUsername("nobody")
        assertNull(user)
    }

    @Test
    fun getByUsername_returnsStoredPasswordHash() = runTest {
        dao.insert(makeUser(username = "charlie", passwordHash = "correct_hash"))

        // Hash verification itself is a UserRepositoryImpl/PasswordHasher concern, not the
        // DAO's — this only confirms the DAO round-trips the stored hash correctly.
        val result = dao.getByUsername("charlie")
        assertNotNull(result)
        assertEquals("correct_hash", result!!.passwordHash)
    }

    @Test
    fun deactivate_removesUserFromObserveAll() = runTest {
        val user = makeUser(username = "eve")
        dao.insert(user)
        dao.deactivate(user.id)

        val users = dao.observeAll().first()
        assertEquals(0, users.size)
    }

    @Test
    fun updateLastLogin_setsTimestamp() = runTest {
        val user = makeUser(username = "frank")
        dao.insert(user)

        val timestamp = System.currentTimeMillis()
        dao.updateLastLogin(user.id, timestamp)

        val fetched = dao.getById(user.id)
        assertNotNull(fetched!!.lastLoginAt)
        assertEquals(timestamp, fetched.lastLoginAt)
    }

    @Test
    fun observeAll_excludesInactiveUsers() = runTest {
        dao.insert(makeUser(username = "active_user"))
        val inactiveUser = makeUser(username = "inactive_user")
        dao.insert(inactiveUser)
        dao.deactivate(inactiveUser.id)

        val users = dao.observeAll().first()
        assertEquals(1, users.size)
        assertEquals("active_user", users[0].username)
    }
}
