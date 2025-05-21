package com.example.spendwise.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.spendwise.data.model.AuthResult
import com.example.spendwise.data.model.User
import com.example.spendwise.data.model.AuthProvider
import com.example.spendwise.auth.AuthRepository
import com.example.spendwise.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthResult>(AuthResult.Error(""))
    val authState: StateFlow<AuthResult> = _authState

    fun register(username: String, password: String, displayName: String) {
        viewModelScope.launch {
            _authState.value = AuthResult.Loading
            try {
                val result = authRepository.register(username, password)
                when (result) {
                    is com.example.spendwise.data.model.AuthResult.Success -> {
                        val user = User(
                            username = username,
                            displayName = displayName,
                            provider = AuthProvider.EMAIL
                        )
                        val createUserResult = userRepository.createUser(user)
                        if (createUserResult.isSuccess) {
                            _authState.value = AuthResult.Success
                        } else {
                            _authState.value = AuthResult.Error(createUserResult.exceptionOrNull()?.message ?: "Failed to create user profile")
                        }
                    }
                    is com.example.spendwise.data.model.AuthResult.Error -> {
                        _authState.value = AuthResult.Error(result.message)
                    }
                    else -> {
                        _authState.value = AuthResult.Error("Unknown error")
                    }
                }
            } catch (e: Exception) {
                _authState.value = AuthResult.Error(e.message ?: "Registration failed")
            }
        }
    }

    fun registerWithGoogle(idToken: String) {
        viewModelScope.launch {
            _authState.value = AuthResult.Loading
            try {
                val result = authRepository.signInWithGoogle(idToken)
                when (result) {
                    is com.example.spendwise.data.model.AuthResult.Success -> {
                        val user = userRepository.getCurrentUser()
                        if (user != null) {
                            val createUserResult = userRepository.createUser(user)
                            if (createUserResult.isSuccess) {
                                _authState.value = AuthResult.Success
                            } else {
                                _authState.value = AuthResult.Error(createUserResult.exceptionOrNull()?.message ?: "Failed to create user profile")
                            }
                        } else {
                            _authState.value = AuthResult.Error("Failed to get user information")
                        }
                    }
                    is com.example.spendwise.data.model.AuthResult.Error -> {
                        _authState.value = AuthResult.Error(result.message)
                    }
                    else -> {
                        _authState.value = AuthResult.Error("Unknown error")
                    }
                }
            } catch (e: Exception) {
                _authState.value = AuthResult.Error(e.message ?: "Google registration failed")
            }
        }
    }
} 