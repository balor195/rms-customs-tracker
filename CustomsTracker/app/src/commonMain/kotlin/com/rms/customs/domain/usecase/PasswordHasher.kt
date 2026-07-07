package com.rms.customs.domain.usecase

expect object PasswordHasher {
    fun hash(password: String): String
    fun verify(password: String, stored: String): Boolean
}
