package com.rms.customs.domain.usecase

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import platform.CoreCrypto.CCKeyDerivationPBKDF
import platform.CoreCrypto.kCCPBKDF2
import platform.CoreCrypto.kCCPRFHmacAlgSHA256
import platform.Security.SecRandomCopyBytes
import platform.Security.kSecRandomDefault

// CommonCrypto ships as a Kotlin/Native *built-in* platform library (kotlin-native's own
// platformLibs/src/platform/ios/CommonCrypto.def), no custom cinterop needed - like
// platform.Foundation/platform.Security, it's usable directly from the shared iosMain. The
// package is platform.CoreCrypto, not platform.CommonCrypto (a historical naming quirk in
// Kotlin/Native's own binding) - confirmed against Kotlin's source after a hand-written custom
// cinterop attempt (package = platform.CommonCrypto, headers = CommonCrypto/CommonKeyDerivation.h)
// resolved the package but not its symbols, since a single-header include doesn't reliably expose
// everything a proper `modules = CommonCrypto` Clang-module import does.
@OptIn(ExperimentalForeignApi::class, ExperimentalEncodingApi::class)
actual object PasswordHasher {
    private const val ITERATIONS: UInt = 100_000u
    private const val KEY_BYTES = 32 // 256 bits, matches the Android actual's KEY_BITS
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
    private fun derive(password: String, salt: ByteArray): ByteArray {
        val passwordBytes = password.encodeToByteArray()
        val derivedKey = ByteArray(KEY_BYTES)
        passwordBytes.usePinned { passwordPinned ->
            salt.usePinned { saltPinned ->
                derivedKey.usePinned { keyPinned ->
                    CCKeyDerivationPBKDF(
                        algorithm = kCCPBKDF2.convert(),
                        password = passwordPinned.addressOf(0),
                        passwordLen = passwordBytes.size.convert(),
                        salt = saltPinned.addressOf(0).reinterpret(),
                        saltLen = salt.size.convert(),
                        prf = kCCPRFHmacAlgSHA256.convert(),
                        rounds = ITERATIONS,
                        derivedKey = keyPinned.addressOf(0).reinterpret(),
                        derivedKeyLen = derivedKey.size.convert(),
                    )
                }
            }
        }
        return derivedKey
    }

    private fun fillSecureRandom(bytes: ByteArray) {
        bytes.usePinned { pinned ->
            SecRandomCopyBytes(kSecRandomDefault, bytes.size.convert(), pinned.addressOf(0))
        }
    }
}
