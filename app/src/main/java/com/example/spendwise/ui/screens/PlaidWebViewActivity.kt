package com.example.spendwise.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.spendwise.ui.theme.SpendWiseTheme

private const val TAG = "PlaidWebViewActivity"

class PlaidWebViewActivity : ComponentActivity() {
    companion object {
        private const val EXTRA_LINK_TOKEN = "link_token"
        
        fun createIntent(context: Context, linkToken: String): Intent {
            return Intent(context, PlaidWebViewActivity::class.java).apply {
                putExtra(EXTRA_LINK_TOKEN, linkToken)
            }
        }
    }
    
    private val linkToken by lazy { intent.getStringExtra(EXTRA_LINK_TOKEN) ?: "" }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (linkToken.isEmpty()) {
            Log.e(TAG, "No link token provided")
            setResult(RESULT_CANCELED)
            finish()
            return
        }
        
        setContent {
            SpendWiseTheme {
                PlaidLinkScreen(
                    linkToken = linkToken,
                    onSuccess = { publicToken ->
                        val resultIntent = Intent().apply {
                            putExtra("public_token", publicToken)
                        }
                        setResult(RESULT_OK, resultIntent)
                        finish()
                    },
                    onExit = {
                        setResult(RESULT_CANCELED)
                        finish()
                    },
                    onClose = {
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
fun PlaidLinkScreen(
    linkToken: String,
    onSuccess: (String) -> Unit,
    onExit: () -> Unit,
    onClose: () -> Unit
) {
    val isLoading = remember { mutableStateOf(true) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Connect Your Bank",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            EnhancedPlaidWebView(
                linkToken = linkToken,
                onSuccess = onSuccess,
                onExit = onExit,
                onPageStarted = { isLoading.value = true },
                onPageFinished = { isLoading.value = false }
            )
            
            if (isLoading.value) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            // Instructions box at the bottom
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "For Plaid Sandbox testing:",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text("Username: user_good")
                    Text("Password: pass_good")
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun EnhancedPlaidWebView(
    linkToken: String,
    onSuccess: (String) -> Unit,
    onExit: () -> Unit,
    onPageStarted: () -> Unit,
    onPageFinished: () -> Unit
) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.javaScriptCanOpenWindowsAutomatically = true
                settings.setSupportMultipleWindows(true)
                settings.loadsImagesAutomatically = true
                settings.userAgentString = settings.userAgentString + " PlaidSDK"
                
                // Enable WebView debugging
                WebView.setWebContentsDebuggingEnabled(true)
                
                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        Log.d(TAG, "🔍 Page loading started: $url")
                        onPageStarted()
                    }
                    
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                        val url = request.url.toString()
                        Log.d(TAG, "🔍 Loading URL: $url")
                        
                        // Handle success callback
                        if (url.startsWith("plaidlink://success") || url.contains("oauth/callback?public_token=")) {
                            val uri = Uri.parse(url)
                            val publicToken = uri.getQueryParameter("public_token")
                                ?: uri.toString().substringAfter("public_token=").substringBefore("&")
                            
                            if (publicToken.isNotEmpty()) {
                                Log.d(TAG, "✅ Success with public token: $publicToken")
                                onSuccess(publicToken)
                                return true
                            }
                        }
                        
                        // Handle exit callback
                        if (url.startsWith("plaidlink://exit")) {
                            Log.d(TAG, "❌ Exit callback received")
                            onExit()
                            return true
                        }
                        
                        return false
                    }
                    
                    override fun onPageFinished(view: WebView, url: String) {
                        super.onPageFinished(view, url)
                        Log.d(TAG, "🔍 Page finished loading: $url")
                        onPageFinished()
                        
                        // First, inject console logger to see what's happening
                        val consoleLogger = """
                            javascript:(function() {
                                console.log = function(message) {
                                    AndroidConsole.log(message);
                                };
                                console.info = function(message) {
                                    AndroidConsole.info(message);
                                };
                                console.warn = function(message) {
                                    AndroidConsole.warn(message);
                                };
                                console.error = function(message) {
                                    AndroidConsole.error(message);
                                };
                                console.log("🔧 Custom console logger injected");
                            })();
                        """.trimIndent()
                        
                        view.addJavascriptInterface(object {
                            @android.webkit.JavascriptInterface
                            fun log(message: String) {
                                Log.d(TAG, "📱 WebView Console: $message")
                            }
                            
                            @android.webkit.JavascriptInterface
                            fun info(message: String) {
                                Log.i(TAG, "📱 WebView Console: $message")
                            }
                            
                            @android.webkit.JavascriptInterface
                            fun warn(message: String) {
                                Log.w(TAG, "📱 WebView Console: $message")
                            }
                            
                            @android.webkit.JavascriptInterface
                            fun error(message: String) {
                                Log.e(TAG, "📱 WebView Console: $message")
                            }
                        }, "AndroidConsole")
                        
                        view.evaluateJavascript(consoleLogger, null)
                        
                        // Enhanced JavaScript injection for Plaid Sandbox
                        if (url.contains("cdn.plaid.com/link")) {
                            // More aggressive credential filling approach
                            val sandboxHelperScript = """
                                javascript:(function() {
                                    // Debug DOM structure
                                    function debugDom() {
                                        console.log("🔍 Debugging DOM Structure");
                                        console.log("Body children count: " + document.body.children.length);
                                        
                                        const inputs = document.querySelectorAll('input');
                                        console.log("Found " + inputs.length + " input fields");
                                        inputs.forEach((input, i) => {
                                            console.log("Input #" + i + ": type=" + input.type + 
                                                       ", id=" + input.id + 
                                                       ", name=" + input.name + 
                                                       ", placeholder=" + input.placeholder);
                                        });
                                        
                                        const buttons = document.querySelectorAll('button');
                                        console.log("Found " + buttons.length + " buttons");
                                        buttons.forEach((button, i) => {
                                            console.log("Button #" + i + ": type=" + button.type + 
                                                       ", text=" + button.innerText);
                                        });
                                    }
                                    
                                    // Initial DOM debugging
                                    debugDom();
                                    
                                    function fillCredentials() {
                                        console.log("🔑 Attempting to fill credentials");
                                        debugDom();
                                        
                                        // Try multiple selector strategies for username/password
                                        const strategies = [
                                            // Strategy 1: By type
                                            function() {
                                                console.log("Trying strategy 1: By input type");
                                                const textInputs = Array.from(document.querySelectorAll('input[type="text"], input:not([type])'));
                                                const passwordInputs = Array.from(document.querySelectorAll('input[type="password"]'));
                                                
                                                if (textInputs.length > 0 && passwordInputs.length > 0) {
                                                    // Assume first text input is username
                                                    const usernameField = textInputs[0];
                                                    const passwordField = passwordInputs[0];
                                                    
                                                    usernameField.value = 'user_good';
                                                    usernameField.dispatchEvent(new Event('input', { bubbles: true }));
                                                    usernameField.dispatchEvent(new Event('change', { bubbles: true }));
                                                    console.log("Username filled via strategy 1");
                                                    
                                                    passwordField.value = 'pass_good';
                                                    passwordField.dispatchEvent(new Event('input', { bubbles: true }));
                                                    passwordField.dispatchEvent(new Event('change', { bubbles: true }));
                                                    console.log("Password filled via strategy 1");
                                                    return true;
                                                }
                                                return false;
                                            },
                                            
                                            // Strategy 2: By placeholder text
                                            function() {
                                                console.log("Trying strategy 2: By placeholder text");
                                                const inputs = document.querySelectorAll('input');
                                                let usernameField = null;
                                                let passwordField = null;
                                                
                                                for (let input of inputs) {
                                                    if (input.placeholder && 
                                                        (input.placeholder.toLowerCase().includes('username') || 
                                                         input.placeholder.toLowerCase().includes('user') ||
                                                         input.placeholder.toLowerCase().includes('email'))) {
                                                        usernameField = input;
                                                    }
                                                    
                                                    if (input.placeholder && 
                                                        input.placeholder.toLowerCase().includes('password')) {
                                                        passwordField = input;
                                                    }
                                                }
                                                
                                                if (usernameField && passwordField) {
                                                    usernameField.value = 'user_good';
                                                    usernameField.dispatchEvent(new Event('input', { bubbles: true }));
                                                    usernameField.dispatchEvent(new Event('change', { bubbles: true }));
                                                    console.log("Username filled via strategy 2");
                                                    
                                                    passwordField.value = 'pass_good';
                                                    passwordField.dispatchEvent(new Event('input', { bubbles: true }));
                                                    passwordField.dispatchEvent(new Event('change', { bubbles: true }));
                                                    console.log("Password filled via strategy 2");
                                                    return true;
                                                }
                                                return false;
                                            },
                                            
                                            // Strategy 3: By order of appearance if there are exactly 2 inputs
                                            function() {
                                                console.log("Trying strategy 3: By order of appearance");
                                                const inputs = document.querySelectorAll('input');
                                                
                                                if (inputs.length === 2) {
                                                    const usernameField = inputs[0];
                                                    const passwordField = inputs[1];
                                                    
                                                    usernameField.value = 'user_good';
                                                    usernameField.dispatchEvent(new Event('input', { bubbles: true }));
                                                    usernameField.dispatchEvent(new Event('change', { bubbles: true }));
                                                    console.log("Username filled via strategy 3");
                                                    
                                                    passwordField.value = 'pass_good';
                                                    passwordField.dispatchEvent(new Event('input', { bubbles: true }));
                                                    passwordField.dispatchEvent(new Event('change', { bubbles: true }));
                                                    console.log("Password filled via strategy 3");
                                                    return true;
                                                }
                                                return false;
                                            },
                                            
                                            // Strategy 4: Brute force - fill all inputs
                                            function() {
                                                console.log("Trying strategy 4: Brute force");
                                                const inputs = document.querySelectorAll('input');
                                                let filled = false;
                                                
                                                inputs.forEach((input, i) => {
                                                    if (input.type === 'text' || !input.type || input.type === 'email') {
                                                        input.value = 'user_good';
                                                        input.dispatchEvent(new Event('input', { bubbles: true }));
                                                        input.dispatchEvent(new Event('change', { bubbles: true }));
                                                        console.log("Filled text input #" + i);
                                                        filled = true;
                                                    } else if (input.type === 'password') {
                                                        input.value = 'pass_good';
                                                        input.dispatchEvent(new Event('input', { bubbles: true }));
                                                        input.dispatchEvent(new Event('change', { bubbles: true }));
                                                        console.log("Filled password input #" + i);
                                                        filled = true;
                                                    }
                                                });
                                                
                                                return filled;
                                            }
                                        ];
                                        
                                        // Try each strategy until one works
                                        for (let strategy of strategies) {
                                            if (strategy()) {
                                                console.log("Credential filling succeeded");
                                                break;
                                            }
                                        }
                                        
                                        // Find and click the submit button
                                        setTimeout(() => {
                                            console.log("Looking for submit button");
                                            const buttons = document.querySelectorAll('button');
                                            let submitButton = null;
                                            
                                            // Try different approaches to find the submit button
                                            for (let button of buttons) {
                                                // By type
                                                if (button.type === 'submit') {
                                                    submitButton = button;
                                                    console.log("Found submit button by type");
                                                    break;
                                                }
                                                
                                                // By text content
                                                if (button.innerText && 
                                                    (button.innerText.toLowerCase().includes('continue') || 
                                                     button.innerText.toLowerCase().includes('submit') ||
                                                     button.innerText.toLowerCase().includes('next') ||
                                                     button.innerText.toLowerCase().includes('sign in') ||
                                                     button.innerText.toLowerCase().includes('login'))) {
                                                    submitButton = button;
                                                    console.log("Found submit button by text: " + button.innerText);
                                                    break;
                                                }
                                            }
                                            
                                            // If no obvious submit button, try the last button
                                            if (!submitButton && buttons.length > 0) {
                                                submitButton = buttons[buttons.length - 1];
                                                console.log("Using last button as submit");
                                            }
                                            
                                            // Click the button
                                            if (submitButton) {
                                                console.log("Clicking submit button");
                                                submitButton.click();
                                                console.log("Submit button clicked");
                                            } else {
                                                console.log("No submit button found");
                                                
                                                // Fallback: press Enter in password field
                                                const passwordInputs = document.querySelectorAll('input[type="password"]');
                                                if (passwordInputs.length > 0) {
                                                    console.log("Trying to press Enter in password field");
                                                    const event = new KeyboardEvent('keypress', {
                                                        key: 'Enter',
                                                        code: 'Enter',
                                                        which: 13,
                                                        keyCode: 13,
                                                        bubbles: true
                                                    });
                                                    passwordInputs[0].dispatchEvent(event);
                                                }
                                            }
                                        }, 1000);
                                    }
                                    
                                    // Set up mutation observer to detect when form appears
                                    console.log("Setting up mutation observer");
                                    const observer = new MutationObserver(function(mutations) {
                                        for (let mutation of mutations) {
                                            if (mutation.addedNodes.length) {
                                                console.log("DOM mutation detected");
                                                const inputs = document.querySelectorAll('input');
                                                if (inputs.length >= 2) {
                                                    console.log("Form detected via mutation, filling credentials");
                                                    fillCredentials();
                                                }
                                            }
                                        }
                                    });
                                    
                                    observer.observe(document.body, { childList: true, subtree: true });
                                    
                                    // Listen for Plaid Link events
                                    console.log("Setting up message listener");
                                    window.addEventListener('message', function(event) {
                                        if (event.data && typeof event.data === 'object') {
                                            console.log("Plaid message received: " + JSON.stringify(event.data));
                                            if (event.data.action === 'TRANSITION_VIEW' || 
                                                event.data.action === 'RENDER_VIEW') {
                                                console.log("View transition/render: " + event.data.view_name);
                                                if (event.data.view_name === 'CREDENTIAL') {
                                                    console.log("Credential view detected");
                                                    setTimeout(fillCredentials, 500);
                                                }
                                            }
                                        }
                                    });
                                    
                                    // If sandbox is in the URL, override console to print more details
                                    if (window.location.href.includes('sandbox')) {
                                        console.log("🏖️ Sandbox detected in URL");
                                    }
                                    
                                    // Try immediate fill (for already loaded forms)
                                    fillCredentials();
                                    
                                    // Also try at staggered intervals
                                    console.log("Scheduling delayed fill attempts");
                                    const intervals = [1000, 2000, 3000, 5000, 8000];
                                    for (let interval of intervals) {
                                        setTimeout(() => {
                                            console.log("Delayed fill attempt at " + interval + "ms");
                                            fillCredentials();
                                        }, interval);
                                    }
                                })();
                            """.trimIndent()
                            
                            view.evaluateJavascript(sandboxHelperScript, null)
                            
                            // Check DOM structure after a delay
                            Handler(Looper.getMainLooper()).postDelayed({
                                val domInspector = """
                                    javascript:(function() {
                                        console.log("📊 DOM INSPECTOR");
                                        console.log("URL: " + window.location.href);
                                        console.log("iframes: " + document.querySelectorAll('iframe').length);
                                        console.log("forms: " + document.querySelectorAll('form').length);
                                        console.log("inputs: " + document.querySelectorAll('input').length);
                                    })();
                                """.trimIndent()
                                view.evaluateJavascript(domInspector, null)
                            }, 3000)
                        }
                    }
                }
                
                // Load the Plaid Link URL with explicit redirect parameters
                val plaidUrl = "https://cdn.plaid.com/link/v2/stable/link.html?isWebview=true&token=$linkToken&redirect_uri=plaidlink://success&exit_uri=plaidlink://exit"
                Log.d(TAG, "🚀 Loading Plaid URL: $plaidUrl")
                loadUrl(plaidUrl)
            }
        }
    )
} 