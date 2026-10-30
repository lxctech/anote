package com.anote.data.lock

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.Base64

private val Context.lockDataStore by preferencesDataStore(name = "lock_settings")

data class LockConfig(
    val enabled: Boolean = false,
    val biometricEnabled: Boolean = false,
    val hasPin: Boolean = false,
)

class LockStore(context: Context) {
    private val dataStore = context.applicationContext.lockDataStore

    val config: Flow<LockConfig> = dataStore.data.map { prefs ->
        LockConfig(
            enabled = prefs[ENABLED] == true,
            biometricEnabled = prefs[BIOMETRIC] == true,
            hasPin = prefs[SALT] != null && prefs[HASH] != null,
        )
    }.distinctUntilChanged()

    suspend fun setPin(pin: String) {
        val salt = PinHasher.newSalt()
        val hash = withContext(Dispatchers.Default) { PinHasher.hash(pin, salt) }
        dataStore.edit { prefs ->
            prefs[SALT] = Base64.getEncoder().encodeToString(salt)
            prefs[HASH] = Base64.getEncoder().encodeToString(hash)
            prefs[ENABLED] = true
        }
    }

    suspend fun verify(pin: String): Boolean = withContext(Dispatchers.Default) {
        val prefs = dataStore.data.first()
        val salt = decode(prefs, SALT) ?: return@withContext false
        val expected = decode(prefs, HASH) ?: return@withContext false
        PinHasher.verify(pin, salt, expected)
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            if (prefs[ENABLED] == true && prefs[HASH] != null) {
                prefs[BIOMETRIC] = enabled
            }
        }
    }

    suspend fun clear() {
        dataStore.edit { prefs ->
            prefs.remove(SALT)
            prefs.remove(HASH)
            prefs[ENABLED] = false
            prefs[BIOMETRIC] = false
        }
    }

    private fun decode(prefs: Preferences, key: Preferences.Key<String>): ByteArray? =
        prefs[key]?.let { Base64.getDecoder().decode(it) }

    private companion object {
        val ENABLED = booleanPreferencesKey("lock_enabled")
        val BIOMETRIC = booleanPreferencesKey("biometric_enabled")
        val SALT = stringPreferencesKey("pin_salt")
        val HASH = stringPreferencesKey("pin_hash")
    }
}
