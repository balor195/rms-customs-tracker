package com.rms.customs.data.local

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

// iosTest, not commonTest: the Android actual needs a real android.content.Context (only available
// via Robolectric or an instrumented test, neither set up in this project - the original Android
// SessionStore never had a test either), while Keychain Services runs fine in a plain Kotlin/Native
// test binary on the iOS Simulator, same as PasswordHasherTest did in Phase 4a.
class SessionStoreTest {

    private val sessionStore = SessionStore(PlatformContext())

    @AfterTest
    fun tearDown() {
        sessionStore.clear()
    }

    @Test
    fun `save then load returns the saved userId`() {
        sessionStore.save("user-123")
        val loaded = sessionStore.load()
        assertEquals("user-123", loaded?.first)
    }

    @Test
    fun `clear empties the session`() {
        sessionStore.save("user-123")
        sessionStore.clear()
        assertNull(sessionStore.load())
    }

    @Test
    fun `load returns null when nothing was ever saved`() {
        assertNull(sessionStore.load())
    }
}
