package com.raul1x9.mineconsole.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

public final class SecurityHelper private constructor(context: Context) {
    private val sharedPreferences: SharedPreferences

    init {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        sharedPreferences = EncryptedSharedPreferences.create(
            context,
            "mineconsole_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    public fun saveString(key: String, value: String): Boolean {
        return try {
            sharedPreferences.edit().putString(key, value).commit()
        } catch (e: Exception) {
            false
        }
    }

    public fun readString(key: String): String? {
        return sharedPreferences.getString(key, null)
    }

    public fun delete(key: String) {
        sharedPreferences.edit().remove(key).apply()
    }

    companion object {
        @Volatile
        private var INSTANCE: SecurityHelper? = null

        fun getInstance(context: Context): SecurityHelper {
            return INSTANCE ?: synchronized(this) {
                val instance = SecurityHelper(context)
                INSTANCE = instance
                instance
            }
        }
    }
}
