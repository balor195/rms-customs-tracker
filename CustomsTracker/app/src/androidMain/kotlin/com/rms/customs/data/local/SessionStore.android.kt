package com.rms.customs.data.local

import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

// Not a typealias to android.content.Context directly: Context is abstract, which conflicts
// with the expect class's implicit final modality. A thin wrapper sidesteps that entirely.
actual class PlatformContext(val context: android.content.Context)

private const val SESSION_FILE   = "customs_session"
private const val KEY_USER_ID    = "user_id"
private const val KEY_LOGIN_TIME = "login_time"
private const val SESSION_TTL_MS = 8L * 3600 * 1000   // 8 hours

actual class SessionStore actual constructor(context: PlatformContext) {
    private val androidContext = context.context

    private val prefs by lazy {
        val masterKey = MasterKey.Builder(androidContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            androidContext,
            SESSION_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    actual fun save(userId: String) {
        prefs.edit()
            .putString(KEY_USER_ID, userId)
            .putLong(KEY_LOGIN_TIME, System.currentTimeMillis())
            .apply()
    }

    actual fun load(): Pair<String, Long>? {
        val idStr     = prefs.getString(KEY_USER_ID, null) ?: return null
        val loginTime = prefs.getLong(KEY_LOGIN_TIME, 0L)
        if (System.currentTimeMillis() - loginTime > SESSION_TTL_MS) {
            clear()
            return null
        }
        return Pair(idStr, loginTime)
    }

    actual fun clear() {
        prefs.edit().clear().apply()
    }
}
