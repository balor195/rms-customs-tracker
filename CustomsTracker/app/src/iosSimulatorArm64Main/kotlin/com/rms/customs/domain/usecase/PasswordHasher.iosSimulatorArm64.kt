package com.rms.customs.domain.usecase

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import platform.CommonCrypto.CCKeyDerivationPBKDF
import platform.CommonCrypto.kCCPBKDF2
import platform.CommonCrypto.kCCPRFHmacAlgSHA256

// Duplicated verbatim in iosArm64Main/PasswordHasher.iosArm64.kt - the commonCrypto cinterop klib
// is per-target and isn't visible from the shared iosMain source set (see the comment on
// PasswordHasher.ios.kt), so this ~15-line primitive can't be deduplicated without cinterop
// commonization, which didn't work reliably for single-target builds/tests.
@OptIn(ExperimentalForeignApi::class)
internal actual fun pbkdf2HmacSha256(password: ByteArray, salt: ByteArray, iterations: UInt, keyBytes: Int): ByteArray {
    val derivedKey = ByteArray(keyBytes)
    password.usePinned { passwordPinned ->
        salt.usePinned { saltPinned ->
            derivedKey.usePinned { keyPinned ->
                CCKeyDerivationPBKDF(
                    algorithm = kCCPBKDF2.convert(),
                    password = passwordPinned.addressOf(0),
                    passwordLen = password.size.convert(),
                    salt = saltPinned.addressOf(0).reinterpret(),
                    saltLen = salt.size.convert(),
                    prf = kCCPRFHmacAlgSHA256.convert(),
                    rounds = iterations,
                    derivedKey = keyPinned.addressOf(0).reinterpret(),
                    derivedKeyLen = derivedKey.size.convert(),
                )
            }
        }
    }
    return derivedKey
}
