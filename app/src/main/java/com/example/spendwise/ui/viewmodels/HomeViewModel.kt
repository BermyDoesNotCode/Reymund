package com.example.spendwise.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.spendwise.data.model.Transaction
import com.example.spendwise.data.plaid.PlaidService
import com.example.spendwise.data.plaid.PlaidState
import com.example.spendwise.data.plaid.PlaidTransaction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject
import kotlin.math.absoluteValue

private const val TAG = "HomeViewModel"

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val plaidService: PlaidService
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        // Try to load Plaid data immediately if we have a stored token
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                loadPlaidData()
            } catch (e: Exception) {
                // If loading fails, show empty state with no data
                Log.d(TAG, "Failed to load Plaid data, showing empty state", e)
                _uiState.update { it.copy(
                    totalBudget = 0.0,
                    totalSpent = 0.0, 
                    remainingBudget = 0.0,
                    recentTransactions = emptyList(),
                    isLoading = false,
                    error = null,
                    dataSource = "No Data"
                ) }
            }
        }
    }

    fun handlePublicToken(publicToken: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(
                    isLoading = true,
                    error = null
                ) }
                
                Log.d(TAG, "Received public token: $publicToken, exchanging for access token")
                
                if (publicToken.isBlank()) {
                    throw IllegalArgumentException("Received empty public token")
                }
                
                // Exchange public token for access token
                plaidService.exchangePublicToken(publicToken)
                
                // Load real Plaid data
                loadPlaidData()
            } catch (e: Exception) {
                Log.e(TAG, "Error handling public token", e)
                // Show empty state if Plaid exchange fails
                _uiState.update { 
                    it.copy(
                        totalBudget = 0.0,
                        totalSpent = 0.0,
                        remainingBudget = 0.0,
                        recentTransactions = emptyList(),
                        error = "Failed to connect to bank: ${e.message}",
                        isLoading = false,
                        dataSource = "No Data"
                    )
                }
            }
        }
    }

    private suspend fun loadPlaidData() {
        try {
            _uiState.update { it.copy(
                isLoading = true,
                error = null
            ) }

            Log.d(TAG, "Loading Plaid data")
            
            // Get account balances
            val balances = plaidService.getAccountBalances()
            val totalBalance = balances.sumOf { it.balance }
            Log.d(TAG, "Retrieved ${balances.size} account(s), total balance: $totalBalance")

            // Get recent transactions (last 30 days)
            val calendar = Calendar.getInstance()
            val endDate = calendar.time
            calendar.add(Calendar.DAY_OF_MONTH, -30)
            val startDate = calendar.time

            val transactions = plaidService.getTransactions(startDate, endDate)
            Log.d(TAG, "Retrieved ${transactions.size} transaction(s)")
            
            val plaidTransactions = transactions.map { plaidTransaction ->
                Transaction(
                    id = plaidTransaction.id,
                    description = plaidTransaction.name,
                    amount = plaidTransaction.amount,
                    date = plaidTransaction.date,
                    isExpense = plaidTransaction.amount < 0
                )
            }

            val spent = plaidTransactions.filter { it.isExpense }.sumOf { it.amount }.absoluteValue
            val remaining = totalBalance - spent

            _uiState.update { currentState ->
                currentState.copy(
                    totalBudget = totalBalance,
                    totalSpent = spent,
                    remainingBudget = remaining,
                    recentTransactions = plaidTransactions,
                    isLoading = false,
                    error = null,
                    dataSource = "Plaid Data"
                )
            }
            
            Log.d(TAG, "Plaid data loaded successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading Plaid data", e)
            throw e
        }
    }

    fun connectPlaidAccount() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(
                    isLoading = true,
                    error = null
                ) }
                
                val userId = "user-sandbox-${UUID.randomUUID()}" // Create a unique ID for sandbox user
                Log.d(TAG, "Connecting Plaid account for user ID: $userId")
                
                // Reset any existing link token before creating a new one
                _uiState.update { it.copy(linkToken = null) }
                
                val linkToken = plaidService.createLinkToken(
                    clientName = "SpendWise",
                    userId = userId
                )
                
                Log.d(TAG, "Link token created: $linkToken")
                
                _uiState.update { it.copy(
                    linkToken = linkToken,
                    isLoading = false
                ) }
                
                Log.d(TAG, "Link token created successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error connecting Plaid account", e)
                _uiState.update { it.copy(
                    error = "Failed to initialize bank connection: ${e.message}",
                    isLoading = false
                ) }
            }
        }
    }

    fun refreshData() {
        viewModelScope.launch {
            try {
                loadPlaidData()
            } catch (e: Exception) {
                // Show empty state if refresh fails
                Log.e(TAG, "Error refreshing data, showing empty state", e)
                _uiState.update { it.copy(
                    totalBudget = 0.0,
                    totalSpent = 0.0,
                    remainingBudget = 0.0,
                    recentTransactions = emptyList(),
                    error = "Could not load bank data: ${e.message}",
                    isLoading = false,
                    dataSource = "No Data"
                ) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    data class HomeUiState(
        val totalBudget: Double = 0.0,
        val totalSpent: Double = 0.0,
        val remainingBudget: Double = 0.0,
        val recentTransactions: List<Transaction> = emptyList(),
        val error: String? = null,
        val isLoading: Boolean = false,
        val linkToken: String? = null,
        val dataSource: String = "No Data"
    )
} 