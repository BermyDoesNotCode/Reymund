package com.example.spendwise.ui.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.spendwise.data.model.Transaction
import com.example.spendwise.data.plaid.PlaidService
import com.example.spendwise.ui.theme.*
import com.example.spendwise.ui.viewmodels.HomeViewModel
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToAddTransaction: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onLogout: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showPlaidDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    // Register for activity result from Plaid SDK Activity
    val plaidLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            val publicToken = result.data?.getStringExtra(PlaidSDKActivity.EXTRA_PUBLIC_TOKEN)
            publicToken?.let {
                Log.d("HomeScreen", "Received public token from Plaid SDK: $it")
                viewModel.handlePublicToken(it)
            }
        } else {
            Log.d("HomeScreen", "Plaid connection cancelled or failed")
        }
    }

    // Effect to handle Plaid Link token
    LaunchedEffect(uiState.linkToken) {
        uiState.linkToken?.let { token ->
            // Launch the WebView-based Plaid activity
            val intent = PlaidSDKActivity.createIntent(context as Activity, token)
            plaidLauncher.launch(intent)
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Logout") },
            text = { Text("Are you sure you want to logout?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    }
                ) {
                    Text("Yes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("No")
                }
            }
        )
    }

    if (showPlaidDialog) {
        AlertDialog(
            onDismissRequest = { showPlaidDialog = false },
            title = { Text("Connect Bank Account") },
            text = { 
                Column {
                    Text("Would you like to connect your bank account to track your finances?")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "For Plaid Sandbox, use the following credentials:",
                        fontWeight = FontWeight.Bold
                    )
                    Text("Username: user_good")
                    Text("Password: pass_good")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPlaidDialog = false
                        // Start the Plaid connection process
                        viewModel.connectPlaidAccount()
                    }
                ) {
                    Text("Connect")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPlaidDialog = false }) {
                    Text("Not Now")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "SpendWise",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    // Refresh button
                    IconButton(onClick = { viewModel.refreshData() }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh Data",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    // Connect bank button
                    IconButton(onClick = { showPlaidDialog = true }) {
                        Icon(
                            Icons.Default.AccountBalance,
                            contentDescription = "Connect Bank",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    // Logout button
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(
                            Icons.Default.Logout,
                            contentDescription = "Logout",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddTransaction,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Transaction")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Show data source indicator
                if (uiState.dataSource != "Plaid Data") {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Currently showing ${uiState.dataSource}. Connect a bank to see real data.",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                item {
                    BudgetSummaryCard(
                        totalBudget = uiState.totalBudget,
                        spent = uiState.totalSpent,
                        remaining = uiState.remainingBudget
                    )
                }

                item {
                    QuickActionsRow(
                        onAddExpense = { onNavigateToAddTransaction() },
                        onAddIncome = { onNavigateToAddTransaction() },
                        onViewReports = { /* TODO */ }
                    )
                }

                item {
                    Text(
                        "Recent Transactions",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (uiState.recentTransactions.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No transactions to display",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    items(uiState.recentTransactions) { transaction ->
                        TransactionItem(transaction = transaction)
                    }
                }
            }
        }

        uiState.error?.let { error ->
            Snackbar(
                modifier = Modifier
                    .padding(16.dp)
                    .padding(bottom = 80.dp), // Extra padding for FAB
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                dismissAction = {
                    IconButton(onClick = { viewModel.refreshData() }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Retry",
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            ) {
                Text(error)
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PlaidWebView(
    linkToken: String,
    onSuccess: (String) -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.setGeolocationEnabled(true)
                settings.javaScriptCanOpenWindowsAutomatically = true
                settings.setSupportMultipleWindows(true)
                settings.loadsImagesAutomatically = true
                settings.userAgentString = settings.userAgentString + " PlaidSDK"
                
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                        val url = request.url.toString()
                        Log.d("PlaidWebView", "Loading URL: $url")
                        
                        // Handle success callback
                        if (url.startsWith("plaidlink://success") || url.contains("oauth/callback?public_token=")) {
                            val uri = Uri.parse(url)
                            val publicToken = uri.getQueryParameter("public_token")
                                ?: uri.toString().substringAfter("public_token=").substringBefore("&")
                            
                            if (publicToken.isNotEmpty()) {
                                Log.d("PlaidWebView", "Success with public token: $publicToken")
                                onSuccess(publicToken)
                                return true
                            }
                        }
                        
                        // Handle exit callback
                        if (url.startsWith("plaidlink://exit")) {
                            Log.d("PlaidWebView", "Exit callback received")
                            onExit()
                            return true
                        }
                        
                        return false
                    }
                    
                    override fun onPageFinished(view: WebView, url: String) {
                        super.onPageFinished(view, url)
                        Log.d("PlaidWebView", "Page finished loading: $url")
                        
                        // More robust JavaScript injection for Plaid Sandbox
                        if (url.contains("cdn.plaid.com/link")) {
                            // First attempt - direct injection
                            val directInjection = """
                                javascript:(function() {
                                    function fillCredentials() {
                                        const inputs = document.querySelectorAll('input');
                                        const buttons = document.querySelectorAll('button');
                                        
                                        for (let input of inputs) {
                                            if (input.type === 'text' || input.placeholder && input.placeholder.toLowerCase().includes('username')) {
                                                input.value = 'user_good';
                                                input.dispatchEvent(new Event('input', { bubbles: true }));
                                                console.log('Username filled');
                                            }
                                            
                                            if (input.type === 'password' || input.placeholder && input.placeholder.toLowerCase().includes('password')) {
                                                input.value = 'pass_good';
                                                input.dispatchEvent(new Event('input', { bubbles: true }));
                                                console.log('Password filled');
                                            }
                                        }
                                        
                                        // Try to find and click continue/submit button
                                        for (let button of buttons) {
                                            if (button.type === 'submit' || 
                                                button.innerText && (button.innerText.toLowerCase().includes('continue') || 
                                                                   button.innerText.toLowerCase().includes('submit'))) {
                                                console.log('Submit button found, will click shortly');
                                                setTimeout(() => button.click(), 500);
                                                break;
                                            }
                                        }
                                    }
                                    
                                    // Try immediately
                                    fillCredentials();
                                    
                                    // Also set up a mutation observer to watch for form appearing
                                    const observer = new MutationObserver(function(mutations) {
                                        for (let mutation of mutations) {
                                            if (mutation.addedNodes.length) {
                                                const inputs = document.querySelectorAll('input');
                                                if (inputs.length >= 2) {
                                                    console.log('Form detected via mutation, filling credentials');
                                                    fillCredentials();
                                                }
                                            }
                                        }
                                    });
                                    
                                    observer.observe(document.body, { childList: true, subtree: true });
                                    
                                    // Also set up message listener for Plaid Link events
                                    window.addEventListener('message', function(event) {
                                        console.log('Plaid message:', event.data);
                                        if (event.data && event.data.action === 'RENDER_VIEW') {
                                            console.log('View rendering:', event.data.view_name);
                                            if (event.data.view_name === 'CREDENTIAL') {
                                                console.log('Credential view detected');
                                                setTimeout(fillCredentials, 500);
                                            }
                                        }
                                    });
                                    
                                    // Also try every second for a few seconds
                                    for (let i = 1; i <= 5; i++) {
                                        setTimeout(fillCredentials, i * 1000);
                                    }
                                })();
                            """.trimIndent()
                            view.evaluateJavascript(directInjection, null)
                        }
                    }
                }
                
                // Load the Plaid Link URL with explicit redirect parameters
                val plaidUrl = "https://cdn.plaid.com/link/v2/stable/link.html?isWebview=true&token=$linkToken&redirect_uri=plaidlink://success&exit_uri=plaidlink://exit"
                Log.d("PlaidWebView", "Loading Plaid URL: $plaidUrl")
                loadUrl(plaidUrl)
            }
        }
    )
    
    // Show instructions for sandbox testing
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "For Plaid Sandbox testing:",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = Color.DarkGray
            )
            Text(
                text = "Username: user_good",
                style = MaterialTheme.typography.bodySmall,
                color = Color.DarkGray
            )
            Text(
                text = "Password: pass_good",
                style = MaterialTheme.typography.bodySmall,
                color = Color.DarkGray
            )
        }
    }
}

@Composable
fun BudgetSummaryCard(
    totalBudget: Double,
    spent: Double,
    remaining: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                "Budget Summary",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            BudgetProgressBar(
                progress = (spent / totalBudget).coerceIn(0.0, 1.0),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                BudgetItem(
                    label = "Total Budget",
                    amount = totalBudget,
                    icon = Icons.Default.AccountBalance,
                    color = MaterialTheme.colorScheme.primary
                )
                BudgetItem(
                    label = "Spent",
                    amount = spent,
                    icon = Icons.Default.ShoppingCart,
                    color = MaterialTheme.colorScheme.error
                )
                BudgetItem(
                    label = "Remaining",
                    amount = remaining,
                    icon = Icons.Default.AccountBalanceWallet,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

@Composable
fun BudgetProgressBar(
    progress: Double,
    modifier: Modifier = Modifier
) {
    LinearProgressIndicator(
        progress = progress.toFloat(),
        modifier = modifier.height(8.dp),
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.surfaceVariant
    )
}

@Composable
fun BudgetItem(
    label: String,
    amount: Double,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = formatCurrency(amount),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = color
        )
    }
}

@Composable
fun QuickActionsRow(
    onAddExpense: () -> Unit,
    onAddIncome: () -> Unit,
    onViewReports: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            QuickActionButton(
                icon = Icons.Default.Remove,
                label = "Expense",
                onClick = onAddExpense,
                color = MaterialTheme.colorScheme.error
            )
            QuickActionButton(
                icon = Icons.Default.Add,
                label = "Income",
                onClick = onAddIncome,
                color = MaterialTheme.colorScheme.primary
            )
            QuickActionButton(
                icon = Icons.Default.Assessment,
                label = "Reports",
                onClick = onViewReports,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}

@Composable
fun QuickActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = color
        )
    }
}

@Composable
fun TransactionItem(transaction: Transaction) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (transaction.isExpense) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                    contentDescription = null,
                    tint = if (transaction.isExpense) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = transaction.description,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = transaction.date,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = formatCurrency(transaction.amount),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = if (transaction.isExpense) 
                    MaterialTheme.colorScheme.error
                else 
                    MaterialTheme.colorScheme.primary
            )
        }
    }
}

private fun formatCurrency(amount: Double): String {
    return NumberFormat.getCurrencyInstance(Locale.getDefault()).format(amount)
} 