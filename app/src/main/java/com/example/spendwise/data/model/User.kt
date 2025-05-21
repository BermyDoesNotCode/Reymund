package com.example.spendwise.data.model

import com.google.firebase.database.Exclude
import java.util.Date

data class User(
    val id: String = "",
    val username: String = "",
    val displayName: String = "",
    val photoUrl: String = "",
    val provider: AuthProvider = AuthProvider.EMAIL,
    val createdAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long = System.currentTimeMillis()
) {
    @get:Exclude
    val createdAtAsDate: Date
        get() = Date(createdAt)

    @get:Exclude
    val lastLoginAtAsDate: Date
        get() = Date(lastLoginAt)
}

enum class AuthProvider {
    EMAIL,
    GOOGLE
} 