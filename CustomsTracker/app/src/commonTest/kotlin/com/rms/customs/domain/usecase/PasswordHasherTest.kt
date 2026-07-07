package com.rms.customs.domain.usecase

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class PasswordHasherTest {

    @Test
    fun `hash then verify round-trips for the correct password`() {
        val stored = PasswordHasher.hash("s3cr3t-P@ssw0rd")
        assertTrue(PasswordHasher.verify("s3cr3t-P@ssw0rd", stored))
    }

    @Test
    fun `verify rejects the wrong password`() {
        val stored = PasswordHasher.hash("s3cr3t-P@ssw0rd")
        assertFalse(PasswordHasher.verify("wrong-password", stored))
    }

    @Test
    fun `hash uses a fresh random salt each call`() {
        val first = PasswordHasher.hash("same-password")
        val second = PasswordHasher.hash("same-password")
        assertNotEquals(first, second)
    }

    // Independent known-answer test: this salt/hash pair was computed with Python's
    // hashlib.pbkdf2_hmac("sha256", ..., iterations=100_000, dklen=32) - a third, independent
    // PBKDF2-HMAC-SHA256 implementation - not derived from either platform's own actual. Guards
    // against the Android (javax.crypto) and iOS (CommonCrypto) actuals silently drifting from the
    // PBKDF2 standard's parameters (algorithm/iteration-count/salt-length/key-length) in a way that
    // happened to still agree with each other but not with the standard.
    @Test
    fun `verify accepts an independently-computed PBKDF2-HMAC-SHA256 known vector`() {
        val stored = "AAECAwQFBgcICQoLDA0ODw==:NPuAtx40doeWgqg/HeWmEBabNy24omBZwAAb7zikxz0="
        assertTrue(PasswordHasher.verify("CorrectHorseBatteryStaple", stored))
        assertFalse(PasswordHasher.verify("wrong-password", stored))
    }
}
