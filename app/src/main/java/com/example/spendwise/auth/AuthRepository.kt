package com.example.spendwise.auth

import com.example.spendwise.data.model.AuthResult
import com.example.spendwise.data.repository.SecureTokenStorage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

interface AuthRepository {
    suspend fun signInWithUsername(username: String, password: String): AuthResult
    suspend fun signInWithGoogle(idToken: String): AuthResult
    suspend fun register(username: String, password: String): AuthResult
    suspend fun signOut()
    fun isUserSignedIn(): Boolean
    fun getCurrentUser(): String?
}

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val tokenStorage: SecureTokenStorage
) : AuthRepository {

    override suspend fun signInWithUsername(username: String, password: String): AuthResult {
        return try {
            // Since Firebase requires email, we're appending a domain to the username
            // to simulate username-based auth while using Firebase's email auth
            val email = "$username@spendwise.app"
            val result = auth.signInWithEmailAndPassword(email, password).await()
            result.user?.getIdToken(false)?.await()?.token?.let { token ->
                tokenStorage.saveToken("auth_token", token)
                AuthResult.Success
            } ?: AuthResult.Error("Failed to get token")
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Authentication failed")
        }
    }

    override suspend fun signInWithGoogle(idToken: String): AuthResult {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            result.user?.getIdToken(false)?.await()?.token?.let { token ->
                tokenStorage.saveToken("auth_token", token)
                AuthResult.Success
            } ?: AuthResult.Error("Failed to get token")
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Google sign-in failed")
        }
    }

    override suspend fun register(username: String, password: String): AuthResult {
        return try {
            // Convert username to email format for Firebase
            val email = "$username@spendwise.app"
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            result.user?.getIdToken(false)?.await()?.token?.let { token ->
                tokenStorage.saveToken("auth_token", token)
                AuthResult.Success
            } ?: AuthResult.Error("Failed to get token")
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Registration failed")
        }
    }

    override suspend fun signOut() {
        auth.signOut()
        tokenStorage.clearTokens()
    }

    override fun isUserSignedIn(): Boolean = auth.currentUser != null

    override fun getCurrentUser(): String? {
        // Extract username from email (removing @spendwise.app)
        return auth.currentUser?.email?.split("@")?.firstOrNull()
    }
} 