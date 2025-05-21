package com.example.spendwise.data.repository

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecureTokenStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Using regular SharedPreferences for now to eliminate encryption-related crashes
    // In production, you would want to use EncryptedSharedPreferences
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        "token_prefs",
        Context.MODE_PRIVATE
    )

    fun saveToken(key: String, value: String) {
        sharedPreferences.edit().putString(key, value).apply()
    }

    fun getToken(key: String): String? {
        return sharedPreferences.getString(key, null)
    }

    fun removeToken(key: String) {
        sharedPreferences.edit().remove(key).apply()
    }

    fun clearTokens() {
        sharedPreferences.edit().clear().apply()
    }
} 