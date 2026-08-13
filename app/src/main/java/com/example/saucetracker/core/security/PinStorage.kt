package com.example.saucetracker.core.security

import android.content.Context
import android.util.Base64
import com.example.saucetracker.core.preferences.KEY_APP_LOCK_PIN_HASH
import com.example.saucetracker.core.preferences.KEY_APP_LOCK_PIN_SALT
import com.example.saucetracker.core.preferences.SaucePreferences
import java.security.MessageDigest
import java.security.SecureRandom

internal class PinStorage private constructor(
    private val preferences: SaucePreferences
) {
    data class StoredPin(val hash: String, val salt: String) {
        val configured: Boolean get() = hash.isNotBlank() && salt.isNotBlank()
    }

    fun current(): StoredPin = StoredPin(
        hash = preferences.string(KEY_APP_LOCK_PIN_HASH),
        salt = preferences.string(KEY_APP_LOCK_PIN_SALT)
    )

    fun store(pinInput: String): String? {
        val pin = normalize(pinInput)
        if (pin.isBlank()) return "PIN cannot be empty."
        if (pin.length > MAX_PIN_LENGTH) return "PIN cannot be longer than 20 digits."
        val salt = generateSalt()
        preferences.raw.edit()
            .putString(KEY_APP_LOCK_PIN_SALT, salt)
            .putString(KEY_APP_LOCK_PIN_HASH, hash(pin, salt))
            .apply()
        return null
    }

    fun verify(pinInput: String): Boolean {
        val stored = current()
        if (!stored.configured) return false
        val pin = normalize(pinInput)
        if (pin.isBlank()) return false
        return MessageDigest.isEqual(
            hash(pin, stored.salt).toByteArray(Charsets.UTF_8),
            stored.hash.toByteArray(Charsets.UTF_8)
        )
    }

    fun clear() {
        preferences.raw.edit()
            .putString(KEY_APP_LOCK_PIN_HASH, "")
            .putString(KEY_APP_LOCK_PIN_SALT, "")
            .apply()
    }

    fun normalize(value: String): String = value.filter(Char::isDigit).take(MAX_PIN_LENGTH)

    private fun generateSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    private fun hash(pin: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return Base64.encodeToString(
            digest.digest("$salt:$pin".toByteArray(Charsets.UTF_8)),
            Base64.NO_WRAP
        )
    }

    companion object {
        private const val MAX_PIN_LENGTH = 20

        fun from(context: Context): PinStorage = PinStorage(SaucePreferences.from(context))
    }
}
