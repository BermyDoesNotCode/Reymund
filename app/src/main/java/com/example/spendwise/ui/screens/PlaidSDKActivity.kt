package com.example.spendwise.ui.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.example.spendwise.ui.theme.SpendWiseTheme

/**
 * Activity that uses WebView to show the Plaid Link interface.
 * This approach is more reliable than trying to use the native SDK.
 */
class PlaidSDKActivity : ComponentActivity() {
    companion object {
        private const val TAG = "PlaidSDKActivity"
        const val EXTRA_LINK_TOKEN = "link_token"
        const val EXTRA_PUBLIC_TOKEN = "public_token"

        fun createIntent(activity: Activity, linkToken: String): Intent {
            return Intent(activity, PlaidSDKActivity::class.java).apply {
                putExtra(EXTRA_LINK_TOKEN, linkToken)
            }
        }
    }

    private val linkToken by lazy { intent.getStringExtra(EXTRA_LINK_TOKEN) ?: "" }
    private var webViewError: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (linkToken.isBlank()) {
            Log.e(TAG, "No link token provided")
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        Log.d(TAG, "Starting PlaidSDKActivity with link token: ${linkToken.take(10)}...")

        setContent {
            SpendWiseTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PlaidLinkWebViewScreen(
                        linkToken = linkToken,
                        onSuccess = { publicToken ->
                            Log.d(TAG, "Public token received: $publicToken")
                            setResult(RESULT_OK, Intent().apply {
                                putExtra(EXTRA_PUBLIC_TOKEN, publicToken)
                            })
                            finish()
                        },
                        onExit = { reason ->
                            Log.d(TAG, "User exited Plaid Link flow: $reason")
                            setResult(RESULT_CANCELED)
                            finish()
                        },
                        onWebViewError = { error ->
                            webViewError = error
                            Log.e(TAG, "WebView error: $error")
                        }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "PlaidSDKActivity destroyed. Error: $webViewError")
    }
}

private const val WEB_TAG = "PlaidWebView"

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PlaidLinkWebViewScreen(
    linkToken: String,
    onSuccess: (String) -> Unit,
    onExit: (String) -> Unit,
    onWebViewError: (String) -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current
    
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Instructions at the top
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Plaid Sandbox Credentials",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Username: user_good\nPassword: pass_good",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium
            )
            
