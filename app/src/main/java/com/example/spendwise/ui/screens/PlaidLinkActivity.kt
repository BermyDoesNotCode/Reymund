package com.example.spendwise.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.spendwise.ui.theme.SpendWiseTheme

/**
 * A simple activity that displays the Plaid Link web interface for sandbox testing.
 * This approach provides a simplified alternative to the Plaid SDK which can be
 * challenging to integrate.
 */
class PlaidLinkActivity : ComponentActivity() {
    companion object {
        private const val TAG = "PlaidLinkActivity"
        const val EXTRA_LINK_TOKEN = "link_token"
        const val EXTRA_PUBLIC_TOKEN = "public_token"
        
        fun createIntent(activity: Activity, linkToken: String): Intent {
            return Intent(activity, PlaidLinkActivity::class.java).apply {
                putExtra(EXTRA_LINK_TOKEN, linkToken)
            }
        }
    }

    private val linkTokenArg by lazy { intent.getStringExtra(EXTRA_LINK_TOKEN) ?: "" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (linkTokenArg.isBlank()) {
            Log.e(TAG, "No link token provided")
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        setContent {
            SpendWiseTheme {
                PlaidLinkWebScreen(
                    linkToken = linkTokenArg,
                    onSuccess = { publicToken ->
                        Log.d(TAG, "Public token obtained: $publicToken")
                        setResult(RESULT_OK, Intent().apply {
                            putExtra(EXTRA_PUBLIC_TOKEN, publicToken)
                        })
                        finish()
                    },
                    onExit = { error ->
                        val message = error?.let { "Error: ${it}" } ?: "User exited Plaid Link flow"
                        Log.d(TAG, message)
                        setResult(RESULT_CANCELED)
                        finish()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaidLinkWebScreen(
    linkToken: String,
    onSuccess: (String) -> Unit,
    onExit: (String?) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Connect Your Bank") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Instructions
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Plaid Sandbox Credentials",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Username: user_good")
                    Text("Password: pass_good")
                }
            }
            
            // WebView to load Plaid Link
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            
                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                                    val url = request.url.toString()
                                    
                                    // Handle success callback
                                    if (url.startsWith("plaidlink://success") || url.contains("oauth/callback?public_token=")) {
                                        val uri = Uri.parse(url)
                                        val publicToken = uri.getQueryParameter("public_token")
                                            ?: uri.toString().substringAfter("public_token=").substringBefore("&")
                                        
                                        if (publicToken.isNotEmpty()) {
                                            onSuccess(publicToken)
                                            return true
                                        }
                                    }
                                    
                                    // Handle exit callback
                                    if (url.startsWith("plaidlink://exit")) {
                                        val uri = Uri.parse(url)
                                        val error = uri.getQueryParameter("error")
                                        onExit(error)
                                        return true
                                    }
                                    
                                    return false
                                }
                            }
                            
                            // Load the Plaid Link URL
                            val plaidUrl = "https://cdn.plaid.com/link/v2/stable/link.html?isWebview=true&token=$linkToken&redirect_uri=plaidlink://success&exit_uri=plaidlink://exit"
                            loadUrl(plaidUrl)
                        }
                    }
                )
                
                // Loading indicator
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            
            // Cancel button
            Button(
                onClick = { onExit(null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("Cancel")
            }
        }
    }
} 