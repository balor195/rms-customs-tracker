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
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFStringRef
import platform.CoreFoundation.CFTypeRefVar
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
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
// (CPointer<__CFString>?), and NSMutableDictionary is Kotlin/Native's own NSDictionaryAsKMap
// wrapper. Both directions of "the same bytes, different static Kotlin type" toll-free bridging
// need an *explicit* bridge function call - a plain `as` cast compiles but throws a
// ClassCastException at runtime, since Kotlin/Native's object model doesn't recognize CF/NS
// equivalence without going through CFBridgingRelease (CF pointer -> NS object, used for the
// immortal kSec* constants below - the ownership-transfer semantics of "Release" don't matter for
// process-wide singletons) or CFBridgingRetain (NS object -> CF pointer, used for the
// freshly-built query dictionaries, which we do own and must CFRelease after the Security call).
@OptIn(ExperimentalForeignApi::class)
private fun CFStringRef?.asNSString(): NSString = CFBridgingRelease(this) as NSString

@OptIn(ExperimentalForeignApi::class)
private fun NSMutableDictionary.asCFDictionary(): CFDictionaryRef = CFBridgingRetain(this) as CFDictionaryRef

@OptIn(ExperimentalForeignApi::class)
actual class SessionStore actual constructor(context: PlatformContext) {

    actual fun save(userId: String) {
        deleteItem()
        val value = "$userId:${Clock.System.now().toEpochMilliseconds()}"
        val query = baseQuery()
        query.setObject(value.toNSData(), forKey = kSecValueData.asNSString())
        val cfQuery = query.asCFDictionary()
        SecItemAdd(cfQuery, null)
        CFRelease(cfQuery)
    }

    actual fun load(): Pair<String, Long>? {
        val query = baseQuery()
        query.setObject(true, forKey = kSecReturnData.asNSString())
        query.setObject(kSecMatchLimitOne.asNSString(), forKey = kSecMatchLimit.asNSString())
        val cfQuery = query.asCFDictionary()
        val data = memScoped {
            val result = alloc<CFTypeRefVar>()
            val status = SecItemCopyMatching(cfQuery, result.ptr)
            CFRelease(cfQuery)
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
        val cfQuery = baseQuery().asCFDictionary()
        SecItemDelete(cfQuery)
        CFRelease(cfQuery)
    }

    private fun baseQuery(): NSMutableDictionary {
        val query = NSMutableDictionary()
        query.setObject(kSecClassGenericPassword.asNSString(), forKey = kSecClass.asNSString())
        query.setObject(SESSION_SERVICE, forKey = kSecAttrService.asNSString())
        query.setObject(SESSION_ACCOUNT, forKey = kSecAttrAccount.asNSString())
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
