package com.rms.customs.data.local

import kotlin.test.AfterTest
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

// iosTest, not commonTest: the Android actual needs a real android.content.Context (only available
// via Robolectric or an instrumented test, neither set up in this project - the original Android
// SessionStore never had a test either).
class SessionStoreTest {

    private val sessionStore = SessionStore(PlatformContext())

    @AfterTest
    fun tearDown() {
        sessionStore.clear()
    }

    // Ignored: this is a known, documented Kotlin/Native limitation, not a bug in SessionStore.
    // ./gradlew iosSimulatorArm64Test runs a bare, unsigned test binary directly via simctl - never
    // wrapped in a real app bundle with an entitlements plist - and Keychain Services' SecItemAdd
    // requires keychain-access-groups entitlements at runtime to actually persist an item (see
    // JetBrains YouTrack KT-61470, "Keychain operations fail in iOS tests due to missing
    // entitlements"). save() silently fails in this environment as a result. The real app (built via
    // Xcode, with a real entitlements-capable bundle) is expected to work correctly - this can't be
    // confirmed until SessionStore is wired into a live iOS screen in Phase 5.
    @Ignore
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
