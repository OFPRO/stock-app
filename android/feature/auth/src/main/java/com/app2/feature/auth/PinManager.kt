package com.app2.feature.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PinManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun isPinSet(): Boolean = prefs.contains(KEY_PIN_HASH)

    fun setPin(pin: String) {
        prefs.edit().putString(KEY_PIN_HASH, hash(pin)).apply()
    }

    fun verifyPin(pin: String): Boolean {
        val storedHash = prefs.getString(KEY_PIN_HASH, null) ?: return false
        return hash(pin) == storedHash
    }

    fun getRemainingAttempts(): Int {
        if (isLocked()) return 0
        return MAX_ATTEMPTS - prefs.getInt(KEY_ATTEMPTS, 0)
    }

    fun registerFailedAttempt() {
        val attempts = prefs.getInt(KEY_ATTEMPTS, 0) + 1
        val lockoutEnd = if (attempts >= MAX_ATTEMPTS) {
            System.currentTimeMillis() + LOCKOUT_DURATION_MS
        } else 0L
        prefs.edit()
            .putInt(KEY_ATTEMPTS, attempts)
            .putLong(KEY_LOCKOUT_END, lockoutEnd)
            .apply()
    }

    fun resetAttempts() {
        prefs.edit()
            .putInt(KEY_ATTEMPTS, 0)
            .remove(KEY_LOCKOUT_END)
            .apply()
    }

    fun isLocked(): Boolean {
        val lockoutEnd = prefs.getLong(KEY_LOCKOUT_END, 0L)
        if (lockoutEnd == 0L) return false
        if (System.currentTimeMillis() >= lockoutEnd) {
            prefs.edit().remove(KEY_LOCKOUT_END).apply()
            return false
        }
        return true
    }

    fun getLockoutRemainingMs(): Long {
        if (!isLocked()) return 0L
        val lockoutEnd = prefs.getLong(KEY_LOCKOUT_END, 0L)
        return (lockoutEnd - System.currentTimeMillis()).coerceAtLeast(0L)
    }

    private fun hash(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(pin.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val PREFS_NAME = "pin_prefs"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_ATTEMPTS = "pin_attempts"
        private const val KEY_LOCKOUT_END = "lockout_end"
        const val MAX_ATTEMPTS = 5
        private const val LOCKOUT_DURATION_MS = 30_000L
    }
}
