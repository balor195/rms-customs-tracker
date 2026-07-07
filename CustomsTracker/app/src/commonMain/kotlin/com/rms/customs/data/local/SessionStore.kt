package com.rms.customs.data.local

/** Returns (userId, loginTime) if a valid non-expired session exists, null otherwise. */
expect class SessionStore(context: PlatformContext) {
    fun save(userId: String)
    fun load(): Pair<String, Long>?
    fun clear()
}
