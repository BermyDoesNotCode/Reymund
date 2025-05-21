package com.example.spendwise.ui.viewmodels

import android.content.Intent
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.spendwise.data.model.AuthResult
import com.example.spendwise.data.model.User
import com.example.spendwise.auth.AuthRepository
import com.example.spendwise.data.repository.UserRepository
import com.example.spendwise.data.repository.BiometricHelper
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val biometricHelper: BiometricHelper
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthResult>(AuthResult.Error(""))
    val authState: StateFlow<AuthResult> = _authState

    private val _isBiometricAvailable = MutableStateFlow(false)
    val isBiometricAvailable: StateFlow<Boolean> = _isBiometricAvailable

    init {
        checkBiometricAvailability()
    }

    private fun checkBiometricAvailability() {
        _isBiometricAvailable.value = biometricHelper.canUseBiometric()
    }

    fun login(username: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthResult.Loading
            try {
                val result = authRepository.signInWithUsername(username, password)
                when (result) {
                    is com.example.spendwise.data.model.AuthResult.Success -> {
                        val user = userRepository.getCurrentUser()
                        if (user != null) {
                            userRepository.updateLastLogin(user.id)
                        }
                        _authState.value = AuthResult.Success
                    }
                    is com.example.spendwise.data.model.AuthResult.Error -> {
                        _authState.value = AuthResult.Error(result.message)
                    }
                    else -> {
                        _authState.value = AuthResult.Error("Unknown error")
                    }
                }
            } catch (e: Exception) {
                _authState.value = AuthResult.Error(e.message ?: "Login failed")
            }
        }
    }

    fun loginWithGoogle(idToken: String) {
        viewModelScope.launch {
            _authState.value = AuthResult.Loading
            try {
                val result = authRepository.signInWithGoogle(idToken)
                when (result) {
                    is com.example.spendwise.data.model.AuthResult.Success -> {
                        val user = userRepository.getCurrentUser()
                        if (user != null) {
                            userRepository.updateLastLogin(user.id)
                        }
                        _authState.value = AuthResult.Success
                    }
                    is com.example.spendwise.data.model.AuthResult.Error -> {
                        _authState.value = AuthResult.Error(result.message)
                    }
                    else -> {
                        _authState.value = AuthResult.Error("Unknown error")
                    }
                }
            } catch (e: Exception) {
                _authState.value = AuthResult.Error(e.message ?: "Google login failed")
            }
        }
    }

    fun handleGoogleSignInResult(data: Intent?) {
        viewModelScope.launch {
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                val account = task.getResult(ApiException::class.java)
                account?.idToken?.let { token ->
                    loginWithGoogle(token)
                } ?: run {
                    _authState.value = AuthResult.Error("Failed to get Google ID token")
                }
            } catch (e: ApiException) {
                _authState.value = AuthResult.Error("Google sign-in failed: ${e.message}")
            }
        }
    }

    fun authenticateWithBiometric(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        biometricHelper.showBiometricPrompt(
            activity = activity,
            onSuccess = {
                viewModelScope.launch {
                    val user = userRepository.getCurrentUser()
                    if (user != null) {
                        userRepository.updateLastLogin(user.id)
                        onSuccess()
                    } else {
                        onError("User not found")
                    }
                }
            },
            onError = { error ->
                onError(error)
            }
        )
    }

    // Add this method to handle missing Google configuration
    fun handleMissingGoogleConfig() {
        _authState.value = AuthResult.Error("Google Sign-In is not configured. Please set up Google authentication in Firebase console.")
    }
} 