package com.example.spendwise.data.model

import com.google.firebase.database.Exclude
import java.util.Date

data class Transaction(
    val id: String,
    val description: String,
    val amount: Double,
    val date: String,
    val isExpense: Boolean
) {
    @get:Exclude
    val dateAsDate: Date
        get() = Date(date.toLong())

    @get:Exclude
    val createdAt: Long = System.currentTimeMillis()

    @get:Exclude
    val createdAtAsDate: Date
        get() = Date(createdAt)

    @get:Exclude
    val updatedAt: Long = System.currentTimeMillis()

    @get:Exclude
    val updatedAtAsDate: Date
        get() = Date(updatedAt)
}

enum class TransactionType {
    INCOME,
    EXPENSE
} 