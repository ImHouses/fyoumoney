package dev.jcasas.features.transactions

import java.math.BigDecimal
import java.time.LocalDate

enum class TransactionType { INCOME, EXPENSE }

data class Transaction(
    val id: Int,
    val amount: BigDecimal,
    val type: TransactionType,
    val description: String,
    val date: LocalDate,
)

data class NewTransaction(
    val amount: BigDecimal,
    val type: TransactionType,
    val description: String,
    val date: LocalDate,
)