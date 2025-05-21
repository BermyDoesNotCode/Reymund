package com.example.spendwise

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.spendwise.ui.screens.HomeScreen
import com.example.spendwise.ui.screens.LoginScreen
import com.example.spendwise.ui.screens.RegisterScreen
import com.example.spendwise.ui.theme.SpendWiseTheme
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    companion object {
        private const val TAG = "SpendWiseMain"
    }

    @Inject
    lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Thread.setDefaultUncaughtExceptionHandler { _, exception ->
            Log.e(TAG, "Uncaught exception: ${exception.message}", exception)
            Toast.makeText(this, "App error: ${exception.message}", Toast.LENGTH_LONG).show()
        }
        
        setContent {
            SpendWiseTheme {
                val errorHandler = { error: Throwable -> 
                    Log.e(TAG, "Compose error: ${error.message}", error)
                    Unit
                }
                
                AuthNavHost(
                    errorHandler = errorHandler,
                    onLogout = {
                        auth.signOut()
                    }
                )
            }
        }
    }
}

@Composable
fun AuthNavHost(
    errorHandler: (Throwable) -> Unit = {},
    onLogout: () -> Unit
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    
    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginScreen(
                onNavigateToRegister = { 
                    try {
                        navController.navigate("register") 
                    } catch (e: Exception) {
                        errorHandler(e)
                    }
                },
                onLoginSuccess = { 
                    try {
                        navController.navigate("home") {
                            popUpTo("login") { inclusive = true }
                        }
                    } catch (e: Exception) {
                        errorHandler(e)
                    }
                }
            )
        }
        composable("register") {
            RegisterScreen(
                onNavigateToLogin = { 
                    try {
                        navController.popBackStack() 
                    } catch (e: Exception) {
                        errorHandler(e)
                    }
                },
                onRegisterSuccess = { 
                    try {
                        navController.popBackStack() 
                    } catch (e: Exception) {
                        errorHandler(e)
                    }
                }
            )
        }
        composable("home") {
            HomeScreen(
                onNavigateToAddTransaction = {
                    // TODO: Implement navigation to add transaction screen
                },
                onNavigateToProfile = {
                    // TODO: Implement navigation to profile screen
                },
                onLogout = {
                    onLogout()
                    navController.navigate("login") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            )
        }
    }
}