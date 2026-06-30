package com.rms.customs.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private const val SESSION_FILE   = "customs_session"
private const val KEY_USER_ID    = "user_id"
private const val KEY_LOGIN_TIME = "login_time"
private const val SESSION_TTL_MS = 8L * 3600 * 1000   // 8 hours

@Singleton
class SessionStore @Inject constructor(@ApplicationContext context: Context) {

    private val prefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            SESSION_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun save(userId: UUID) {
        prefs.edit()
            .putString(KEY_USER_ID, userId.toString())
            .putLong(KEY_LOGIN_TIME, System.currentTimeMillis())
            .apply()
    }

    /** Returns (userId, loginTime) if a valid non-expired session exists, null otherwise. */
    fun load(): Pair<UUID, Long>? {
        val idStr     = prefs.getString(KEY_USER_ID, null) ?: return null
        val loginTime = prefs.getLong(KEY_LOGIN_TIME, 0L)
        if (System.currentTimeMillis() - loginTime > SESSION_TTL_MS) {
            clear()
            return null
        }
        return Pair(UUID.fromString(idStr), loginTime)
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
