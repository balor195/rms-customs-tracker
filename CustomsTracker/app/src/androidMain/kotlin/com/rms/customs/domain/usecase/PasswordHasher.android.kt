package com.rms.customs.domain.usecase

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

actual object PasswordHasher {
    private const val ALGORITHM  = "PBKDF2WithHmacSHA256"
    private const val ITERATIONS = 100_000
    private const val KEY_BITS   = 256
    private const val SALT_BYTES = 16

    actual fun hash(password: String): String {
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val hash = derive(password, salt)
        val enc = Base64.getEncoder()
        return "${enc.encodeToString(salt)}:${enc.encodeToString(hash)}"
    }

    actual fun verify(password: String, stored: String): Boolean {
        val parts = stored.split(":")
        if (parts.size != 2) return false
        val dec  = Base64.getDecoder()
        val salt = dec.decode(parts[0])
        val expected = dec.decode(parts[1])
        val actual   = derive(password, salt)
        return actual.contentEquals(expected)
    }

    private fun derive(password: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_BITS)
        return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded
    }
}
