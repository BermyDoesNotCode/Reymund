package com.example.spendwise.data.plaid

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecureTokenStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_token_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveToken(key: String, token: String) {
        sharedPreferences.edit().putString(key, token).apply()
    }

    fun getToken(key: String): String? {
        return sharedPreferences.getString(key, null)
    }

    fun clearToken(key: String) {
        sharedPreferences.edit().remove(key).apply()
    }

    fun clearAllTokens() {
        sharedPreferences.edit().clear().apply()
    }
} 