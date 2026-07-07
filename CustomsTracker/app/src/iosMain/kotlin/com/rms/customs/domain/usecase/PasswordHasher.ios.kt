package com.rms.customs.domain.usecase

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import platform.Security.SecRandomCopyBytes
import platform.Security.kSecRandomDefault

// The CommonCrypto PBKDF2 call itself is pushed down into per-concrete-target actuals
// (iosArm64Main/iosSimulatorArm64Main - see pbkdf2HmacSha256() below) rather than living here in
// the shared iosMain: CommonCrypto has no Apple-published module map, so it needs a hand-written
// cinterop def declared per target, and that per-target cinterop binding isn't visible from a
// shared intermediate source set like iosMain without cinterop commonization actually completing
// for every target sharing it - which didn't happen reliably for a single-target test invocation
// (:app:iosSimulatorArm64Test only builds that one target's klib). Keeping everything else
// (base64, salt generation via platform.Security - a pre-bound system framework, unlike
// CommonCrypto - and parsing) here in one place, so only the ~15-line primitive is duplicated.
@OptIn(ExperimentalForeignApi::class, ExperimentalEncodingApi::class)
actual object PasswordHasher {
    const val ITERATIONS: UInt = 100_000u
    const val KEY_BYTES = 32 // 256 bits, matches the Android actual's KEY_BITS
    private const val SALT_BYTES = 16

    actual fun hash(password: String): String {
        val salt = ByteArray(SALT_BYTES).also(::fillSecureRandom)
        val derived = derive(password, salt)
        return "${Base64.Default.encode(salt)}:${Base64.Default.encode(derived)}"
    }

    actual fun verify(password: String, stored: String): Boolean {
        val parts = stored.split(":")
        if (parts.size != 2) return false
        val salt = Base64.Default.decode(parts[0])
        val expected = Base64.Default.decode(parts[1])
        val actual = derive(password, salt)
        return actual.contentEquals(expected)
    }

    // Mirrors the Android actual's PBKDF2WithHmacSHA256 / 100_000 iterations / 256-bit key exactly -
    // PBKDF2 is a deterministic standard, so identical algorithm/iteration-count/salt/key-length
    // parameters produce byte-identical output on both platforms, keeping every existing stored
    // password hash valid.
    private fun derive(password: String, salt: ByteArray): ByteArray =
        pbkdf2HmacSha256(password.encodeToByteArray(), salt, ITERATIONS, KEY_BYTES)

    private fun fillSecureRandom(bytes: ByteArray) {
        bytes.usePinned { pinned ->
            SecRandomCopyBytes(kSecRandomDefault, bytes.size.convert(), pinned.addressOf(0))
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
internal expect fun pbkdf2HmacSha256(password: ByteArray, salt: ByteArray, iterations: UInt, keyBytes: Int): ByteArray
