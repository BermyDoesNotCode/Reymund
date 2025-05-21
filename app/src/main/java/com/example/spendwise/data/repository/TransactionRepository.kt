package com.example.spendwise.data.repository

import com.example.spendwise.data.model.Transaction
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.getValue
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepository @Inject constructor(
    private val database: FirebaseDatabase
) {
    private val transactionsRef = database.getReference("transactions")

    suspend fun addTransaction(transaction: Transaction): Result<Transaction> = try {
        val newTransaction = transaction.copy(
            id = transactionsRef.push().key ?: throw Exception("Failed to generate key")
        )
        transactionsRef.child(newTransaction.id).setValue(newTransaction).await()
        Result.success(newTransaction)
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun getTransactions(userId: String): Flow<List<Transaction>> = callbackFlow {
        val listener = transactionsRef
            .orderByChild("userId")
            .equalTo(userId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val transactions = snapshot.children.mapNotNull { it.getValue<Transaction>() }
                    trySend(transactions)
                }

                override fun onCancelled(error: DatabaseError) {
                    trySend(emptyList())
                }
            })

        awaitClose { transactionsRef.removeEventListener(listener) }
    }

    suspend fun updateTransaction(transaction: Transaction): Result<Transaction> = try {
        transactionsRef.child(transaction.id).setValue(transaction).await()
        Result.success(transaction)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun deleteTransaction(transactionId: String): Result<Unit> = try {
        transactionsRef.child(transactionId).removeValue().await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun getTransactionsByType(userId: String, isExpense: Boolean): Flow<List<Transaction>> = callbackFlow {
        val listener = transactionsRef
            .orderByChild("userId")
            .equalTo(userId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val transactions = snapshot.children
                        .mapNotNull { it.getValue<Transaction>() }
                        .filter { it.isExpense == isExpense }
                    trySend(transactions)
                }

                override fun onCancelled(error: DatabaseError) {
                    trySend(emptyList())
                }
            })

        awaitClose { transactionsRef.removeEventListener(listener) }
    }

    fun getTransactionsByDateRange(
        userId: String,
        startDate: Date,
        endDate: Date
    ): Flow<List<Transaction>> = callbackFlow {
        val listener = transactionsRef
            .orderByChild("userId")
            .equalTo(userId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val transactions = snapshot.children
                        .mapNotNull { it.getValue<Transaction>() }
                        .filter {
                            val dateLong = it.date.toLongOrNull() ?: 0L
                            dateLong in startDate.time..endDate.time
                        }
                    trySend(transactions)
                }

                override fun onCancelled(error: DatabaseError) {
                    trySend(emptyList())
                }
            })

        awaitClose { transactionsRef.removeEventListener(listener) }
    }
} 