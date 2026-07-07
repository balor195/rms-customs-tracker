package com.rms.customs.data.local

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.datetime.Clock
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFTypeRefVar
import platform.Foundation.NSData
import platform.Foundation.NSMutableDictionary
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
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

private const val SESSION_SERVICE = "com.rms.customs.session"
private const val SESSION_ACCOUNT = "session"
private const val SESSION_TTL_MS = 8L * 3600 * 1000 // 8 hours, matches the Android actual

// Session (userId + loginTime) is stored as one colon-joined "$userId:$loginTime" string in a
// single Keychain item, mirroring PasswordHasher's own "salt:hash" convention from Phase 4a.
@OptIn(ExperimentalForeignApi::class)
actual class SessionStore actual constructor(context: PlatformContext) {

    actual fun save(userId: String) {
        deleteItem()
        val value = "$userId:${Clock.System.now().toEpochMilliseconds()}"
        val data = (value as NSString).dataUsingEncoding(NSUTF8StringEncoding) ?: return
        val query = baseQuery()
        query.setObject(data, forKey = kSecValueData)
        SecItemAdd(query as CFDictionaryRef, null)
    }

    actual fun load(): Pair<String, Long>? {
        val query = baseQuery()
        query.setObject(true, forKey = kSecReturnData)
        query.setObject(kSecMatchLimitOne, forKey = kSecMatchLimit)
        val data = memScoped {
            val result = alloc<CFTypeRefVar>()
            val status = SecItemCopyMatching(query as CFDictionaryRef, result.ptr)
            if (status != errSecSuccess) return null
            result.value as? NSData
        } ?: return null
        val stored = NSString.create(data = data, encoding = NSUTF8StringEncoding) as? String ?: return null
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
        query.setObject(kSecClassGenericPassword, forKey = kSecClass)
        query.setObject(SESSION_SERVICE, forKey = kSecAttrService)
        query.setObject(SESSION_ACCOUNT, forKey = kSecAttrAccount)
        return query
    }
}
