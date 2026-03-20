package dev.jcasas.features.transactions

import kotlinx.serialization.Serializable
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

@Serializable
data class TransactionRequest(
    val amount: String,
    val type: TransactionType,
    val description: String,
    val date: String,
) {
    fun toNewTransaction() = NewTransaction(
        amount = BigDecimal(amount),
        type = type,
        description = description,
        date = LocalDate.parse(date),
    )
}

@Serializable
data class TransactionResponse(
    val id: Int,
    val amount: String,
    val type: TransactionType,
    val description: String,
    val date: String,
) {
    companion object {
        fun from(transaction: Transaction) = TransactionResponse(
            id = transaction.id,
            amount = transaction.amount.toPlainString(),
            type = transaction.type,
            description = transaction.description,
            date = transaction.date.toString(),
        )
    }
}