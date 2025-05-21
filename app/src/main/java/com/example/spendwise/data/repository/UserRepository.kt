package com.example.spendwise.data.repository

import com.example.spendwise.data.model.User
import com.example.spendwise.data.model.AuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.getValue
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val database: FirebaseDatabase,
    private val auth: FirebaseAuth
) {
    private val usersRef = database.getReference("users")

    suspend fun createUser(user: User): Result<User> = try {
        // Make sure user is authenticated
        val currentUser = auth.currentUser 
            ?: return Result.failure(Exception("Authentication required before creating user profile"))
            
        val newUser = user.copy(
            id = currentUser.uid,
            createdAt = System.currentTimeMillis(),
            lastLoginAt = System.currentTimeMillis()
        )
        
        try {
            usersRef.child(newUser.id).setValue(newUser).await()
            Result.success(newUser)
        } catch (e: Exception) {
            if (e.message?.contains("Permission denied") == true) {
                Result.failure(Exception("Database permission denied. Please update your Firebase Database rules to allow authenticated users to write data."))
            } else {
                Result.failure(e)
            }
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun getUser(userId: String): Flow<User?> = callbackFlow {
        val listener = usersRef.child(userId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val user = snapshot.getValue<User>()
                    trySend(user)
                }

                override fun onCancelled(error: DatabaseError) {
                    trySend(null)
                }
            })

        awaitClose { usersRef.child(userId).removeEventListener(listener) }
    }

    suspend fun updateUser(user: User): Result<User> = try {
        val updatedUser = user.copy(lastLoginAt = System.currentTimeMillis())
        usersRef.child(user.id).setValue(updatedUser).await()
        Result.success(updatedUser)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun updateLastLogin(userId: String): Result<Unit> = try {
        usersRef.child(userId)
            .child("lastLoginAt")
            .setValue(System.currentTimeMillis())
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun deleteUser(userId: String): Result<Unit> = try {
        usersRef.child(userId).removeValue().await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun getCurrentUser(): User? {
        val firebaseUser = auth.currentUser ?: return null
        // Extract username from email (remove @spendwise.app)
        val username = firebaseUser.email?.split("@")?.firstOrNull() ?: ""
        
        return User(
            id = firebaseUser.uid,
            username = username,
            displayName = firebaseUser.displayName ?: "",
            photoUrl = firebaseUser.photoUrl?.toString() ?: "",
            provider = if (firebaseUser.providerData.any { it.providerId == "google.com" }) 
                AuthProvider.GOOGLE else AuthProvider.EMAIL
        )
    }
} 