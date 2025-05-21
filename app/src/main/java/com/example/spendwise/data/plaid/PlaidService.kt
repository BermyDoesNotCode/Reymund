package com.example.spendwise.data.plaid

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

// Retrofit API interface for Plaid
interface PlaidApi {
    @POST("link/token/create")
    suspend fun createLinkToken(@Body request: LinkTokenCreateRequest): LinkTokenCreateResponse

    @POST("item/public_token/exchange")
    suspend fun exchangePublicToken(@Body request: ExchangePublicTokenRequest): ExchangePublicTokenResponse

    @POST("accounts/balance/get")
    suspend fun getAccountBalances(@Body request: AccountsBalanceRequest): AccountsBalanceResponse

    @POST("transactions/get")
    suspend fun getTransactions(@Body request: TransactionsRequest): TransactionsResponse
}

// Request/Response data classes
data class LinkTokenCreateRequest(
    val client_id: String,
    val secret: String,
    val client_name: String,
    val user: PlaidUser,
    val products: List<String>,
    val country_codes: List<String>,
    val language: String,
    // Set these to true for sandbox testing
    val android_package_name: String = "com.example.spendwise"
)

data class PlaidUser(val client_user_id: String)

data class LinkTokenCreateResponse(val link_token: String)

data class ExchangePublicTokenRequest(
    val client_id: String,
    val secret: String,
    val public_token: String
)

data class ExchangePublicTokenResponse(val access_token: String, val item_id: String)

data class AccountsBalanceRequest(
    val client_id: String,
    val secret: String,
    val access_token: String
)

data class AccountsBalanceResponse(val accounts: List<Account>)

data class Account(
    val account_id: String,
    val name: String,
    val type: String,
    val balances: Balances
)

data class Balances(
    val current: Double,
    val available: Double?
)

data class TransactionsRequest(
    val client_id: String,
    val secret: String,
    val access_token: String,
    val start_date: String,
    val end_date: String
)

data class TransactionsResponse(
    val transactions: List<Transaction>,
    val accounts: List<Account>
)

data class Transaction(
    val transaction_id: String,
    val account_id: String,
    val amount: Double,
    val date: String,
    val name: String,
    val category: List<String>?
)

private const val TAG = "PlaidService"

@Singleton
class PlaidService @Inject constructor(
    private val secureTokenStorage: SecureTokenStorage
) {
    private val _plaidState = MutableStateFlow<PlaidState>(PlaidState.Initial)
    val plaidState: StateFlow<PlaidState> = _plaidState

    // Create a basic OkHttpClient without advanced configuration
    private val client = OkHttpClient.Builder()
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://sandbox.plaid.com/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val plaidApi = retrofit.create(PlaidApi::class.java)

    // Plaid sandbox credentials - make sure these are valid
    private val clientId = "6682b93015b1df700214d519c"
    private val secret = "83519eaeaf52807460f0692fe5d48e"

    suspend fun createLinkToken(
        clientName: String,
        userId: String
    ): String = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Creating link token for user: $userId")
            val request = LinkTokenCreateRequest(
                client_id = clientId,
                secret = secret,
                client_name = clientName,
                user = PlaidUser(client_user_id = userId),
                products = listOf("transactions", "auth", "balance"),
                country_codes = listOf("US"),
                language = "en",
                android_package_name = "com.example.spendwise"
            )

            val response = plaidApi.createLinkToken(request)
            Log.d(TAG, "Link token created successfully: ${response.link_token.take(10)}...")
            response.link_token
        } catch (e: Exception) {
            Log.e(TAG, "Error creating link token", e)
            _plaidState.value = PlaidState.Error(e)
            throw e
        }
    }

    suspend fun exchangePublicToken(publicToken: String): String = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Exchanging public token: ${publicToken.take(10)}...")
            val request = ExchangePublicTokenRequest(
                client_id = clientId,
                secret = secret,
                public_token = publicToken
            )

            val response = plaidApi.exchangePublicToken(request)
            val accessToken = response.access_token
            Log.d(TAG, "Public token exchanged successfully. Access token: ${accessToken.take(10)}...")

            // Store the access token securely
            secureTokenStorage.saveToken("plaid_access_token", accessToken)
            accessToken
        } catch (e: Exception) {
            Log.e(TAG, "Error exchanging public token", e)
            _plaidState.value = PlaidState.Error(e)
            throw e
        }
    }

    suspend fun getAccountBalances(): List<AccountBalance> = withContext(Dispatchers.IO) {
        try {
            val accessToken = secureTokenStorage.getToken("plaid_access_token")
                ?: throw Exception("No access token found")
            
            Log.d(TAG, "Getting account balances with access token: ${accessToken.take(10)}...")

            val request = AccountsBalanceRequest(
                client_id = clientId,
                secret = secret,
                access_token = accessToken
            )

            val response = plaidApi.getAccountBalances(request)
            Log.d(TAG, "Retrieved ${response.accounts.size} account(s)")
            
            response.accounts.map { account ->
                AccountBalance(
                    accountId = account.account_id,
                    name = account.name,
                    type = account.type,
                    balance = account.balances.current
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting account balances", e)
            _plaidState.value = PlaidState.Error(e)
            throw e
        }
    }

    suspend fun getTransactions(
        startDate: Date,
        endDate: Date
    ): List<PlaidTransaction> = withContext(Dispatchers.IO) {
        try {
            val accessToken = secureTokenStorage.getToken("plaid_access_token")
                ?: throw Exception("No access token found")

            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val startDateStr = dateFormat.format(startDate)
            val endDateStr = dateFormat.format(endDate)
            
            Log.d(TAG, "Getting transactions from $startDateStr to $endDateStr")
            
            val request = TransactionsRequest(
                client_id = clientId,
                secret = secret,
                access_token = accessToken,
                start_date = startDateStr,
                end_date = endDateStr
            )

            val response = plaidApi.getTransactions(request)
            Log.d(TAG, "Retrieved ${response.transactions.size} transaction(s)")
            
            response.transactions.map { transaction ->
                PlaidTransaction(
                    id = transaction.transaction_id,
                    accountId = transaction.account_id,
                    amount = transaction.amount,
                    date = transaction.date,
                    name = transaction.name,
                    category = transaction.category ?: emptyList()
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting transactions", e)
            _plaidState.value = PlaidState.Error(e)
            throw e
        }
    }
}

data class AccountBalance(
    val accountId: String,
    val name: String,
    val type: String,
    val balance: Double
)

data class PlaidTransaction(
    val id: String,
    val accountId: String,
    val amount: Double,
    val date: String,
    val name: String,
    val category: List<String>
)

sealed class PlaidState {
    object Initial : PlaidState()
    object Loading : PlaidState()
    data class Success(
        val publicToken: String,
        val metadata: Map<String, Any>
    ) : PlaidState()
    data class Exit(val error: Exception?) : PlaidState()
    object Cancelled : PlaidState()
    data class Error(val error: Throwable) : PlaidState()
} 