            // Show error if present
            error?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Error: $it",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
                Button(onClick = { error = null; isLoading = true }) {
                    Text("Retry")
                }
            }
        }
        
        // Only show WebView if no error
        if (error == null) {
            // WebView
            AndroidView(
                factory = { context ->
                    Log.d(WEB_TAG, "Creating WebView for Plaid Link with token: ${linkToken.take(10)}...")
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.loadsImagesAutomatically = true
                        settings.javaScriptCanOpenWindowsAutomatically = true
                        settings.setSupportMultipleWindows(true)
                        settings.userAgentString = settings.userAgentString + " PlaidApp"
                        
                        // Add console logging
                        webChromeClient = object : WebChromeClient() {
                            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                                Log.d(WEB_TAG, "Console: ${consoleMessage?.message()} -- From line ${consoleMessage?.lineNumber()} of ${consoleMessage?.sourceId()}")
                                return true
                            }
                        }
                        
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                                val url = request.url.toString()
                                Log.d(WEB_TAG, "URL loading: $url")
                                
                                // Handle success callback
                                if (url.startsWith("plaidlink://success") || url.contains("oauth/callback?public_token=") || url.contains("public_token=")) {
                                    try {
                                        val uri = Uri.parse(url)
                                        // Try different ways to extract the token
                                        val publicToken = when {
                                            uri.getQueryParameter("public_token") != null -> {
                                                uri.getQueryParameter("public_token") ?: ""
                                            }
                                            url.contains("public_token=") -> {
                                                url.substringAfter("public_token=").substringBefore("&")
                                            }
                                            else -> ""
                                        }
                                        
                                        if (publicToken.isNotEmpty()) {
                                            Log.d(WEB_TAG, "Successfully extracted public token: ${publicToken.take(5)}...")
                                            onSuccess(publicToken)
                                            return true
                                        } else {
                                            Log.e(WEB_TAG, "Failed to extract public token from URL: $url")
                                            error = "Failed to extract token"
                                            onWebViewError("Failed to extract public token")
                                        }
                                    } catch (e: Exception) {
                                        Log.e(WEB_TAG, "Error processing success URL", e)
                                        error = "Error: ${e.message}"
                                        onWebViewError("Error processing success URL: ${e.message}")
                                    }
                                }
                                
                                // Handle exit callback
                                if (url.startsWith("plaidlink://exit")) {
                                    val errorMessage = try {
                                        val uri = Uri.parse(url)
                                        uri.getQueryParameter("error") ?: "User canceled"
                                    } catch (e: Exception) {
                                        "Exit with parsing error: ${e.message}"
                                    }
                                    onExit(errorMessage)
                                    return true
                                }
                                
                                return false
                            }
                            
                            override fun onPageFinished(view: WebView, url: String) {
                                super.onPageFinished(view, url)
                                Log.d(WEB_TAG, "Page finished loading: $url")
                                isLoading = false
                                
                                // Inject JavaScript to auto-fill credentials in sandbox
                                if (url.contains("plaid.com") || url.contains("cdn.plaid.com")) {
                                    val autofillJs = """
                                        javascript:(function() {
                                            console.log('Plaid autofill script injected');
                                            
                                            function fillCredentials() {
                                                console.log('Trying to fill credentials');
                                                var inputs = document.getElementsByTagName('input');
                                                for (var i = 0; i < inputs.length; i++) {
                                                    var input = inputs[i];
                                                    if (input.placeholder && (
                                                        input.placeholder.toLowerCase().includes('username') || 
                                                        input.placeholder.toLowerCase().includes('user') ||
                                                        input.type === 'text')) {
                                                        input.value = 'user_good';
                                                        console.log('Username filled');
                                                        
                                                        // Trigger input events
                                                        var event = new Event('input', { bubbles: true });
                                                        input.dispatchEvent(event);
                                                        input.dispatchEvent(new Event('change', { bubbles: true }));
                                                    }
                                                    
                                                    if (input.placeholder && (
                                                        input.placeholder.toLowerCase().includes('password') || 
                                                        input.type === 'password')) {
                                                        input.value = 'pass_good';
                                                        console.log('Password filled');
                                                        
                                                        // Trigger input events
                                                        var event = new Event('input', { bubbles: true });
                                                        input.dispatchEvent(event);
                                                        input.dispatchEvent(new Event('change', { bubbles: true }));
                                                    }
                                                }
                                                
                                                // Try to find and click continue/submit button
                                                var buttons = document.getElementsByTagName('button');
                                                for (var i = 0; i < buttons.length; i++) {
                                                    var button = buttons[i];
                                                    if (button.innerText && (
                                                        button.innerText.toLowerCase().includes('continue') ||
                                                        button.innerText.toLowerCase().includes('submit') ||
                                                        button.innerText.toLowerCase().includes('next'))) {
                                                        console.log('Submit button found, clicking it');
                                                        setTimeout(function() { 
                                                            button.click();
                                                            console.log('Button clicked');
                                                        }, 500);
                                                        break;
                                                    }
                                                }
                                            }
                                            
                                            // Try to fill immediately
                                            fillCredentials();
                                            
                                            // And also after a short delay
                                            setTimeout(fillCredentials, 1000);
                                            setTimeout(fillCredentials, 2000);
                                        })();
                                    """.trimIndent()
                                    
                                    view.evaluateJavascript(autofillJs, null)
                                }
                            }
                            
                            // Add error handling
                            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                                super.onReceivedError(view, request, error)
                                Log.e(WEB_TAG, "WebView error: ${error.description} (${error.errorCode})")
                                isLoading = false
                                val errorMessage = "WebView error: ${error.description}"
                                onWebViewError(errorMessage)
                            }
                            
                            override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, errorResponse: WebResourceResponse) {
                                super.onReceivedHttpError(view, request, errorResponse)
                                Log.e(WEB_TAG, "HTTP Error: ${errorResponse.statusCode} for URL: ${request.url}")
                                if (errorResponse.statusCode >= 400) {
                                    isLoading = false
                                    val errorMessage = "HTTP Error ${errorResponse.statusCode}"
                                    onWebViewError(errorMessage)
                                }
                            }
                        }
                        
                        // Load the Plaid Link URL with explicit redirection parameters
                        val plaidUrl = "https://cdn.plaid.com/link/v2/stable/link.html" +
                                "?isWebview=true" +
                                "&token=$linkToken" +
                                "&redirect_uri=plaidlink://success" +
                                "&exit_uri=plaidlink://exit"
                        Log.d(WEB_TAG, "Loading Plaid URL: $plaidUrl")
                        loadUrl(plaidUrl)
                    }
                },
                modifier = Modifier.fillMaxSize().padding(top = 72.dp),
                update = { webView ->
                    // This block is called when the composition is recomposed
                    // We can use it to update the WebView if needed
                    Log.d(WEB_TAG, "WebView composition updated")
                }
            )
            
            // Lifecycle observer to ensure proper cleanup
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        Log.d(WEB_TAG, "Lifecycle resumed")
                    } else if (event == Lifecycle.Event.ON_PAUSE) {
                        Log.d(WEB_TAG, "Lifecycle paused")
                    } else if (event == Lifecycle.Event.ON_STOP) {
                        Log.d(WEB_TAG, "Lifecycle stopped")
                    }
                }
                
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                    Log.d(WEB_TAG, "WebView disposed")
                }
            }
        }
        
        // Loading indicator
        if (isLoading && error == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        
        // Cancel button at the bottom
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.BottomCenter)
        ) {
            Button(
                onClick = { onExit("User canceled") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel")
            }
        }
    }
} 