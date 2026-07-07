package com.rms.customs.data.local

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import kotlinx.datetime.Clock
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFTypeRefVar
import platform.Foundation.NSData
import platform.Foundation.NSMutableDictionary
import platform.Foundation.NSString
import platform.Foundation.create
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData
import platform.posix.memcpy

private const val SESSION_SERVICE = "com.rms.customs.session"
private const val SESSION_ACCOUNT = "session"
private const val SESSION_TTL_MS = 8L * 3600 * 1000 // 8 hours, matches the Android actual

// Session (userId + loginTime) is stored as one colon-joined "$userId:$loginTime" string in a
// single Keychain item, mirroring PasswordHasher's own "salt:hash" convention from Phase 4a.
//
// kSec* constants (kSecClass, kSecAttrService, ...) are raw CFStringRef pointers
// (CPointer<__CFString>?), not NSCopyingProtocol-conforming objects - NSMutableDictionary.setObject
// needs an explicit `as NSString` cast on them (CFStringRef/NSString are toll-free bridged at
// runtime, but Kotlin/Native doesn't apply that bridging implicitly).
@OptIn(ExperimentalForeignApi::class)
actual class SessionStore actual constructor(context: PlatformContext) {

    actual fun save(userId: String) {
        deleteItem()
        val value = "$userId:${Clock.System.now().toEpochMilliseconds()}"
        val query = baseQuery()
        query.setObject(value.toNSData(), forKey = kSecValueData as NSString)
        SecItemAdd(query as CFDictionaryRef, null)
    }

    actual fun load(): Pair<String, Long>? {
        val query = baseQuery()
        query.setObject(true, forKey = kSecReturnData as NSString)
        query.setObject(kSecMatchLimitOne as NSString, forKey = kSecMatchLimit as NSString)
        val data = memScoped {
            val result = alloc<CFTypeRefVar>()
            val status = SecItemCopyMatching(query as CFDictionaryRef, result.ptr)
            if (status != errSecSuccess) return null
            result.value as? NSData
        } ?: return null
        val stored = data.toKotlinString()
        val parts = stored.split(":")
        if (parts.size != 2) return null
        val loginTime = parts[1].toLongOrNull() ?: return null
        if (Clock.System.now().toEpochMilliseconds() - loginTime > SESSION_TTL_MS) {
            clear()
            return null
        }
        return Pair(parts[0], loginTime)
    }

    actual fun clear() {
        deleteItem()
    }

    private fun deleteItem() {
        SecItemDelete(baseQuery() as CFDictionaryRef)
    }

    private fun baseQuery(): NSMutableDictionary {
        val query = NSMutableDictionary()
        query.setObject(kSecClassGenericPassword as NSString, forKey = kSecClass as NSString)
        query.setObject(SESSION_SERVICE, forKey = kSecAttrService as NSString)
        query.setObject(SESSION_ACCOUNT, forKey = kSecAttrAccount as NSString)
        return query
    }
}

// Raw byte-buffer conversion, avoiding uncertainty around NSString's exact Kotlin-generated
// dataUsingEncoding/create(data:encoding:) binding signatures.
@OptIn(ExperimentalForeignApi::class)
private fun String.toNSData(): NSData {
    val bytes = encodeToByteArray()
    return bytes.usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = bytes.size.convert())
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toKotlinString(): String {
    val byteArray = ByteArray(length.convert())
    if (byteArray.isNotEmpty()) {
        byteArray.usePinned { pinned ->
            memcpy(pinned.addressOf(0), bytes, length)
        }
    }
    return byteArray.decodeToString()
}
