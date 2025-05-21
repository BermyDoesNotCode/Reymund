package com.example.spendwise

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SpendWiseApplication : Application() {
    companion object {
        private const val TAG = "SpendWiseApp"
    }
    
    override fun onCreate() {
        super.onCreate()
        
        try {
            // Initialize Firebase
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
                Log.d(TAG, "Firebase initialized successfully")
                
                // Test Firebase Auth to ensure it's working
                val auth = FirebaseAuth.getInstance()
                Log.d(TAG, "Firebase Auth instance: ${auth != null}")
            } else {
                Log.d(TAG, "Firebase already initialized")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Firebase: ${e.message}", e)
        }
    }
} 